import java.net.*;
import java.io.*; 
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;


/* Online Broker server. 
 */
public class OnlineBroker {
    public static void main (String[] args) throws IOException {
        ServerSocket brokerSocket = null;
        boolean listening = true;

	Socket lookupSocket = null;
	ObjectOutputStream out = null;
	ObjectInputStream in = null;
    
        /* Create Online Broker server socket */
	// This is where server will be listening
        try { 
            if (args.length == 4) {
                brokerSocket = new ServerSocket(Integer.parseInt(args[0]));
            } else {
                System.err.println("Error: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

	// Connect to the BrokerLookup thread.
	try {
	    /* variables for default brokerLookup thread */
	    String hostname = "localhost";
	    int port = 4444;
		
	    if(args.length == 4) {
		hostname = args[0];
		port = Integer.parseInt(args[1]);
	    } else {
		System.err.println("ERROR: Invalid arguments!");
		System.exit(-1);
	    }
	    lookupSocket = new Socket(hostname, port);
	    out = new ObjectOutputStream(lookupSocket.getOutputStream());
	    in = new ObjectInputStream(lookupSocket.getInputStream());

	} catch (UnknownHostException e) {
	    System.err.println("ERROR: Don't know where to connect!!");
	    System.exit(1);
	} catch (IOException e) {
	    System.err.println("ERROR: Couldn't get I/O for the connection.");
	    System.exit(1);
	}

	// Register broker using BrokerLookup
	// $1 = hostname, $2 = port of BrokerLookupServer, $3 = port where I listen, $4 = key
	/* make a new request packet */
	BrokerPacket packetToLookup = new BrokerPacket();
	packetToLookup.type = BrokerPacket.LOOKUP_REQUEST;
	packetToLookup.symbol = args[3];
	packetToLookup.locations[0] = new BrokerLocation(InetAddress.getLocalHost().getHostName(), Integer.parseInt(args[2]));
	out.writeObject(packetToLookup);

        /* Store table into a hashmap */
        ConcurrentHashMap<String, Long> table = new ConcurrentHashMap<String, Long>();
        BufferedReader input = new BufferedReader(new FileReader(args[3]));
        String line = "";
        Long quote;
        while ((line = input.readLine()) != null) {
            String parts[] = line.split(" ");
            quote = Long.parseLong(parts[1], 10);
            table.put(parts[0], quote);
        }
        input.close();

        /* Set table quotes in OnlineBrokerHandlerThread */
        OnlineBrokerHandlerThread.setNasdaq(table);

        /* Listen for clients */
        while (listening) {
            new OnlineBrokerHandlerThread(brokerSocket.accept()).start();
        }
    }
}

