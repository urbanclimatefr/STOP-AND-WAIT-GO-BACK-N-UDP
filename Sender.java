import java.io.FileInputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.ArrayList;

public class Sender {

	/** maximum segment size (quantity of data from the application layer in the segment) */
	public static final int mss = 4;

	/** probability of loss during packet sending */
	public static final double probability = 0.01;

	/** window size in the first sending while loop (number of packets sent without ACKing) */
	public static final int window_size_first = 1;

	/** window size in the second sending while loop (number of packets sent without ACKing) */
	public static final int window_size_second = 5;
	
	/** Time (ms) before Resending all the non-acked packets */
	public static final int timer = 60;


	public static void main(String[] args) throws Exception{

		/** get port number from input argument */
		int port = Integer.parseInt(args[1]);
		
		/** sequence number of the last packet sent */
		int nextseqnum = 0;
		
		/** sequence number of the last ACKed packet */
		int send_base = 0;

		/** Read in a file using an InputStream and byte array */
		InputStream ins = new FileInputStream(args[2]); /** get filename from input argument */	

		byte[] fileBytes = ins.readAllBytes();

		System.out.println("Data size: " + fileBytes.length + " bytes");

		ins.close();

		/** last packet sequence number */	
		int lastseqnum = (int) Math.ceil((double) fileBytes.length / mss);

		System.out.println("Number of packets to be sent: " + lastseqnum);

		DatagramSocket toReceiver = new DatagramSocket(); 

		/** address of the receiver */
		InetAddress receiverAddress = InetAddress.getByName(args[0]); // get host from input argument
		
		/** list of all the packets to be sent */
		ArrayList<RDTPacket> sent = new ArrayList<RDTPacket>();

		while(true){

			/** first Sending loop */
			while(nextseqnum - send_base < window_size_first && nextseqnum < lastseqnum){

				/** array to store part of the bytes to be sent */
				byte[] filePacketBytes = new byte[mss];

				/** copy segment of data bytes to array */
				filePacketBytes = Arrays.copyOfRange(fileBytes, nextseqnum*mss, nextseqnum*mss + mss);

				/** initialize RDTPacket object */
				RDTPacket rdtPacketObject = new RDTPacket(nextseqnum, filePacketBytes, (nextseqnum == lastseqnum-1) ? true : false);

				/** serialize the RDTPacket object */
				byte[] sendData = Serializer.toBytes(rdtPacketObject);

				/** initialize the packet */
				DatagramPacket packet = new DatagramPacket(sendData, sendData.length, receiverAddress, port);

				System.out.println("Sending packet with sequence number " + nextseqnum +  " and size " + sendData.length + " bytes...");

				/** add packet to the sent list */
				sent.add(rdtPacketObject);
				
				/** send with a probability of loss */
				if(Math.random() > probability){
					toReceiver.send(packet);
				}else{
					System.out.println("[X] Lost packet with sequence number " + nextseqnum);
				}

				/** increase the last sent */
				nextseqnum++;

			} /** end of first sending while */
			
			/** second Sending loop */
						while(nextseqnum - send_base < window_size_second && nextseqnum < lastseqnum){

							/** array to store part of the bytes to be sent */
							byte[] filePacketBytes = new byte[mss];

							/** copy segment of data bytes to array */
							filePacketBytes = Arrays.copyOfRange(fileBytes, nextseqnum*mss, nextseqnum*mss + mss);

							/** initialize RDTPacket object */
							RDTPacket rdtPacketObject = new RDTPacket(nextseqnum, filePacketBytes, (nextseqnum == lastseqnum-1) ? true : false);

							/** serialize the RDTPacket object */
							byte[] sendData = Serializer.toBytes(rdtPacketObject);

							/** initialize the packet */
							DatagramPacket packet = new DatagramPacket(sendData, sendData.length, receiverAddress, port);

							System.out.println("Sending packet with sequence number " + nextseqnum +  " and size " + sendData.length + " bytes...");

							/** add packet to the sent list */
							sent.add(rdtPacketObject);
							
							/** send with a probability of loss */
							if(Math.random() > probability){
								toReceiver.send(packet);
							}else{
								System.out.println("[X] Lost packet with sequence number " + nextseqnum);
							}

							/** increase the last sent */
							nextseqnum++;

						} /** end of second sending while */			
			
			/** byte array for the ACK sent by the receiver */
			byte[] ackBytes = new byte[40];
			
			/** initialize packet for the ACK */
			DatagramPacket ack = new DatagramPacket(ackBytes, ackBytes.length);
			
			try{
				/** if an ACK was not received during the specified time specified*/
				toReceiver.setSoTimeout(timer);
				
				/** receive the packet */
				toReceiver.receive(ack);
				
				/** unserialize the RDTAck object */
				RDTAck ackObject = (RDTAck) Serializer.toObject(ack.getData());
				
				System.out.println("Received ACK for " + ackObject.getPacket());
				
				/** if this ack is for the last packet, stop the sender */
				if(ackObject.getPacket() == lastseqnum){
					break;
				}
				
				send_base = Math.max(send_base, ackObject.getPacket());
				
			}catch(SocketTimeoutException e){
				/** send all the sent but non-acked packets */
				
				for(int i = send_base; i < nextseqnum; i++){
					
					/** serialize the RDTPacket object */
					byte[] sendData = Serializer.toBytes(sent.get(i));

					/** initialize the packet */
					DatagramPacket packet = new DatagramPacket(sendData, sendData.length, receiverAddress, port);
					
					/** Send with a probability of packet loss */
					if(Math.random() > probability){
						toReceiver.send(packet);
					}else{
						System.out.println("[X] Lost packet with sequence number " + sent.get(i).getSeq());
					}

					System.out.println("Resending packet with sequence number " + sent.get(i).getSeq() +  " and size " + sendData.length + " bytes");
				}
			}
			
		
		}
		System.out.println("Transmission is finished");

	}

}
