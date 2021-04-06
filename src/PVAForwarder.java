import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Enumeration;

public class PVAForwarder {

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

	/**
	 * PVA protocol magic.
	 */
	public static final byte PVA_MAGIC = (byte)0xCA;

	/**
	 * PVA protocol message header size.
	 */
	public static final short PVA_MESSAGE_HEADER_SIZE = 8;

    /**
	 * UDP maximum receive message size.
	 * MAX_UDP: 65535 (max UDP packet size) - 20/40(IPv4/IPv6) - 8(UDP) 
	 */
	public static final int MAX_UDP_PACKET = 65487;

	/**
	 * Default PVA beacon port.
	 */
	public static final int PVA_BROADCAST_PORT = 5076;

	
	public static final String MC_ADDRESS = "239.219.1.200";
	public static final short MC_PORT = PVA_BROADCAST_PORT;

	/**
	 * Encode address as IPv6 address.
	 * @param buffer byte-buffer where to put encoded data.
	 * @param address address to encode.
	 * @throws RuntimeException thrown if address is unsupported.
	 */
	public static final void encodeAsIPv6Address(ByteBuffer buffer, InetAddress address) throws RuntimeException {
		if (address instanceof Inet6Address)
			buffer.put(address.getAddress());	// always network byte order
		else if (address instanceof Inet4Address)
		{
			// IPv4 compatible IPv6 address
			// first 80-bit are 0
			buffer.putLong(0);
			buffer.putShort((short)0);
			// next 16-bits are 1
			buffer.putShort((short)0xFFFF);
			// following IPv4 address
			buffer.put(address.getAddress());	// always network byte order
		}
		else
			throw new RuntimeException("unsupported network addresss: " + address);
	}

	public static NetworkInterface getFirstLoopbackNIF() throws SocketException {
		Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
		while (nifs.hasMoreElements()) {
			NetworkInterface nif = nifs.nextElement();
			Enumeration<InetAddress> nifAddrs = nif.getInetAddresses();
			// we check only first one, since we expect IPv4 enforced
			if (nifAddrs.hasMoreElements()) {
				InetAddress nifAddr = nifAddrs.nextElement();
				if (nifAddr.isLoopbackAddress()) {
					return nif;
				}
			}
		}
		throw new RuntimeException("no loopback interface found");
	}
	
	public static void main(String[] args) throws Throwable {
		
		System.out.println("Binding to UDP socket at port " + MC_PORT);

		DatagramSocket receiveSocket = new DatagramSocket(MC_PORT);
		/*
		DatagramSocket receiveSocket = new DatagramSocket(null);
		receiveSocket.setReuseAddress(true);
		receiveSocket.bind(new InetSocketAddress(MC_PORT));
		*/
		
		InetSocketAddress mcAddress = new InetSocketAddress(MC_ADDRESS, MC_PORT);
		System.out.println("MC Group:   " + mcAddress);

		NetworkInterface loNif = PVAForwarder.getFirstLoopbackNIF();
		System.out.println("MC Loopback Network IF: " + loNif);

		MulticastSocket sendSocket = new MulticastSocket();
		sendSocket.setNetworkInterface(loNif);

		
		byte[] buffer = new byte[MAX_UDP_PACKET];		
		while (true) {
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			receiveSocket.receive(packet);
			InetSocketAddress responseFrom = (InetSocketAddress)packet.getSocketAddress();
			if (responseFrom.getAddress().isLoopbackAddress()) {
				continue;
			}
			
			System.out.println("Received packet from: " + responseFrom);
			
			ByteBuffer receiveBuffer = ByteBuffer.wrap(packet.getData(), packet.getOffset(), packet.getLength());
			
			if (receiveBuffer.remaining() < PVA_MESSAGE_HEADER_SIZE) {
				System.out.println("Received packet too small, ignoring...");
				continue;
			}
			
			//
			// read header
			//
	
			// first byte is PVA_MAGIC
			final byte magic = receiveBuffer.get();
			if (magic != PVA_MAGIC) {
				System.out.println("Invalid magic signature, ignoring...");
				continue;
			}
			
			// second byte version - major/minor nibble 
			// check only major version for compatibility
			receiveBuffer.get(); 
			
			final byte flags = receiveBuffer.get();
			if ((flags & 0x80) != 0)
			{
				// 7th bit is set
				receiveBuffer.order(ByteOrder.BIG_ENDIAN);
			}
			else
			{
				receiveBuffer.order(ByteOrder.LITTLE_ENDIAN);
			}
			
			// command ID and paylaod
			final byte commandId = receiveBuffer.get();
			if (commandId != 3) {
				System.out.println("Not a search request, ignoring... (ID: " + commandId + " != 3)");
				continue;
			}
			final int payloadSize = receiveBuffer.getInt();
			
			// control message check (skip message)
			if ((flags & 0x01) != 0) {
				System.out.println("Control message received, ignoring...");
				continue;
			}

			
			
			
			//
			// payload
			//
			
			if (receiveBuffer.remaining() < payloadSize ||
				receiveBuffer.remaining() < (4+1+3+16+2)) {
				System.out.println("Payload too short, ignoring...");
				continue;
			}
			
			receiveBuffer.getInt();
			final byte qosCode = receiveBuffer.get();

			// reserved part
			receiveBuffer.get();
			receiveBuffer.getShort();

			// 128-bit IPv6 address
			byte[] byteAddress = new byte[16]; 
			receiveBuffer.get(byteAddress);
			
			final int port = receiveBuffer.getShort() & 0xFFFF;
			
			
			// NOTE: Java knows how to compare IPv4/IPv6 :)
			InetAddress addr;
			try {
				addr = InetAddress.getByAddress(byteAddress);
			} catch (UnknownHostException e) {
				System.out.println("Invalid address '" +  new String(byteAddress) + "' in search response.");
				return;
			}

			
			// accept given address if explicitly specified by sender
			if (!addr.isAnyLocalAddress())
				responseFrom = new InetSocketAddress(addr, port);
			else
				responseFrom = new InetSocketAddress(responseFrom.getAddress(), port);

			// 
			// locally broadcast ALL, not only if unicast (qosCode & 0x80 == 0x80)
			//
			//if ((qosCode & 0x80) == 0x80)
			{
				// clear unicast flag
				receiveBuffer.put(PVA_MESSAGE_HEADER_SIZE + 4, (byte)(qosCode & ~0x80));
				
				// update response address
				receiveBuffer.position(PVA_MESSAGE_HEADER_SIZE + 8);
				PVAForwarder.encodeAsIPv6Address(receiveBuffer, responseFrom.getAddress());
				
				// need to recreate a new packet, otherwise send does not work
				packet = new DatagramPacket(
						packet.getData(), 
						packet.getLength(),
						mcAddress.getAddress(), MC_PORT);

				System.out.println("Forwarding packet to: " + packet.getSocketAddress());
				sendSocket.send(packet);
			} 
		}
	}

	
	
	
	
	
	
	
}
