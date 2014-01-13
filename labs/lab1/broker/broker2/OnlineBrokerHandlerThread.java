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
				
                /* BROKER_REQUEST */
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
				
				/* BROKER_NULL || BROKER_BYE */
				if (packetFromClient.type == BrokerPacket.BROKER_NULL || packetFromClient.type == BrokerPacket.BROKER_BYE) {
					gotByePacket = true;
					packetToClient.type = BrokerPacket.BROKER_BYE;
					System.out.println("Client is exiting");
					toClient.writeObject(packetToClient);
					break;
				}

                /* EXCHANGE_ADD */
                if (packetFromClient.type == BrokerPacket.EXCHANGE_ADD) {
                    packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
                    if (nasdaq.get(packetFromClient.symbol) == null) {
                        packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
                        continue;
                    } 

                    nasdaq.put(packetFromClient.symbol, Long.valueOf(0));
                    OnlineBrokerHandlerThread.updateNasdaqTable();
                    packetToClient.symbol = packetFromClient.symbol;
                    packetToClient.error_code = 0;
                    continue;
                }

                /* EXCHANGE_UPDATE */
                if (packetFromClient.type == BrokerPacket.EXCHANGE_UPDATE) {
                    packetToClient.type = BrokerPacket.EXCHANGE_REPLY;

                    if (nasdaq.get(packetFromClient.symbol) == null) {
                        packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
                        continue;
                    } else if (packetFromClient.quote > 300 || packetFromClient.quote < 1) {
                        packetToClient.error_code = BrokerPacket.ERROR_OUT_OF_RANGE;
                        continue;
                    }

                    nasdaq.put(packetFromClient.symbol, packetFromClient.quote);
                    OnlineBrokerHandlerThread.updateNasdaqTable();
                    packetToClient.error_code = 0;
                    continue;
                }
                
                /* EXCHANGE_REMOVE */
                if (packetFromClient.type == BrokerPacket.EXCHANGE_REMOVE) {
                    packetToClient.type = BrokerPacket.EXCHANGE_REPLY;

                    if (nasdaq.get(packetFromClient.symbol) == null) {
                        packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
                        continue;
                    } 

                    nasdaq.remove(packetFromClient.symbol);
                    OnlineBrokerHandlerThread.updateNasdaqTable();
                    packetToClient.error_code = 0;
                    continue;
                }
				
				/* if code comes here, there is an error in the packet */
				System.err.println("ERROR: Unknown packet!!");
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

    private static void updateNasdaqTable() {
        /* Clear nasdaq table and write updated entries */
        try {
            FileOutputStream nasdaqWriter = new FileOutputStream("nasdaq");

            /* Clear contents of nasdaq */
            nasdaqWriter.write("".getBytes());

            /* Copy updated contents of hashmap into nasdaq */
            ObjectOutputStream nasdaqOOS = new ObjectOutputStream(nasdaqWriter);
            nasdaqOOS.writeObject(nasdaq);

            nasdaqOOS.flush();
            nasdaqOOS.close();
            nasdaqWriter.close();
        } catch (Exception e) {
            System.out.println("File (nasdaq) update error");
        }
    }

}
