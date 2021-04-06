import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class MCReceive {

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

	public static void main(String[] args) throws Throwable {
		
		// bind to MC address on VMS
		MulticastSocket socket = new MulticastSocket(MCUtils.MC_GROUP);
		//MulticastSocket socket = new MulticastSocket(MCUtils.MC_PORT);
		socket.setInterface(MCUtils.getMCNetworkInterfaceAddress());
		socket.joinGroup(MCUtils.getMCAddress());
		
		byte[] buffer = new byte[1500];
		while (true) {
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			socket.receive(packet);
			System.out.println("Received packet from: " + packet.getSocketAddress());
		}
	}

}
