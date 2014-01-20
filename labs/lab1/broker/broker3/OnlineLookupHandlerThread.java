import java.net.*;
import java.io.*; 
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Enumeration;
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

				/* LOOKUP_REGISTER */
				// Client wants to find a corresponding IP and port for a given broker name
				if(packetFromClient.type == BrokerPacket.LOOKUP_REGISTER) {
					System.out.println("From Broker: LOOKUP_REGISTER ");
					System.out.println("From Client: " + packetFromClient.symbol);
					if (packetFromClient.symbol == null) { 
						/* valid symbol could not be processed */
						System.out.println("From Broker: request error");
						System.out.println(table.toString());
						System.out.println(table.get(packetFromClient.symbol));

						packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
					} else if(!table.containsKey(packetFromClient.symbol)){ // New broker is trying to register!
						packetToClient.type = BrokerPacket.LOOKUP_REPLY;
						String temp_IP = packetFromClient.locations[0].broker_host;
						int temp_port = packetFromClient.locations[0].broker_port;

						System.out.println("Lookup is registering new broker.");
						System.out.println("IP: " + temp_IP + " Port: " + temp_port);

						table.put(packetFromClient.symbol, temp_IP + " " + temp_port );
						OnlineLookupHandlerThread.updateTable();

						System.out.println("To Broker: registration  success ");
						packetToClient.symbol = packetFromClient.symbol;
						packetToClient.error_code = 0;

					} else { // Broker already exists.						
						packetToClient.type = BrokerPacket.LOOKUP_REPLY;
						String temp_IP = packetFromClient.locations[0].broker_host;
						int temp_port = packetFromClient.locations[0].broker_port;

						System.out.println("Re-registering broker.");
						System.out.println("IP: " + temp_IP + " Port: " + temp_port);

						table.put(packetFromClient.symbol, temp_IP + " " + temp_port );
						OnlineLookupHandlerThread.updateTable();
						System.out.println("To Client: update success ");
						packetToClient.error_code = 0;
				        	
					}
					toClient.writeObject(packetToClient);
					continue;
				}
				
				/* LOOKUP_REQUEST */
				if(packetFromClient.type == BrokerPacket.LOOKUP_REQUEST) {
					System.out.println("From Client/Exchange: " + packetFromClient.symbol);
					if (packetFromClient.symbol == null || !table.containsKey(packetFromClient.symbol)) {
						/* valid symbol could not be processed */
						System.out.println("From Client: request error");
						System.out.println(table.toString());
						System.out.println(table.get(packetFromClient.symbol));

						packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
					} else {
						packetToClient.type = BrokerPacket.BROKER_QUOTE;
						
						String symbol = packetFromClient.symbol;
						String host = OnlineLookupHandlerThread.getHost(symbol);
						int port = OnlineLookupHandlerThread.getPort(symbol); 
						packetToClient.symbol = symbol;
						packetToClient.locations = new BrokerLocation[1];
						packetToClient.locations[0] = new BrokerLocation(host, port);	

						System.out.println("Replying to Client: " + symbol + " " + host + " " + port);					
					}
					toClient.writeObject(packetToClient);
					continue;
				}

             	   		/* BROKER_FORWARD */
				// Return all locations of brokers
				if (packetFromClient.type == BrokerPacket.BROKER_FORWARD) {
				    packetToClient.type = BrokerPacket.LOOKUP_REPLY;

				    System.out.println("From Broker: BROKER_FORWARD ");

				    String symbol = packetFromClient.symbol;
				    Object[] keys = table.keySet().toArray();
				    int size = table.size(); 
				    packetToClient.symbol = symbol;
				    packetToClient.num_locations = size;
				    packetToClient.locations = new BrokerLocation[size];

				    for(int i = 0; i < size; i++){
					String host = keys[i].toString();
				    	int port = OnlineLookupHandlerThread.getPort(host);

				    	System.out.println("Iteration:" + i + "... Saving host: " + host + " port: " + port);
				    	packetToClient.locations[i] = new BrokerLocation(host, port);				    
				    }
				
				    System.out.println("From Broker: Sending list of brokers back.");
				    System.out.println("num_locations is " + packetToClient.num_locations);
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
    public static void setTable (ConcurrentHashMap <String, String> quotes) {
        OnlineLookupHandlerThread.table = quotes;
    }

    private static String getHost(String broker) {
	String query = table.get(broker);
	String parts[] = query.split(" ");

	return parts[0];
    }

    private static int getPort(String broker) {
	String query = table.get(broker);
	String parts[] = query.split(" ");

	return Integer.valueOf(parts[1]);
    }

    private static void updateTable() {
        /* Clear table table and write updated entries */
        try {
            FileWriter tableWriter = new FileWriter("lookuptable");

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
