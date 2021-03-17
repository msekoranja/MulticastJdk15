import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class MCUtils {
	public static final String MC_ADDRESS = "239.219.1.200";
	public static final short MC_PORT = 6789;
	public static final InetSocketAddress MC_GROUP = new InetSocketAddress(MC_ADDRESS, MC_PORT);

	public static final byte[] TEST_PACKET_DATA = { 0, 1, 2, 3, 4, 5, 6, 8 };

	private static InetAddress mcAddress = null;
	public static synchronized InetAddress getMCAddress() throws UnknownHostException {
		if (mcAddress == null) {
			mcAddress = InetAddress.getByName(MC_ADDRESS);
		}
		return mcAddress;
	}

	private static NetworkInterface firstLoopbackIf = null;
	public static NetworkInterface getFirstLoopbackNIF() throws SocketException {
		if (firstLoopbackIf == null)
		{
			Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
			while (nifs.hasMoreElements()) {
				NetworkInterface nif = nifs.nextElement();
				Enumeration<InetAddress> nifAddrs = nif.getInetAddresses();
				// we check only first one, since we expect IPv4 enforced
				if (nifAddrs.hasMoreElements()) {
					InetAddress nifAddr = nifAddrs.nextElement();
					if (nifAddr.isLoopbackAddress()) {
						firstLoopbackIf = nif;
						return nif;
					}
				}
			}
		}
		return firstLoopbackIf;
	}
	
	public static NetworkInterface getMCNetowkInterface() throws SocketException, UnknownHostException {
		//NetworkInterface netIf = NetworkInterface.getByName("lo");
		NetworkInterface netIf = getFirstLoopbackNIF();
		InetAddress netIfAddr = netIf.getInetAddresses().nextElement();
		
		System.out.println("MC Group:   " + getMCAddress() + ":" + MCUtils.MC_PORT);
		System.out.println("Network IF: " + netIfAddr);
		if (netIfAddr instanceof Inet6Address) {
			System.out.println("NOTE: IPv6 network inteface used");
		}
		return netIf;
	}
	
	public static InetAddress getMCNetowkInterfaceAddress() throws SocketException, UnknownHostException {
		return getMCNetowkInterface().getInetAddresses().nextElement();
	}

}
