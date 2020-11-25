import java.util.concurrent.locks.ReentrantLock;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ThreadLocalRandom;
import java.util.*;
import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.*; 
import java.util.Timer; 
import java.util.TimerTask;


public class TCPSocketImpl extends TCPSocket {

    public static final int LISTEN_PORT = 23456;
    public static final int RECV_PORT = 12346;
    public static final int SENDER_PORT = 12345;
    public static final int RECV_BUFF = 100;
    public static final int TCP_HEADER_SIZE = 32 + 4;//8*sizeof(int) + 4*sizeof(bool)
    public static final int TCP_PAYLOAD = 1000 - TCP_HEADER_SIZE;
    public static final int TIMEOUT = 5000;//in milisecond
    private int port;
    private int seqNumber = -1;
    private int lastReceivedSeq = -1;
    private int lastReceivedLen = 0;
    private int lastChunckSent = 0;
    public int rwnd = 100;
    public int lastAckedID = 0;

    private ReentrantLock senderWindowLock;
    public EnhancedDatagramSocket enhancedDatagramSocket;
    public List <packetInSenderWindow> senderWindow;
    public List <packetInWindow> receiverWindow;
    public List <Packet> readPackets;
    public FSM senderFSM;
    private Timer timer;
    private Task task;

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        this.port = port;
        System.out.println("port :");
        System.out.println(port);
        try{
            enhancedDatagramSocket = new EnhancedDatagramSocket(port);
            System.out.println("hereeee in tcp!");
        } catch(SocketException se){
            System.out.println("error in constructing: enhanced datagram socket");
        }
        senderWindow = new ArrayList<packetInSenderWindow>();
        senderWindow.add(null);
        senderFSM = new FSM();
        readPackets = new ArrayList<Packet>();
        receiverWindow = new ArrayList<packetInWindow>();
        for(int i = 0; i < RECV_BUFF; i++) {
        	receiverWindow.add(null);
        }
        senderWindowLock = new ReentrantLock();
        System.out.println("end of constructor");
    }

    public void setSeqNum(int seqNum) {
        seqNumber = seqNum;
    }
    public void setLastRecvSeqNum(int lastRecvSeqNum) {
        lastReceivedSeq = lastRecvSeqNum;
    }

    public Packet partialSend(boolean syn, boolean ack, boolean fin, boolean isData, int id, int last, int destPort, String data, int rwnd) {
        Random random = new Random();
        Packet pack = null;
        try{
            if (seqNumber == -1) {
            	seqNumber = random.nextInt(Integer.MAX_VALUE - 1) + 1;
            }

            pack = new Packet(port, destPort, seqNumber, lastReceivedSeq, syn, ack, fin, isData, id, last, data, rwnd);
            
            byte buf[] = new byte[TCP_PAYLOAD];
            buf = packetToByte(pack);

            InetAddress ip = InetAddress.getByName("localhost");
            DatagramPacket dp = new DatagramPacket(buf, buf.length, ip, destPort);
            enhancedDatagramSocket.send(dp);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch(IOException ioe) {
            System.out.println("IO Exception in send");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pack;
    }

    public Packet partialReceive(boolean synFlg, boolean ackFlg, boolean ackAck, boolean finFlg, boolean dataFlg) throws SocketTimeoutException, IOException, ClassNotFoundException{
        Packet pack = null;
        while(true){
            byte receiveData[] = new byte[TCP_PAYLOAD + 500];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            enhancedDatagramSocket.receive(receivePacket);
            pack = byteToPacket(receivePacket.getData());
            if(pack.ack != ackFlg || pack.syn != synFlg || pack.fin != finFlg || pack.isData != dataFlg) {
                continue;
            }

            if(!ackAck){
                if(pack.isData == true && pack.ack != true){//data
                    if(pack.lastChunkSize == 0)
                        lastReceivedSeq = pack.seqNum + TCP_PAYLOAD;
                    else{
                        lastReceivedSeq = pack.seqNum + TCP_HEADER_SIZE + pack.lastChunkSize;
                    }
                }
                else if(pack.isData == true && pack.ack == true){//dataack
                	lastReceivedSeq = pack.seqNum + 1;
                }
                else{//fin-finack-synack
                	seqNumber = pack.ackNum;
                    lastReceivedSeq = pack.seqNum + 1;
                }
            }

            break;
        }
        return pack;
    }

    public void connect() {
        Packet synAckPacket = null;
        Packet ackAckPacket = null;
        Packet temp = null;
        //SYN
        try {
            enhancedDatagramSocket.setSoTimeout(6000);
        } catch(SocketException e) {
            System.out.println("set time out");
        }
        while(true) {
            
            temp = partialSend(true, false, false, false, 1, 0, LISTEN_PORT, null, 0);

            System.out.println("Syn sent!");
                //receive SYN-ACK              
            try {
                synAckPacket = partialReceive(true, true, false, false, false);
                break;    
            } catch(SocketTimeoutException toe) {
                System.out.println("time out in receive syn-ack");
                continue;
            } catch(IOException ioe) {
                System.out.println("error in io syn ack pack");
            } catch (ClassNotFoundException e) {
                System.out.println("class not found exception in receive syn_ack");
                e.printStackTrace();
            }
            
        }

        // this while needs time out
        long cTime = System.currentTimeMillis();
        long eTime = cTime + 5000;
        byte receiveData[] = null;
        while(true) {
            //send ACK
            temp = partialSend(false, true, false, false, 1, 0, LISTEN_PORT, null, 0);
            System.out.println("Ack sent!");


            // Receive ACK-ACK

            try {
                ackAckPacket = partialReceive(false, false, true, false, false);
                break;
            } catch(SocketTimeoutException toe) {
                System.out.println("time out in receive ack-ack");
                if (System.currentTimeMillis() > eTime) {
                    break;
                }
                continue;
            } catch(IOException ioe) {
                System.out.println("error in io ack ack pack");
            } catch (ClassNotFoundException e) {
                System.out.println("class not found exception in receive syn_ack");
                e.printStackTrace();
            }


        }
        try {
            Thread.sleep(1000);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }


    private static List<String> chunckFile(File file) {
        FileInputStream fin = null;
        String s = null;
        List<String> chunks = new ArrayList<String>();
        try {
            // create FileInputStream object
            fin = new FileInputStream(file);
 
            byte fileContent[] = new byte[(int)file.length()];
             
            // Reads up to certain bytes of data from this input stream into an array of bytes.
            fin.read(fileContent);
            //create string from byte array
            s = new String(fileContent);
            // System.out.println("File content: " + s);
        }
        catch (IOException ioe) {
            System.out.println("Exception while reading file " + ioe);
        }
        int chunkNum = 0;
        if(s.length()%TCP_PAYLOAD == 0){
        	chunkNum = s.length() / TCP_PAYLOAD;
        }
        else{
        	chunkNum = (s.length() / TCP_PAYLOAD) + 1;
        	// System.out.println("File chuncks: " + chunkNum);
        }
        for(int i = 0; i < chunkNum; i++){
        	// System.out.println("File content: " + s.length());
            String chunk = "";
            if(i == chunkNum - 1){
            	for(int j = 0; j < s.length()%TCP_PAYLOAD; j++){
	                chunk += s.charAt(i*TCP_PAYLOAD + j);
	            } 
            }
            else{
                // System.out.println("tcp payload: " + TCP_PAYLOAD);
	            for(int j = 0; j < TCP_PAYLOAD; j++){
	                chunk += s.charAt(i*TCP_PAYLOAD + j);
	            }    	
            }
            // System.out.println("chunk in chunk file with len:" + chunk.length() + ":" + chunk);
            chunks.add(chunk);
        }
        return chunks;
    }

    public void retransmitPacket(Packet pack, int destPort){
    	try{
    		byte buf[] = new byte[TCP_PAYLOAD];
    		buf = packetToByte(pack);

            InetAddress ip = InetAddress.getByName("localhost");
            DatagramPacket dp = new DatagramPacket(buf, buf.length, ip, destPort);
            enhancedDatagramSocket.send(dp);
            //reset timer for first packet in window
			timer = new Timer();
			task = new Task();
        	// timer.schedule(task, TIMEOUT, TIMEOUT);
        	timer.schedule(task, TIMEOUT);

    	} catch (UnknownHostException e) {
            e.printStackTrace();
        } catch(IOException ioe) {
            System.out.println("IO Exception in send");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int setLastAcked(int ackNum){
		int index = 0;
		int lastAcked = 0;
        // System.out.println("size in set last acked:" + senderWindow.size());
        // System.out.println("expectedAck: " + senderWindow.get(index).expectedAck);
        // System.out.println("ackNum: " + ackNum);
        
		while(senderWindow.get(index).expectedAck <= ackNum){
            lastAcked = senderWindow.get(index).chunckID;
			senderWindow.get(index).acked = true;
            if(senderWindow.get(index).expectedAck == ackNum){
                break;
            }
			index++;
			if(index == senderWindow.size()){
				break;
			}
		}
		return lastAcked;
    }
    private void detectMoreLoss(){//detecting more than one loss in a window and resend it(detection without 3rd dup ack)
    	if(senderWindow.get(0) != null){
    		retransmitPacket(senderWindow.get(0).packet, RECV_PORT);
    	}
    }
    private synchronized int changeSenderWindowSize(int lastSent){

    	//delete acked slots from window
    	while(senderWindow.size() > 0){
            if(senderWindow.get(0) == null){
                break;
            }
    		if(senderWindow.get(0).acked == true){
    			senderWindow.remove(0);
    		}
    		else{
    			break;
    		}
    	}

    	if(senderFSM.cwnd >= senderWindow.size()){
    		int addedCapacity = (senderFSM.cwnd - senderWindow.size());
    		for(int i = 0; i < addedCapacity; i++){
    			senderWindow.add(null);
    		}
    	}
    	else{
    		int reducedCapacity = senderWindow.size() - senderFSM.cwnd;
    		for(int i = 0; i < reducedCapacity; i++){
    			Packet lastPack = senderWindow.get(senderWindow.size() - 1).packet;
    			if(lastPack.lastChunkSize != 0){
    				seqNumber -= (lastPack.lastChunkSize + TCP_HEADER_SIZE);
    			}
    			else{
    				seqNumber -= TCP_PAYLOAD;
    			}
    			senderWindow.remove(senderWindow.size() - 1);
    		}
    		lastSent -= reducedCapacity;
    	}
    	return lastSent;
    }
    public synchronized void setWindowItem(int index, Packet pack, int id){
		senderWindow.set(index, new packetInSenderWindow(pack, seqNumber, id));
    }

    @Override
    public void send(String pathToFile) throws Exception {
        File file = new File(pathToFile);
        List<String> chuncks = chunckFile(file);
        int lastAckedChunck = 0;
        lastChunckSent = 0;
        Packet sentPack = null;
        Packet dataPacket = null;
        boolean resend = false;
        System.out.println(chuncks.size());
        while(lastAckedChunck != chuncks.size()){
            System.out.println("llllllllllllllllllllllllllll " + lastAckedChunck);
            // System.out.println("size before sending:" + senderWindow.size());
        	for(int i = 0; i < senderWindow.size(); i++){
        		if(senderWindow.get(i) == null && lastChunckSent < chuncks.size()){
        			if(lastChunckSent == chuncks.size() - 1){
        				// System.out.println("last chunk sent with len: " +chuncks.get(lastChunckSent).length());
		                sentPack = partialSend(false, false, false, true, lastChunckSent+1, chuncks.get(lastChunckSent).length(), RECV_PORT, chuncks.get(lastChunckSent), 0);
                        System.out.println("last pack with id " + sentPack.chunkID + " sent");
                        setWindowItem(i, sentPack, lastChunckSent + 1);
                        seqNumber += (TCP_HEADER_SIZE + chuncks.get(lastChunckSent).length());////////+TCP_PAYLOAD???
		            }
		            else{
		            	// System.out.println("chunk sent with len: " +chuncks.get(lastChunckSent).length());
		                sentPack = partialSend(false, false, false, true, lastChunckSent+1, 0, RECV_PORT, chuncks.get(lastChunckSent), 0);
                        System.out.println("pack with id " + sentPack.chunkID + " sent");		                
                        setWindowItem(i, sentPack, lastChunckSent + 1);
		                seqNumber += TCP_PAYLOAD;
	            	}
	            	if(i == 0){//set timer for first packet in window
	                	timer = new Timer();
        				task = new Task();
	                	// timer.schedule(task, TIMEOUT, TIMEOUT);
                    	timer.schedule(task, TIMEOUT);

		            }
	            	lastChunckSent++;
        		}
        	}
            // System.out.println("size after sending:" + senderWindow.size());
            try {
                dataPacket = partialReceive(false, true, false, false, true);
                System.out.println("ack packet received with " + dataPacket.ackNum + " in sent");
                //time out yadet nareeeee
                if(dataPacket.ackNum < senderWindow.get(0).expectedAck){//dup ack
                    resend = senderFSM.makeTransition("dupack");
                    lastChunckSent = changeSenderWindowSize(lastChunckSent);
                }
                else if(dataPacket.ackNum >= senderWindow.get(0).expectedAck){//newack
                    timer.cancel();
                    resend = senderFSM.makeTransition("newack");
                    // System.out.println("size before set last:" + senderWindow.size());
                    lastAckedChunck = setLastAcked(dataPacket.ackNum);
                    // System.out.println("after set last");
                    lastChunckSent = changeSenderWindowSize(lastChunckSent);
                    detectMoreLoss();

                }

                if(resend){
                    retransmitPacket(senderWindow.get(0).packet, RECV_PORT);
                }
            } catch(SocketTimeoutException toe) {
                System.out.println("time out in receive data ack");
                continue;
            } catch(IOException ioe) {
                System.out.println("error in io data ack pack");
            } catch (ClassNotFoundException e) {
                System.out.println("class not found exception in receive data ack");
                e.printStackTrace();
            }

        }     

    }
    public int calcRWND(){
    	int occupiedNum = 0;
    	for(int i = 0; i < receiverWindow.size(); i++){
    		if(receiverWindow.get(i) != null){
    			occupiedNum ++;
    		}
    	}
    	return RECV_BUFF - occupiedNum;
    }

    public void changeReceiverWindowSize(){
    	int i = 0;
    	for(i = 0; i < RECV_BUFF; i++){
    		if(receiverWindow.get(i) != null){
    			receiverWindow.remove(i);
    		}
    		else {
    			break;
    		}
    	}
    	for(int j = 0; j < i; j++){
    		receiverWindow.add(null);
    	}

    }

    public void readPacket(){
    	for(int i = 0; i < RECV_BUFF; i++){
    		if(receiverWindow.get(i) != null){
    			readPackets.add(receiverWindow.get(i).packet);
    		}
    		else {
    			break;
    		}
    	}
    }

    public int obtainAckNumber() {
    	int ackNumber = -1;
    	for(int i = 0; i < RECV_BUFF; i++){
    		if(receiverWindow.get(i) != null){
    			if(receiverWindow.get(i).packet.lastChunkSize == 0){
    				ackNumber = receiverWindow.get(i).packet.seqNum + TCP_PAYLOAD;
    			}
    			else {
    				ackNumber = receiverWindow.get(i).packet.seqNum + TCP_HEADER_SIZE + receiverWindow.get(i).packet.lastChunkSize;
    			}
    		}
    		else {
                /////////////////////////////////////////////////////////////////
    			lastAckedID = receiverWindow.get(i-1).packet.chunkID;
                // lastAckedID = i - 1;
                /////////////////////////////////////////////////////////////////
    			break;
    		}
    	}
        System.out.println(receiverWindow.get(0).packet.chunkID);
    	return ackNumber;
    }

    @Override
    public void receive(String pathToFile) throws Exception {
    	boolean isLastChunck = false, isLastRecvd = false;
        int numOfChunks = 0, recvdCount = 0, rand = 0, lastSeq = 0;
    	Packet recvPack = null, ackPack = null;
     	while(!isLastChunck) {
	   		recvPack = partialReceive(false, false, false, false, true);
            System.out.println("pack with id " + recvPack.chunkID + " received");
            System.out.println("pack with seq " + recvPack.seqNum + " received");  
	   		// System.out.println("recived pack: " + recvPack.data);
            if(recvPack.chunkID == 1){//receive first pack
                rand = recvPack.seqNum;
                if(isLastRecvd){
                    numOfChunks = ((lastSeq - rand)/TCP_PAYLOAD) + 1;
                }
            }
            if(recvPack.lastChunkSize != 0){//receive last pack
                isLastRecvd = true;
                lastSeq = recvPack.seqNum;
                if(rand != 0){
                    numOfChunks = ((lastSeq - rand)/TCP_PAYLOAD) + 1;
                }
            }

            System.out.println("jjjjjjjjjjjjjjjj " + lastAckedID);
	   		if(recvPack.chunkID == lastAckedID + 1) { // the packet received in correct sequence
                if(receiverWindow.get(0) == null || receiverWindow.get(0).chunckID != recvPack.chunkID){
	   			    receiverWindow.set(0, new packetInWindow(recvPack, recvPack.chunkID));
                    recvdCount++;
	   			    lastReceivedSeq = obtainAckNumber();
                    System.out.println("------------------" + lastReceivedSeq);
	   			    readPacket();
	   			    changeReceiverWindowSize();
                }
    		        System.out.println("lastReceivedSeq in correct ack: " + lastReceivedSeq);
	   			    ackPack = partialSend(false, true, false, true, recvPack.chunkID, recvPack.lastChunkSize, SENDER_PORT, "", calcRWND());   
                    System.out.println("ack sent with ack num " + ackPack.ackNum);

	   		} else { // there is a gap in receiving packet and dup ack should be sent
                    System.out.println("last ackeeeeeeeeeed! " + lastAckedID + " chunkIddddd " + recvPack.chunkID);
                    if(receiverWindow.get(recvPack.chunkID - lastAckedID - 1) == null || receiverWindow.get(recvPack.chunkID - lastAckedID - 1).chunckID != recvPack.chunkID){
                        System.out.println("********************");
                        receiverWindow.set(recvPack.chunkID - lastAckedID - 1, new packetInWindow(recvPack, recvPack.chunkID));
                        recvdCount++;
                    }
                    System.out.println("lastReceivedSeq in dup ack: " + lastReceivedSeq);
                    ackPack = partialSend(false, true, false, true, recvPack.chunkID, recvPack.lastChunkSize, SENDER_PORT, "", calcRWND());
                    System.out.println("ack sent with ack num " + ackPack.ackNum);
	   		}
            if((isLastRecvd == true) && (recvdCount == numOfChunks)){
                break;
            }
 	   	}
    }

    @Override
    public void close() throws Exception {
    	throw new RuntimeException("Not implemented!");
        // Packet finPacket = null;
        // Packet temp = null;
        // try {
        //     enhancedDatagramSocket.setSoTimeout(500);
        // } catch(SocketException e) {
        //     System.out.println("set time out in fin pack");
        // }
        // while(true){
        //     temp = partialSend(false, false, true, false, 0, 0, RECV_PORT, null);
        //     System.out.println("fin sent");

        //     try{
        //         finPacket = partialReceive(false, true, false, true);
        //         System.out.println("fin ack received");
        //         break;
        //     } catch(SocketTimeoutException toe) {
        //         System.out.println("time out in receive fin-ack");
        //         continue;
        //     } catch(IOException ioe) {
        //         System.out.println("error in io fin ack pack");
        //     } catch (ClassNotFoundException e) {
        //         System.out.println("class not found exception in receive fin_ack");
        //         e.printStackTrace();
        //     }
        // }
        // enhancedDatagramSocket.close();
    }

    @Override
    public long getSSThreshold() {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getWindowSize() {
        throw new RuntimeException("Not implemented!");
    }

    public static Packet byteToPacket(byte [] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream is = new ObjectInputStream(in);
        Packet ret = (Packet)is.readObject();
        is.close();
        return ret;
    }
    public static byte[] packetToByte(Packet pack) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(pack);
        oos.flush();
        return bos.toByteArray();

    }
	class Task extends TimerTask 
	{
	    public void run() 
	    {
            System.out.println("timeout in ruuuuuuuuuuuuuuuun");
	    	senderFSM.makeTransition("timeout");
            // senderWindowLock.lock();
            // try{
            lastChunckSent = changeSenderWindowSize(lastChunckSent);
            // }finally{
                // senderWindowLock.unlock();
            // }
	        retransmitPacket(senderWindow.get(0).packet, RECV_PORT);
	    } 
	}

}
 