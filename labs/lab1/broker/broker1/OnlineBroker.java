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
    
        /* Create Online Broker server socket */
        try { 
            if (args.length == 1) {
                brokerSocket = new ServerSocket(Integer.parseInt(args[0]));
            } else {
                System.err.println("Error: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        /* Store nasdaq table into a hashmap */
        ConcurrentHashMap<String, Long> nasdaq = new ConcurrentHashMap<String, Long>();
        BufferedReader in = new BufferedReader(new FileReader("nasdaq"));
        String line = "";
        Long quote;
        while ((line = in.readLine()) != null) {
            String parts[] = line.split(" ");
            quote = Long.parseLong(parts[1], 10);
            nasdaq.put(parts[0], quote);
        }
        in.close();

        /* Set nasdaq quotes in OnlineBrokerHandlerClass */
        OnlineBrokerHandlerThread.setNasdaq(nasdaq);

        /* Listen for clients */
        while (listening) {
            new OnlineBrokerHandlerThread(brokerSocket.accept()).start();
        }
    }
}

