import java.io.*; 
import java.net.*;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/*
   Lookup / Naming service thread
 */
public class LookupHandler extends Thread {
    private Socket socket = null;
    private static ConcurrentHashMap<Integer, ClientData> table; /* thread-safe hashmap structure */

    public LookupHandler(Socket socket, ConcurrentHashMap<Integer, ClientData> ltable) {
        super("LookupHandler");
        this.socket = socket;
        this.table = ltable;
        System.out.println("Created new Thread to handle lookup requests");
    }

    public void run() {
        boolean gotByePacket = false;

        try {
            /* stream to read from client */
            ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
            MazePacket packetFromClient;

            /* stream to write back to client */
            ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());

	    table = new ConcurrentHashMap();

            while ((packetFromClient = (MazePacket) fromClient.readObject()) != null) {
                /* create a packet to send reply back to client */
                MazePacket packetToClient = new MazePacket();

                /* LOOKUP_REGISTER */
                // Client wants to find a corresponding IP and port for a given client name
                // Assume all error handling is done in client side
                if(packetFromClient.packet_type == MazePacket.LOOKUP_REGISTER) {
                    System.out.println("From Client: LOOKUP_REGISTER ");

                    packetToClient.packet_type = MazePacket.LOOKUP_REGISTER;
                    String ip = packetFromClient.client_host;
                    int port  = packetFromClient.client_port;

                    System.out.println("Lookup is registering new client.");
                    System.out.println("IP: " + ip + " Port: " + port);

                    // Find empty ID
                    int client_id = 1;
                    while(table.containsKey(client_id)){
                        client_id++;
                    }

		    ClientData cd = new ClientData();		    
		    cd.client_name = packetFromClient.client_name;
		    cd.client_host = packetFromClient.client_host;
		    cd.client_port = packetFromClient.client_port;
		    
                    table.put(client_id, cd);
                    //LookupHandler.updateTable();

                    System.out.println("To Client: registration  success ");
                    packetToClient.client_id = client_id;
                    packetToClient.error_code = 0;

		    packetToClient.lookupTable = new ConcurrentHashMap();
		    packetToClient.lookupTable = table;

                    toClient.writeObject(packetToClient);
                    continue;
                }


                /* CLIENT_QUIT */
                // Delete client from table if quitting
                if (packetFromClient.packet_type == MazePacket.CLIENT_QUIT) {
                    packetToClient.packet_type = MazePacket.LOOKUP_REPLY;

                    System.out.println("From Client: CLIENT_QUIT ");

                    int client_id = packetFromClient.client_id;

                    table.remove(client_id);

                    System.out.println("To Client: Removed from naming service.");
                    toClient.writeObject(packetToClient);
                    continue;
                }

                /* if code comes here, there is an error in the packet */
                System.err.println("ERROR: Unknown packet!!");
                System.exit(-1);

		// DEBUGGING!
		continue;
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

    // /* Accessors */
    // public static void setTable (ConcurrentHashMap <Integer, String> t) {
    //     LookupHandler.table = t;
    // }

    // private static String getHost(String client) {
    //     String query = table.get(client);
    //     String parts[] = query.split(" ");

    //     return parts[0];
    // }

    // private static int getPort(String client) {
    //     String query = table.get(client);
    //     String parts[] = query.split(" ");

    //     return Integer.valueOf(parts[1]);
    // }

    /*private static void updateTable() {
        try {
            FileWriter tableWriter = new FileWriter("lookuptable");

            BufferedWriter out = new BufferedWriter(tableWriter);
            out.write("");
            out.flush();

            int count = 0;
            Iterator<Entry<Integer, String>> it = table.entrySet().iterator();

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
    }*/

}
