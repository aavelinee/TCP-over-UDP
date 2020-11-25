import java.io.IOException;
import java.net.DatagramPacket;

public class Receiver {
    public static void main(String[] args) throws Exception {
        TCPServerSocket tcpServerSocket = new TCPServerSocketImpl(23456);
        TCPSocket tcpSocket = tcpServerSocket.accept();
        tcpSocket.receive("receive.txt");
        System.out.println("after receiveeeeeeeeeee");
        // tcpSocket.close();
        // tcpServerSocket.close();
    }
}
