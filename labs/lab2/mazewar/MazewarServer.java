import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;


/* MazewarServer class
 *
 * Initializes dispatcher
 * Daemon for ServerHandlerThreads 
 *
 */
public class MazewarServer {

    public static void main(String[] args) throws IOException {
        SocketServer mazewarServer = null;
        boolean listening = true;

        /* Create MazewarServer socket */
        try { 
            if (args.length == 1) {
                mazewarServer = new ServerSocket(Integer.parseInt(args[0]));
            } else {
                System.err.println("Error: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        /* Init game resources */
        ServerData gameData = new ServerData();

        /* Spawn single dispather thread? */

        /* Listen for new remote clients */
        while (listening) {
            //new MazewarServerHandlerThread();
        }
        mazewarServer.close();
    }

}

public static class ServerData implements Serializable {
    //eventqueue
    //clientqueue
    BlockingQueue<MazeEvent> eventQueue = new LinkedBlockingQueue();
    ConcurrentMap<String, String> clientTable = new ConcurrentHashMap<>(); //Might need reference to actual thread here, for dispatcher

    public void addClientToTable(String name, Point position) {
        if (!clientTable.containsKey(name)) {
            clientTable.put(name, position);
        } else {
            System.out.println("Client with name " + name + " already exists.");
        }

    }
}
