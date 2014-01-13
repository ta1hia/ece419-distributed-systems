import java.io.*;
import java.net.*;

public class BrokerExchange {
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		Socket brokerSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		try {
			/* variables for hostname/port */
			String hostname = "localhost";
			int port = 4444;
			
			if(args.length == 2) {
				hostname = args[0];
				port = Integer.parseInt(args[1]);
			} else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
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

		System.out.print("Enter command or quit for exit:");
		System.out.print("");
		System.out.print("> ");

		while ((userInput = stdIn.readLine()) != null && userInput.toLowerCase().indexOf("quit") == -1) {

			// Split strings into seperate parts
 			String parts[] = userInput.split(" ");

			/* make a new request packet */
			BrokerPacket packetToServer = new BrokerPacket();
			String command = parts[0].toLowerCase();

			/* Check what type of request it is. */
			switch(command) {
				case "add": 	packetToServer.type = BrokerPacket.EXCHANGE_ADD;
					    	packetToServer.symbol = parts[1];
						break;
				case "update": 	packetToServer.type = BrokerPacket.EXCHANGE_UPDATE;
					    	packetToServer.symbol = parts[1];
					    	packetToServer.quote= parts[2];
						break;
				case "remove": 	packetToServer.type = BrokerPacket.EXCHANGE_REMOVE;
					    	packetToServer.symbol = parts[1];
						break;	
				default: System.out.print("Unknown command...");
					 continue;

			}

			out.writeObject(packetToServer);

			/* print server reply */
			BrokerPacket packetFromServer;
			packetFromServer = (BrokerPacket) in.readObject();

			if (packetFromServer.type == BrokerPacket.EXCHANGE_REPLY){
				int isError = packetFromServer.error_code;

				switch(isError) {
					case ERROR_INVALID_SYMBOL: 	System.out.print(packetFromServer.symbol + " invalid.");
									continue;
					case ERROR_OUT_OF_RANGE: 	System.out.print(packetFromServer.symbol + " out of range.");
									continue;
					case ERROR_SYMBOL_EXISTS: 	System.out.print(packetFromServer.symbol + " exists.");
									continue;	
					case ERROR_INVALID_EXCHANGE: 	System.out.print(packetFromServer.symbol + " invalid.");
									continue;	
					default: break; 
				}

				switch(command) {
					case "add": 	System.out.print(packetFromServer.symbol + " added.");
							break;
					case "update": 	System.out.print(packetFromServer.symbol + " updated to " + packetFromServer.quote + ".");
							break;
					case "remove": 	System.out.print(packetFromServer.symbol + " removed.");
							break;	
					default: System.out.print("Unknown command...");
						 break; 
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
