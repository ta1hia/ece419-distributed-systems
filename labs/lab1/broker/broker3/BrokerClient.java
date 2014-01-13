import java.io.*;
import java.net.*;

public class BrokerClient {
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		Socket brokerSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
                String exchange = null; // Save exchange name.
                
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


		System.out.print("Enter queries or quit for exit:");
		while ((userInput = stdIn.readLine()) != null && userInput.toLowerCase().indexOf("x") == -1) {
			
			
			/* re-print console prompt */
			System.out.print("> ");
	  	
                        // Split strings into seperate parts
                         String parts[] = userInput.split(" ");
                         
			/* make a new request packet */
			BrokerPacket packetToServer = new BrokerPacket();
			String command = parts[0].toLowerCase();
			
			/* Check what type of request it is. */
                        switch(command) {
                        	case "local":	exchange = parts[1];
						System.out.print(exchange + "as local");
                        			continue;
                                default: 	packetToServer.type = BrokerPacket.BROKER_REQUEST;
						packetToServer.symbol = userInput.toLowerCase();
						out.writeObject(packetToServer);
                                         	break;

                        }

			/* print server reply */
			BrokerPacket packetFromServer;
			packetFromServer = (BrokerPacket) in.readObject();

			if (packetFromServer.type == BrokerPacket.BROKER_QUOTE){
				int isError = packetFromServer.error_code;

                                switch(isError) {
                                        case ERROR_INVALID_SYMBOL:         System.out.print(packetFromServer.symbol + " invalid.");
                                                                        continue;
                                        case ERROR_OUT_OF_RANGE:         System.out.print(packetFromServer.symbol + " out of range.");
                                                                        continue;
                                        case ERROR_SYMBOL_EXISTS:         System.out.print(packetFromServer.symbol + " exists.");
                                                                        continue;        
                                        case ERROR_INVALID_EXCHANGE:         System.out.print(packetFromServer.symbol + " invalid.");
                                                                        continue;   
                                        case 0: System.out.println("Quote from broker: " + String.valueOf(packetFromServer.quote));
                                        default: break; 
                                }

				
			} else {
                		/* error returned - this case isn't handled in Broker1 */
                		System.out.println("Quote from broker: 0");
            		}
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
