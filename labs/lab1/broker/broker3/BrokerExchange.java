import java.io.*;
import java.net.*;
import java.util.Arrays;

public class BrokerExchange {
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		Socket lookupSocket = null;
		ObjectOutputStream lookupout = null;
		ObjectInputStream lookupin = null;

		// Connect to lookup.
		try {
			/* variables for hostname/port */
		    	// Don't hardwire!
			String hostname = null;
			int port = -1;
			
			if(args.length == 3) {
				hostname = args[0];
				port = Integer.parseInt(args[1]);
			} else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
			lookupSocket = new Socket(hostname, port);

			lookupout = new ObjectOutputStream(lookupSocket.getOutputStream());
			lookupin = new ObjectInputStream(lookupSocket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		// Make a lookup packet request
		BrokerPacket packetToLookup = new BrokerPacket();
		packetToLookup.type = BrokerPacket.LOOKUP_REQUEST;
		packetToLookup.symbol = args[2];
		lookupout.writeObject(packetToLookup);

		// Get reply from lookup
		BrokerPacket packetFromLookup;
		packetFromLookup = (BrokerPacket) lookupin.readObject();
	  
		// Get IP and port from lookup		
		Socket brokerSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
	
		/* variables for hostname/port */
		String hostname = packetFromLookup.locations[0].broker_host;
		int port = packetFromLookup.locations[0].broker_port;	

		// Connect to brokeri
		try {
			brokerSocket = new Socket(hostname, port);

			out = new ObjectOutputStream(brokerSocket.getOutputStream());
			in = new ObjectInputStream(brokerSocket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;

		System.out.print("Enter command or quit for exit:\n");
		System.out.print("> ");

		while ((userInput = stdIn.readLine()) != null && userInput.toLowerCase().indexOf("quit") == -1) {
	
			// Split strings into seperate parts
 			String parts[] = userInput.split(" ");

			/* make a new request packet */
			BrokerPacket packetToServer = new BrokerPacket();
			String command = parts[0].toLowerCase();
			String symbol = parts[1].toLowerCase();

			/* Check what type of request it is. */
            		if (command.equals("add")) {
				packetToServer.type = BrokerPacket.EXCHANGE_ADD;
			} else if (command.equals("update")) {
			    	packetToServer.type = BrokerPacket.EXCHANGE_UPDATE;
				    packetToServer.quote= Long.parseLong(parts[2], 10);
			} else if (command.equals("remove")) {
			    	packetToServer.type = BrokerPacket.EXCHANGE_REMOVE;
			} else {
				System.out.print("Unknown command...\n");

				/* re-print console prompt */
				System.out.print("> ");

				continue;			
			}


			packetToServer.symbol = symbol;
			out.writeObject(packetToServer);

			/* print server reply */
			BrokerPacket packetFromServer;
			packetFromServer = (BrokerPacket) in.readObject();

			if (packetFromServer.type == BrokerPacket.EXCHANGE_REPLY){
				int isError = packetFromServer.error_code;

				switch(isError) {
					case BrokerPacket.ERROR_INVALID_SYMBOL: 	System.out.print(symbol + " invalid.\n");
									continue;
					case BrokerPacket.ERROR_OUT_OF_RANGE: 	System.out.print(symbol + " out of range.\n");
									continue;
					case BrokerPacket.ERROR_SYMBOL_EXISTS: 	System.out.print(symbol + " exists.\n");
									continue;	
					case BrokerPacket.ERROR_INVALID_EXCHANGE: 	System.out.print(symbol + " invalid.\n");
									continue;	
					default: break; 
				}
                
                if (command.equals("add")) {
                    System.out.print(symbol + " added.\n");
                } else if (command.equals("update")) {
					System.out.print(symbol + " updated to " + String.valueOf(packetFromServer.quote) + ".\n");
			 	} else if (command.equals("remove")) {
					System.out.print(symbol + " removed.\n");
				} else {
					System.out.print("Unknown command...\n");
			    	}
			}


			/* re-print console prompt */
			System.out.print("> ");
		}

		/* tell server that i'm quitting */
		BrokerPacket packetToServer = new BrokerPacket();
		packetToServer.type = BrokerPacket.BROKER_BYE;
		//packetToServer.message = "Bye!";
		out.writeObject(packetToServer);

		out.close();
		in.close();
		stdIn.close();
		brokerSocket.close();
	}
}
