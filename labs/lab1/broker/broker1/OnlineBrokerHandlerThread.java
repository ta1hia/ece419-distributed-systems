import java.net.*;
import java.io.*; 
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineBrokerHandlerThread extends Thread {
    private Socket socket = null;
    private static ConcurrentHashMap<String, Long> nasdaq; /* thread-safe hashmap structure */

    public OnlineBrokerHandlerThread(Socket socket) {
        super("OnlineBrokerHandlerThread");
        this.socket = socket;
        System.out.println("Created new Thread to handle broker client");
    }

    public void run() {

		boolean gotByePacket = false;
		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			BrokerPacket packetFromClient;
			
			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			

			while (( packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
			/* create a packet to send reply back to client */
			    BrokerPacket packetToClient = new BrokerPacket();
				
                /* client packet is BROKER_REQUEST */
				if(packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
                    if (packetFromClient.symbol == null || nasdaq.get(packetFromClient.symbol) == null) {
                        /* valid symbol could not be processed */
					    System.out.println("From Client: request error");
                        packetToClient.type = BrokerPacket.BROKER_ERROR;
                        packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
                    } else {
                        packetToClient.type = BrokerPacket.BROKER_QUOTE;
					    System.out.println("From Client: " + packetFromClient.symbol);
					    System.out.println("Replying to Client: " + nasdaq.get(packetFromClient.symbol));

                        packetToClient.quote = nasdaq.get(packetFromClient.symbol);
                    }
				
					/* send reply back to client */
					toClient.writeObject(packetToClient);
					
					/* wait for next packet */
					continue;
				}
				
				/* Sending an ECHO_NULL || ECHO_BYE means quit */
				if (packetFromClient.type == BrokerPacket.BROKER_NULL || packetFromClient.type == BrokerPacket.BROKER_BYE) {
					gotByePacket = true;
					packetToClient.type = BrokerPacket.BROKER_BYE;
					System.out.println("Client is exiting");
					toClient.writeObject(packetToClient);
					break;
				}
				
				/* if code comes here, there is an error in the packet */
				System.err.println("ERROR: Unknown ECHO_* packet!!");
				System.exit(-1);
			}
			
			/* cleanup when client exits */
			fromClient.close();
			toClient.close();
			socket.close();

		} catch (IOException e) {
			if(!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if(!gotByePacket)
				e.printStackTrace();
		}
    }

    /* Accessors */
    public static void setNasdaq (ConcurrentHashMap <String, Long> quotes) {
        OnlineBrokerHandlerThread.nasdaq = quotes;
    }

}
