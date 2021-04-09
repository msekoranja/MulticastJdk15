import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TCPClient {

	public static final int PORT = 12345;

	public static void main(String[] args) {
		try {
			String serverAddress = args.length > 0 ? args[0] : "localhost";
			InetSocketAddress remoteAddress = new InetSocketAddress(serverAddress, PORT);

			System.out.println("Connecting to: " + remoteAddress);

			SocketChannel socket = SocketChannel.open();
			socket.configureBlocking(true);

			socket.connect(remoteAddress);

			System.out.println("Connected, waiting for message...");

			ByteBuffer msg = ByteBuffer.allocate(1000);
			int nread = socket.read(msg);
			if (nread > 0) {
				System.out.println(new String(msg.array()));
			} else {
				System.out.println("no data");
			}
			Thread.sleep(5000);
			socket.close();
			System.out.println("done.");
		} catch (Throwable th) {
			th.printStackTrace();
		}
	}
}
