import java.util.Timer; 
import java.util.TimerTask; 
import java.util.concurrent.TimeUnit;

class Helper extends TimerTask 
{
    public int id;
	public Helper(int idd){
		super();
		id = idd;
	} 
    public void run() 
    { 
        System.out.println("Timer " + id + " ran " + ++Test.i); 
    } 
} 
  
public class Test 
{ 
    	    public static int i = 0; 
    public static void main(String[] args) 
    { 
          

        	Timer timer = new Timer(); 
        // TimerTask task = new Helper(1); 
        while(true){
        	TimerTask task = new Helper(1); 
	        timer.schedule(task, 0, 1000);
	        try{
	        	TimeUnit.SECONDS.sleep(4);
	        }catch (InterruptedException e) { 
            	System.out.println("Interrupted " + "while Sleeping"); 
        	}
        	System.out.println("AWAKE"); 
	        timer.cancel();

	        try{
	        	TimeUnit.SECONDS.sleep(4);
	        }catch (InterruptedException e) { 
            	System.out.println("Interrupted " + "while Sleeping"); 
        	}


    	}
          
    } 
}


// import java.net.SocketTimeoutException;
// import java.net.UnknownHostException;
// import java.net.SocketException;
// import java.net.DatagramPacket;
// import java.net.InetAddress;
// import java.io.Serializable;
// import java.io.ObjectOutputStream;
// import java.io.ByteArrayOutputStream;
// import java.util.concurrent.ThreadLocalRandom;
// import java.util.*;
// import java.io.*;
// import java.util.concurrent.TimeUnit;
// import java.util.*; 

// public class Test{


//     public static void main(String args[]) {
//     	List<Integer> a = new ArrayList<Integer>(1);
//     	a.add(null);
//     	if(a.get(0) == null){
//     		System.out.println("hiii");
//     	}

//     }
// }


// public class Test{
	
//     private static List<byte[]> chunckFile(String pathToFile) {
//         // Vector chunks = new Vector(); 
//         File file = new File(pathToFile);
//         List<byte[]> chunks = new ArrayList<byte[]>();
//         try (RandomAccessFile data = new RandomAccessFile(file, "r")) {
//             for (long i = 0, len = data.length() / (1480 - 31); i < len + 1; i++) {
//                 byte[] chunk = new byte[1480 - 31];
//                 data.readFully(chunk);
//                 for(int j = 0; j < 1480 - 31; j++){
//                     System.out.print((char)chunk[(int)i]); 
//                 }              
//                 chunks.add(chunk);
//             }
//         } catch(FileNotFoundException f) {
//             System.out.println("f");
//         } catch(IOException io) {
//             System.out.println("io");
//         }
//         System.out.println(chunks.size());
//         return chunks;
//     }


//     public static void main(String args[]) {
//     	List<byte[]> chunks = chunckFile("send.txt");
//     }
// }
