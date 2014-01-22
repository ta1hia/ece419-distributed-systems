import java.net.*;
import java.io.*; 
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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
		    System.out.println("From Client: " + packetFromClient.symbol);

		    packetToClient.type = BrokerPacket.BROKER_QUOTE;
		    if (packetFromClient.symbol == null || !nasdaq.containsKey(packetFromClient.symbol)) {
			/* valid symbol could not be processed */
			System.out.println("From Client: request error");
                        System.out.println(nasdaq.toString());
                        System.out.println(nasdaq.get(packetFromClient.symbol));
                        packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
                    } else {
                        packetToClient.error_code = 0;
			System.out.println("Replying to Client: " + nasdaq.get(packetFromClient.symbol));
                        packetToClient.quote = nasdaq.get(packetFromClient.symbol);
                    }
				
		    toClient.writeObject(packetToClient);
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

		    System.out.println("From Client: EXCHANGE_ADD ");
		    System.out.println("From Client: " + packetFromClient.symbol);

                    if (nasdaq.get(packetFromClient.symbol) != null) {
			System.out.println("ERROR: symbol already exists");
                        packetToClient.error_code = BrokerPacket.ERROR_SYMBOL_EXISTS;
                    } else {
                        nasdaq.put(packetFromClient.symbol, Long.valueOf(0));
                        OnlineBrokerHandlerThread.updateNasdaqTable();

			System.out.println("To Client: add success ");
                        packetToClient.symbol = packetFromClient.symbol;
                        packetToClient.error_code = 0;
                    }
		    toClient.writeObject(packetToClient);
                    continue;
                }

                /* EXCHANGE_UPDATE */
                if (packetFromClient.type == BrokerPacket.EXCHANGE_UPDATE) {
                    packetToClient.type = BrokerPacket.EXCHANGE_REPLY;

		    System.out.println("From Client: EXCHANGE_UPDATE ");
		    System.out.println("From Client: " + packetFromClient.symbol);
		    System.out.println("From Client: " + packetFromClient.quote);

                    if (nasdaq.get(packetFromClient.symbol) == null) {
			System.out.println("ERROR: symbol does not exist");
                        packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
                    } else if (packetFromClient.quote > 300 || packetFromClient.quote < 1) {
			System.out.println("ERROR: quote out of range ");
                        packetToClient.error_code = BrokerPacket.ERROR_OUT_OF_RANGE;
                    } else {
                        nasdaq.put(packetFromClient.symbol, packetFromClient.quote);
                        OnlineBrokerHandlerThread.updateNasdaqTable();
                        System.out.println("To Client: update success ");
                        packetToClient.error_code = 0;
                        packetToClient.quote = packetFromClient.quote;
                    }
		    toClient.writeObject(packetToClient);
                    continue;
                }
                
                /* EXCHANGE_REMOVE */
                if (packetFromClient.type == BrokerPacket.EXCHANGE_REMOVE) {
                    packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
                    
                    System.out.println("From Client: EXCHANGE_REMOVE ");
		    System.out.println("From Client: " + packetFromClient.symbol);

                    if (nasdaq.get(packetFromClient.symbol) == null) {
			System.out.println("ERROR: symbol does not exist");
                        packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
                    } else {
                        nasdaq.remove(packetFromClient.symbol);
                        OnlineBrokerHandlerThread.updateNasdaqTable();
			System.out.println("To Client: remove success ");
                        packetToClient.error_code = 0;
                    }
		    toClient.writeObject(packetToClient);
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

    private static void updateNasdaq() {

    }

    private static void updateNasdaqTable() {
        /* Clear nasdaq table and write updated entries */
        try {
            FileWriter nasdaqWriter = new FileWriter("nasdaq");

            /* Clear contents of nasdaq */
            /* Copy updated contents of hashmap into nasdaq */
            BufferedWriter out = new BufferedWriter(nasdaqWriter);
            out.write("");
            out.flush();

            int count = 0;
            Iterator<Entry<String, Long>> it = nasdaq.entrySet().iterator();

            while (it.hasNext() && count < nasdaq.size()) {
                Map.Entry<String, Long> pairs = it.next();
                out.write(pairs.getKey() + " " + pairs.getValue() + "\n");
                count++;
            }

            out.close();
            nasdaqWriter.close();
        } catch (Exception e) {
            System.out.println("File (nasdaq) update error");
        }
    }

}
