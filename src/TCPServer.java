import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class TCPServer {

	public static final int PORT = 12345;

	public static void main(String[] args) {
		try {
			InetSocketAddress bindAddress = new InetSocketAddress(PORT);

			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.socket().bind(bindAddress);
			serverSocketChannel.configureBlocking(true);

			System.out.println("Listening on port: " + PORT);

			while (true) {
				
				SocketChannel socket = serverSocketChannel.accept();

				SocketAddress address = socket.socket().getRemoteSocketAddress();
				System.out.println("Accepted connection from PVA client: " + address);

				socket.socket().setTcpNoDelay(true);
				socket.socket().setKeepAlive(true);
				
				ByteBuffer msg = ByteBuffer.wrap("from Server".getBytes());
				socket.write(msg);

				System.out.println("Message sent, sleeping for 5s.");
				
				Thread.sleep(5000);
				
				socket.close();
				System.out.println("Connection closed.");
			}
		} catch (Throwable th) {
			th.printStackTrace();
		}
	}
}
