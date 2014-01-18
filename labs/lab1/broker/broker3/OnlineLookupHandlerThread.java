import java.net.*;
import java.io.*; 
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/*
  Lookup / Naming service thread
 */
public class OnlineLookupHandlerThread extends Thread {
    private Socket socket = null;
    private static ConcurrentHashMap<String, String> table; /* thread-safe hashmap structure */

    public OnlineLookupHandlerThread(Socket socket) {
        super("OnlineLookupHandlerThread");
        this.socket = socket;
        System.out.println("Created new Thread to handle lookup requests");
    }

    public void run() {
		boolean gotByePacket = false;
		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			BrokerPacket packetFromClient;
			
			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			

			while ((packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */
			    	BrokerPacket packetToClient = new BrokerPacket();

				/* LOOKUP_REQUEST */
				// Client wants to find a corresponding IP and port for a given broker name
				if(packetFromClient.type == BrokerPacket.LOOKUP_REQUEST) {
					System.out.println("From Client: " + packetFromClient.symbol);
					if (packetFromClient.symbol == null || !table.containsKey(packetFromClient.symbol)) {
						/* valid symbol could not be processed */
						System.out.println("From Client: request error");
						System.out.println(table.toString());
						System.out.println(table.get(packetFromClient.symbol));

						packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
					} else {
						packetToClient.type = BrokerPacket.LOOKUP_REPLY;

						System.out.println("Lookup replying to client: " + OnlineLookupHandlerThread.getIP(packetFromClient.symbol));

						System.out.println("Lookup replying to client: " + OnlineLookupHandlerThread.getPort(packetFromClient.symbol));
						//packetToClient.quote = table.get(packetFromClient.symbol);
					}
					toClient.writeObject(packetToClient);
					continue;
				}
				

             	   		// /* LOOKUP_REGISTER */
				// if (packetFromClient.type == BrokerPacket.EXCHANGE_ADD) {
				//     	packetToClient.type = BrokerPacket.EXCHANGE_REPLY;

				// 	System.out.println("From Client: EXCHANGE_ADD ");
				// 	System.out.println("From Client: " + packetFromClient.symbol);

				//  	if (table.get(packetFromClient.symbol) != null) {
				// 		System.out.println("ERROR: symbol already exists");
				// 		packetToClient.error_code = BrokerPacket.ERROR_SYMBOL_EXISTS;
				//     	} else {
				// 		table.put(packetFromClient.symbol, Long.valueOf(0));
				// 		OnlineLookupHandlerThread.updateTable();

				// 		System.out.println("To Client: add success ");
				// 		packetToClient.symbol = packetFromClient.symbol;
				// 		packetToClient.error_code = 0;
				// 	}

				// 	toClient.writeObject(packetToClient);
				//     	continue;
				// }

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
    public static void setTable (ConcurrentHashMap <String, String> quotes) {
        OnlineLookupHandlerThread.table = quotes;
    }

    private static String getIP(String broker) {
	String query = table.get(broker);
	String parts[] = query.split(" ");

	return parts[0];
    }


    private static String getPort(String broker) {
	String query = table.get(broker);
	String parts[] = query.split(" ");

	return parts[1];
    }

    private static void updateTable() {
        /* Clear table table and write updated entries */
        try {
            FileWriter tableWriter = new FileWriter("table");

            /* Clear contents of table */
            /* Copy updated contents of hashmap into table */
            BufferedWriter out = new BufferedWriter(tableWriter);
            out.write("");
            out.flush();

            int count = 0;
            Iterator<Entry<String, String>> it = table.entrySet().iterator();

            while (it.hasNext() && count < table.size()) {
                Map.Entry<String, String> pairs = it.next();
                out.write(pairs.getKey() + " " + pairs.getValue() + "\n");
                count++;
            }

            out.close();
            tableWriter.close();
        } catch (Exception e) {
            System.out.println("File (table) update error");
        }
    }

}
