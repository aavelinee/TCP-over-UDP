import java.net.DatagramPacket;
import java.io.*;
import java.net.*;
import java.net.SocketException;
import java.util.*;
import java.net.UnknownHostException;

public class TCPServerSocketImpl extends TCPServerSocket {

    public static final int SENDER_PORT = 12345;
    public static final int TCP_HEADER_SIZE = 32 + 4;//8*sizeof(int) + 4*sizeof(bool)

    private int seqNumber = -1;
    private int lastReceivedSeq = -1;
    private int lastReceivedLen = 0;

    public EnhancedDatagramSocket enhancedDatagramSocket;
    private int port;
    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
        this.port = port;
        try {
            enhancedDatagramSocket = new EnhancedDatagramSocket(port);
        } catch(SocketException se) {
            System.out.println("error in constructing: enhanced datagram socket");
        }   
    }

    public void setSeqNum(int seqNum) {
        seqNumber = seqNum;
    }
    public void setLastRecvSeqNum(int lastRecvSeqNum) {
        lastReceivedSeq = lastRecvSeqNum;
    }
    public void handshakeSend(boolean syn, boolean ack, boolean fin, boolean isData, int id, int last, String data) {
        Random random = new Random();
        try{
                if (seqNumber == -1) {
                    // seqNumber = ThreadLocalRandom.current().nextInt(1);
                    seqNumber = random.nextInt(Integer.MAX_VALUE - 1) + 1;
                    System.out.println("random seq number syn pack: " + seqNumber);
                }

                Packet pack = new Packet(port, SENDER_PORT, seqNumber, lastReceivedSeq, syn, ack, fin, isData, id, last, data, 0);
                
                byte buf[] = new byte[1480];
                buf = packetToByte(pack);

                InetAddress ip = InetAddress.getByName("localhost");
                DatagramPacket dp = new DatagramPacket(buf, buf.length, ip, SENDER_PORT);
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
    }


     public Packet handshakeReceive(boolean synFlg, boolean ackFlg, boolean finFlg) throws SocketTimeoutException, IOException, ClassNotFoundException{
        Packet pack = null;
        while(true){
            byte receiveData[] = new byte[1480 + 8];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            enhancedDatagramSocket.receive(receivePacket);
            pack = byteToPacket(receivePacket.getData());
            if(pack.ack != ackFlg || pack.syn != synFlg || pack.fin != finFlg) {
                continue;
            }
            System.out.println("pack received!");

            seqNumber = pack.ackNum;
            if( (!pack.ack && !pack.syn == true) && !pack.fin){//not handsahke and not fin 
                if(pack.lastChunkSize == 0)
                    lastReceivedSeq = pack.seqNum + 1480;
                else{
                    lastReceivedSeq = pack.seqNum + TCP_HEADER_SIZE + pack.lastChunkSize;
                }
            }
            else{
                System.out.println("receive handshake or close");
                lastReceivedSeq = pack.seqNum + 1;
            }

            System.out.println("received seq number synack pack: " + pack.seqNum);
            System.out.println("received ack number synack pack: " + pack.ackNum);

            break;
        }
        return pack;
    }


    @Override
    public TCPSocket accept() throws Exception {
        // receive SYN
        byte buf[] = null;
        byte receiveAckData[] = null;

        Packet ackPacket = null;
        Packet synPacket = null;
        try {
                
            synPacket = handshakeReceive(true, false, false);
            System.out.println("received seq number syn pack: " + synPacket.seqNum);
            System.out.println("received ack number syn pack: " + synPacket.ackNum);

        } catch(IOException ioe) {
            System.out.println("error in io syn");
        }
        try {
            enhancedDatagramSocket.setSoTimeout(500);
        } catch(SocketException e) {
            System.out.println("time out in syn pack");
        }
        while(true) {
            //send SYN-ACK
            handshakeSend(true, true, false, false, 0, 0, null);
            System.out.println("Syn-Ack sent!");

            // receive ACK
            try {
                ackPacket = handshakeReceive(false, true, false);
                break;
            } catch(SocketTimeoutException toe) {
                System.out.println("time out in receive ack");
                continue;
            } catch(IOException ioe) {
                System.out.println("error in io ack");
            }
        }

        // ACK-ACK
        // this while needs time out
        long cTime = System.currentTimeMillis();
        long eTime = cTime + 1000;
        while(true) {
            handshakeSend(false, false, false, false, 0, 0, null);
            if(System.currentTimeMillis() > eTime) {
                break;
            }
        }
        TCPSocketImpl tcpSocket = new TCPSocketImpl("127.0.0.1", 12346);
        tcpSocket.setSeqNum(seqNumber);
        tcpSocket.setLastRecvSeqNum(lastReceivedSeq);
        return tcpSocket;
    }

    @Override
    public void close() throws Exception {
        
        Packet finPacket = null;
        try {
            enhancedDatagramSocket.setSoTimeout(500);
        } catch(SocketException e) {
            System.out.println("set time out fin pack");
        }
        while(true){

            try{
                finPacket = handshakeReceive(false, false, true);
                System.out.println("fin received");

            } catch(SocketTimeoutException toe) {
                System.out.println("time out in receive fin");
                continue;
            } catch(IOException ioe) {
                System.out.println("error in io fin");
            } catch (ClassNotFoundException e) {
                System.out.println("class not found exception in receive fin");
                e.printStackTrace();
            }

            long cTime = System.currentTimeMillis();
            long eTime = cTime + 1000;
            while(true) {
                handshakeSend(false, true, true, false, 0, 0, null);
                System.out.println("fin ack sent!");
                if(System.currentTimeMillis() > eTime) {
                    break;
                }
            }
            break;
        }
        enhancedDatagramSocket.close();
    }    


    public static Packet byteToPacket(byte [] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream is = new ObjectInputStream(in);
        return (Packet)is.readObject();
    }
    
    public static byte[] packetToByte(Packet pack) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(pack);
        oos.flush();
        return bos.toByteArray();

    }
}
