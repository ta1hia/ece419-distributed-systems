import java.io.*;
import java.net.*;

public class BrokerClient {
    public static void main(String[] args) throws IOException,
           ClassNotFoundException {

               // Connect to lookup 
               Socket lookupSocket = null;
               ObjectOutputStream lookupout = null;
               ObjectInputStream lookupin = null;

               String lookupHostname = null;
               int lookupPort = -1;

               // Get IP and port from lookup		
               Socket brokerSocket = null;
               ObjectOutputStream out = null;
               ObjectInputStream in = null;

               try {
                   /* variables for hostname/port */
                   // Don't hardwire!

                   if(args.length == 2) {
                       lookupHostname = args[0];
                       lookupPort = Integer.parseInt(args[1]);
                   } else {
                       System.err.println("ERROR: Invalid arguments!");
                       System.exit(-1);
                   }
                   lookupSocket = new Socket(lookupHostname, lookupPort);

                   lookupout = new ObjectOutputStream(lookupSocket.getOutputStream());
                   lookupin = new ObjectInputStream(lookupSocket.getInputStream());

               } catch (UnknownHostException e) {
                   System.err.println("ERROR: Don't know where to connect!!");
                   System.exit(1);
               } catch (IOException e) {
                   System.err.println("ERROR: Couldn't get I/O for the connection.");
                   System.exit(1);
               }

               // Broker variables
               String hostname = null;
               int port = -1;

               BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
               String userInput;

               System.out.print("Enter symbol or quit for exit:\n");
               System.out.print("> ");

               while ((userInput = stdIn.readLine()) != null && !userInput.toLowerCase().equals("x") && !userInput.toLowerCase().equals("quit")) {

                   // Split strings into seperate parts		   
                   String parts[] = userInput.split(" ");

                   /* make a new request packet */
		   String input = parts[0];
                   String command = parts[0].toLowerCase();

                   /* make a new request packet */
                   if(command.equals("local")){ // Check if it's a request to lookup
		       String originalSymbol = parts[1];
                       String symbol = parts[1].toLowerCase();

                       // Make a lookup packet request
                       BrokerPacket packetToLookup = new BrokerPacket();
                       packetToLookup.type = BrokerPacket.LOOKUP_REQUEST;
                       packetToLookup.symbol = symbol;
                       lookupout.writeObject(packetToLookup);

                       // Get reply from lookup
                       BrokerPacket packetFromLookup;
                       packetFromLookup = (BrokerPacket) lookupin.readObject();

		       if(packetFromLookup.type == BrokerPacket.ERROR_INVALID_SYMBOL){
			   System.out.println("Broker does not exist...");			    	
			   System.out.print("> ");
			   continue;
		       }

                       /* global variables for hostname/port */
                       hostname = packetFromLookup.locations[0].broker_host;
                       port = packetFromLookup.locations[0].broker_port;	

                       // Connect to broker
                       try {
                           brokerSocket = new Socket(hostname, port);

                           out = new ObjectOutputStream(brokerSocket.getOutputStream());
                           in = new ObjectInputStream(brokerSocket.getInputStream());

                       } catch (UnknownHostException e) {
                           System.err.println("ERROR: Don't know where to connect!!\n");
                           System.exit(1);
                       } catch (IOException e) {
                           System.err.println("ERROR: Couldn't get I/O for the connection.\n");
                           System.exit(1);
                       }

                       System.out.print(originalSymbol + " as local.\n");
                       /* re-print console prompt */
                       System.out.print("> ");

                       continue;
                   }

                   if(hostname == null || port < 0) { // Client is currently not connected
                       System.out.println("Client is currently not connected to a broker... Use command 'local'.");			    	
                       System.out.print("> ");
                       continue;
                   }

                   // This is a request to broker	
                   BrokerPacket packetToServer = new BrokerPacket();
                   packetToServer.type = BrokerPacket.BROKER_REQUEST;
                   packetToServer.symbol = userInput.toLowerCase();
                   out.writeObject(packetToServer);

                   /* print server reply */
                   BrokerPacket packetFromServer;
                   packetFromServer = (BrokerPacket) in.readObject();

                   if (packetFromServer.type == BrokerPacket.BROKER_QUOTE){
                       int isError = packetFromServer.error_code;

                       switch(isError) {
                           case BrokerPacket.ERROR_INVALID_SYMBOL:         System.out.print(input + " invalid.\n");
			       break;
                           case BrokerPacket.ERROR_OUT_OF_RANGE:         System.out.print(input + " out of range.\n");
			       break;
		       case BrokerPacket.ERROR_SYMBOL_EXISTS:         System.out.print(input + " exists.\n");
			       break;        
                           case BrokerPacket.ERROR_INVALID_EXCHANGE:         System.out.print(input + " invalid.\n");
			       break;   
		       case 0: System.out.println("Quote from broker: " + String.valueOf(packetFromServer.quote)); break;
                           default: System.out.print("Invalid error...\n"); break; 
                       }
                   } else {
                       /* error returned - this case isn't handled in Broker1 */
                       System.out.println("Unknown packet type...");
                   }

                   /* re-print console prompt */
                   System.out.print("> ");
               }

               /* tell server that i'm quitting */
               if (out != null) {
                   BrokerPacket packetToServer = new BrokerPacket();
                   packetToServer.type = BrokerPacket.BROKER_BYE;
                   out.writeObject(packetToServer);
                   out.close();
                   in.close();
                   brokerSocket.close();
               }

               stdIn.close();
           }
}
