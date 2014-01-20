import java.net.*;
import java.io.*; 
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class OnlineBrokerHandlerThread extends Thread {
    private Socket socket = null;
    private static ConcurrentHashMap<String, Long> table; /* thread-safe hashmap structure */
    private String lookupHostname = null;
    private int lookupPort = -1;
    private static String brokerName = null;

    public OnlineBrokerHandlerThread(Socket socket, String hostname, int port) {
        super("OnlineBrokerHandlerThread");
        this.socket = socket;
	this.lookupHostname = hostname;
	this.lookupPort = port;
        System.out.println("Created new Thread to handle broker client");
    }

    public void run() {

	boolean gotByePacket = false;
		
	try {
	    // Connect to lookup!
	    Socket lookupSocket= new Socket(lookupHostname, lookupPort);;
	    ObjectOutputStream lookupout = new ObjectOutputStream(lookupSocket.getOutputStream());
	    ObjectInputStream lookupin = new ObjectInputStream(lookupSocket.getInputStream());

	    /* stream to read from client */
	    ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
	    BrokerPacket packetFromClient;
			
	    /* stream to write back to client */
	    ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			

	    while ((packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
		/* create a packet to send reply back to client */
		BrokerPacket packetToClient = new BrokerPacket();

		/* BROKER_FORWARD */
		if(packetFromClient.type == BrokerPacket.BROKER_FORWARD) {				    	
		    System.out.println("From another broker: " + packetFromClient.symbol);

		    if (packetFromClient.symbol == null || !table.containsKey(packetFromClient.symbol)) {
			/* valid symbol could not be processed */
			System.out.println("From Client: request error");
			System.out.println(table.toString());
			System.out.println(table.get(packetFromClient.symbol));
			packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
		    } else {
			packetToClient.type = BrokerPacket.BROKER_QUOTE;
			System.out.println("Replying to Client: " + table.get(packetFromClient.symbol));
			packetToClient.quote = table.get(packetFromClient.symbol);
		    }

		    toClient.writeObject(packetToClient);
		    continue;
		}

		/* BROKER_REQUEST */
		if(packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
		    System.out.println("From Client: " + packetFromClient.symbol);
		    if (packetFromClient.symbol == null || !table.containsKey(packetFromClient.symbol)) {
			/* valid symbol could not be processed */
			System.out.println("From Client: Could not find symbol " + packetFromClient.symbol);
			System.out.println(table.toString());
					     
			// Search other brokers if they have it
			System.out.println("Find symbol in other brokers via Lookup."); 
		
			/* make a new request packet */
			BrokerPacket packetToLookup = new BrokerPacket();
			packetToLookup.type = BrokerPacket.BROKER_FORWARD;
			packetToLookup.symbol = packetFromClient.symbol;

			lookupout.writeObject(packetToLookup);

			// Get reply from lookup
			BrokerPacket packetFromLookup;
			packetFromLookup = (BrokerPacket) lookupin.readObject();
	  	
			boolean resultFound  = false;

			System.out.println("Searching all brokers..."); 
			if(packetFromLookup.type == BrokerPacket.LOOKUP_REPLY){
			    // Traverse brokers until until symbol is found
			    int num_locations = packetFromLookup.num_locations;

			    System.out.println("num_locations from lookup " + num_locations); 
			    for(int i = 0; i < num_locations; i++){						    
				// Connect to brokers.

				// Temp variables
				Socket temp_brokerSocket = null;
				ObjectOutputStream temp_out = null;
				ObjectInputStream temp_in = null;

				String temp_hostname = null;
				int temp_port = -1;

				temp_hostname = packetFromLookup.locations[i].broker_host;
				temp_port = packetFromLookup.locations[i].broker_port;	


				System.out.println("Iteration:" + i + " Host: " + temp_hostname + " Port: " + temp_port); 

				// Connect to broker
				try {
				    temp_brokerSocket = new Socket(temp_hostname, temp_port);

				    temp_out = new ObjectOutputStream(temp_brokerSocket.getOutputStream());
				    temp_in = new ObjectInputStream(temp_brokerSocket.getInputStream());

				} catch (UnknownHostException e) {
				    System.err.println("ERROR: Don't know where to connect!!");
				    //System.exit(1);
				} catch (IOException e) {
				    System.err.println("ERROR: Couldn't get I/O for the connection.");
				    //System.exit(1);
				}

				// Request BROKER_FORWARD to broker
				/* make a new request packet */
				BrokerPacket packetToBroker = new BrokerPacket();
				packetToBroker.type = BrokerPacket.BROKER_FORWARD;
				packetToBroker.symbol = packetFromClient.symbol;

				temp_out.writeObject(packetToBroker);

				// Get reply from lookup
				BrokerPacket packetFromBroker;
				packetFromBroker = (BrokerPacket) temp_in.readObject();
						    
				if(packetFromBroker.type  == BrokerPacket.BROKER_QUOTE){
				    resultFound = true;
				    packetToClient.quote = packetFromBroker.quote;
				    break;
				}
						

			    }
			}

			System.out.println("Searching complete..."); 
			if(resultFound)
			    packetToClient.type = BrokerPacket.BROKER_QUOTE;	
			else
			    packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;

					       
		    } else {
			packetToClient.type = BrokerPacket.BROKER_QUOTE;

			System.out.println("Replying to Client: " + table.get(packetFromClient.symbol));
			packetToClient.quote = table.get(packetFromClient.symbol);
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

		    if (table.get(packetFromClient.symbol) != null) {
			System.out.println("ERROR: symbol already exists");
			packetToClient.error_code = BrokerPacket.ERROR_SYMBOL_EXISTS;
		    } else {
			table.put(packetFromClient.symbol, Long.valueOf(0));
			OnlineBrokerHandlerThread.updateTable();

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

		    if (table.get(packetFromClient.symbol) == null) {
			System.out.println("ERROR: symbol does not exist");
			packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
		    } else if (packetFromClient.quote > 300 || packetFromClient.quote < 1) {
			System.out.println("ERROR: quote out of range ");
			packetToClient.error_code = BrokerPacket.ERROR_OUT_OF_RANGE;
		    } else {
			table.put(packetFromClient.symbol, packetFromClient.quote);
			OnlineBrokerHandlerThread.updateTable();
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

		    if (table.get(packetFromClient.symbol) == null) {
			System.out.println("ERROR: symbol does not exist");
			packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
		    } else {
			table.remove(packetFromClient.symbol);
			OnlineBrokerHandlerThread.updateTable();
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
    public static void setTable (ConcurrentHashMap <String, Long> quotes, String name) {
        OnlineBrokerHandlerThread.table = quotes;
	brokerName = name;
    }


    private static void updateTable() {
        /* Clear table table and write updated entries */
        try {
            FileWriter tableWriter = new FileWriter(brokerName);

            /* Clear contents of table */
            /* Copy updated contents of hashmap into table */
            BufferedWriter out = new BufferedWriter(tableWriter);
            out.write("");
            out.flush();

            int count = 0;
            Iterator<Entry<String, Long>> it = table.entrySet().iterator();

            while (it.hasNext() && count < table.size()) {
                Map.Entry<String, Long> pairs = it.next();
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
