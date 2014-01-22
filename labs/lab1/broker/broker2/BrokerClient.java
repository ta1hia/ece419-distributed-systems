import java.io.*;
import java.net.*;

public class BrokerClient {
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

		System.out.print("Enter symbol or quit for exit:\n");
		System.out.print("> ");

		while ((userInput = stdIn.readLine()) != null && !userInput.toLowerCase().equals("x") && !userInput.toLowerCase().equals("exit")) {
						
			/* make a new request packet */
			BrokerPacket packetToServer = new BrokerPacket();
			packetToServer.type = BrokerPacket.BROKER_REQUEST;
			String originalSymbol = userInput;
			packetToServer.symbol = userInput.toLowerCase();
			out.writeObject(packetToServer);

			/* print server reply */
			BrokerPacket packetFromServer;
			packetFromServer = (BrokerPacket) in.readObject();

			if (packetFromServer.type == BrokerPacket.BROKER_QUOTE){
				int isError = packetFromServer.error_code;

                                switch(isError) {
                                        case BrokerPacket.ERROR_INVALID_SYMBOL:         System.out.print(originalSymbol + " invalid.\n");
					    break;
                                        case BrokerPacket.ERROR_OUT_OF_RANGE:         System.out.print(originalSymbol + " out of range.\n");
					    break;
                                        case BrokerPacket.ERROR_SYMBOL_EXISTS:         System.out.print(originalSymbol + " exists.\n");
					    break;
                                        case BrokerPacket.ERROR_INVALID_EXCHANGE:         System.out.print(originalSymbol + " invalid.\n");
					    break;         
                                        case 0: System.out.println("Quote from broker: " + String.valueOf(packetFromServer.quote));
                                        default: break; 
                                }

				
			} else {
			    	/* Recieved  a different kind of packet type... What is it?! */
                		System.out.println("Unknown packet type...\n");
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
