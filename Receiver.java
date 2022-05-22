import java.util.ArrayList;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Receiver {
	
	/** probability of ACK loss */
	public static final double probability = 0.01;

	public static void main(String[] args) throws Exception{

		/** get port number from argument */
		int port = Integer.parseInt(args[0]);
		
		DatagramSocket fromSender = new DatagramSocket(port);
		
		/** the base size (in bytes) of a serialized RDTPacket object is 83 */
		byte[] datareceived = new byte[Sender.mss + 83];
		
		int nextseqnum = 0;
		
		ArrayList<RDTPacket> received = new ArrayList<RDTPacket>();
		
		boolean end = false;
		
		while(!end){
			
			System.out.println("Waiting for packet...");
			
			/** get the packet */
			DatagramPacket packetreceived = new DatagramPacket(datareceived, datareceived.length);
			
			fromSender.receive(packetreceived);
			
			/** unserialize to a RDTPacket object */
			RDTPacket packet = (RDTPacket) Serializer.toObject(packetreceived.getData());
			
			System.out.println("Packet with sequence number " + packet.getSeq() + " is received (last: " + packet.isLast() + ")");
		
			if(packet.getSeq() == nextseqnum && packet.isLast()){
				
				nextseqnum++;

				received.add(packet);
				
				System.out.println("Last packet is received");
				
				end = true;
				
			}else if(packet.getSeq() == nextseqnum){

				nextseqnum++;

				received.add(packet);

				System.out.println("Packet is stored in buffer");

			}else{
				System.out.println("Packet is discarded (not in order)");
			}
			
			/** initialize a RDTAck object */
			RDTAck ackObject = new RDTAck(nextseqnum);
			
			/** use Serializer */
			byte[] ackBytes = Serializer.toBytes(ackObject);
			
			
			DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, packetreceived.getAddress(), packetreceived.getPort());
			
			/** send packet with a probability of loss */
			if(Math.random() > probability){
				fromSender.send(ackPacket);
			}else{
				System.out.println("[X] Lost ACK with sequence number " + ackObject.getPacket());
			}
			
			System.out.println("Sending ACK to sequence number " + nextseqnum + " of " + ackBytes.length  + " bytes...");
			

		}
		
		/** print the received data */
		System.out.println(" *********************** DATA *********************** ");
		
		for(RDTPacket i : received){
			for(byte j: i.getData()){
				System.out.print((char) j);
			}
		}
		
	}
	
	
}
