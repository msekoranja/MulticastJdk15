import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class MCSend {

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

	public static void main(String[] args) throws Throwable {
		
		MulticastSocket socket = new MulticastSocket();
		socket.setNetworkInterface(MCUtils.getMCNetowkInterface());
		//socket.setTimeToLive(1);
		
		while (true) {
			DatagramPacket packet = new DatagramPacket(
					MCUtils.TEST_PACKET_DATA, 
					MCUtils.TEST_PACKET_DATA.length,
					MCUtils.getMCAddress(), MCUtils.MC_PORT);
			socket.send(packet);
			System.out.println(System.currentTimeMillis() + ": packet set.");
			Thread.sleep(1000);
		}
	}

}
