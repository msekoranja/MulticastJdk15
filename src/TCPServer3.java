import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class TCPServer3 {

	public static final int PORT = 12345;

	public static void main(String[] args) {
		try {
			InetSocketAddress bindAddress = new InetSocketAddress(PORT);

			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.socket().bind(bindAddress);
			serverSocketChannel.configureBlocking(true);

			System.out.println("Listening on port: " + PORT);

			while (true) {
				
				final SocketChannel socket = serverSocketChannel.accept();

				SocketAddress address = socket.socket().getRemoteSocketAddress();
				System.out.println("Accepted connection from PVA client: " + address);

				socket.socket().setTcpNoDelay(true);
				socket.socket().setKeepAlive(true);

				// crazy idea, but can blocking read block writing on VMS?!
				new Thread(new Runnable() {
					
					public void run() {
						try {
							ByteBuffer msg = ByteBuffer.allocate(1000);
							int nread = socket.read(msg);
							System.out.println("Bytes read: " + nread);
						} catch (AsynchronousCloseException ace) {
							// noop, expected
						} catch (Throwable th) {
							th.printStackTrace();
						}
					}
				}).start();

				Thread.sleep(1000);
				
				new Thread(new Runnable() {
					
					public void run() {
						try {
							ByteBuffer msg = ByteBuffer.wrap("from Server".getBytes());
							int nwritten = socket.write(msg);
							System.out.println("Message sent, bytes written: " + nwritten);
						} catch (Throwable th) {
							th.printStackTrace();
						}
					}
				}).start();

				System.out.println("Sleeping for 5s.");
				
				Thread.sleep(5000);
				
				socket.close();
				System.out.println("Connection closed.");
			}
		} catch (Throwable th) {
			th.printStackTrace();
		}
	}
}
