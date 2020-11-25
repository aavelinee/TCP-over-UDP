public class packetInSenderWindow extends packetInWindow {
	public int expectedAck;
	public boolean acked;
	public packetInSenderWindow(Packet pack, int exp, int id) {
		super(pack, id);
		expectedAck = exp;
		acked = false;
	}
}