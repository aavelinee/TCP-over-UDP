import java.io.IOException;
import java.net.*;

public class Sender {
    public static void main(String[] args) throws Exception {
        TCPSocket tcpSocket = new TCPSocketImpl("127.0.0.1", 12345);
      	tcpSocket.connect();
        tcpSocket.send("1MB.txt");
        System.out.println("after sendddddddddddddddddddddddddddddd");
        // System.out.println("after send!");
        // tcpSocket.close();
        // tcpSocket.saveCongestionWindowPlot();
    }
}
