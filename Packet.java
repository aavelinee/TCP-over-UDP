import java.io.*;

public class Packet implements java.io.Serializable {

	public int sourcePort;
	public int destPort;
	public int seqNum;
	public	int ackNum;
	public	int windowSize;
	public int chunkID;
	public int lastChunkSize;
	public int rwnd;
	public boolean ack;
	public	boolean syn;
	public	boolean fin;
	public boolean isData;
	public String data;

	public Packet(int source, int dest, int seqNumber, int ackNumber,boolean synBool, boolean ackBool,boolean finish,
					  boolean dataFlg, int id, int last, String tcpData, int recvWnd) throws Exception {
		sourcePort = source;
		destPort = dest;
		seqNum = seqNumber;
		ackNum = ackNumber;
		ack = ackBool;
		syn = synBool;
		fin = finish;
		isData = dataFlg;
		chunkID = id;
		lastChunkSize = last;
		data = tcpData;
		rwnd = recvWnd;
	}
}