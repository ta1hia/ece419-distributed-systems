import java.net.*;
import java.io.*; 
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;


/* 
   Lookup / Naming service
   Stores key associated with all clients

   Hastable format
   key <String> : ID
   IP <String: IP, port
 */
public class Lookup {
    public static void main (String[] args) throws IOException {
        ServerSocket lookupSocket = null;
        boolean listening = true;

        /* Create Lookup server socket */
        try { 
            if (args.length == 1) {
                lookupSocket = new ServerSocket(Integer.parseInt(args[0]));
            } else {
                System.err.println("Error: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        /* Store loopup table into a hashmap */
        ConcurrentHashMap<Integer, ClientData> lookup = new ConcurrentHashMap<Integer, ClientData>();

        // Create file if it doesn't exist
        /*File lookupfile = new File("lookuptable");
        if(lookupfile.exists())
            lookupfile.delete();

        lookupfile.createNewFile();


        BufferedReader in = new BufferedReader(new FileReader("lookuptable"));
        String line = "";
        Long quote;
        while ((line = in.readLine()) != null) {
            String parts[] = line.split(" ");
            lookup.put(parts[0] , parts[1] + " " + parts[2]);    
        }
        in.close(); */

        /* Set table quotes in OnlineLookupHandlerThread */
        //LookupHandler.setTable(lookup);

        /* Listen for clients */
        while (listening) {
            new LookupHandler(lookupSocket.accept(), lookup).start();
        }
    }
}

