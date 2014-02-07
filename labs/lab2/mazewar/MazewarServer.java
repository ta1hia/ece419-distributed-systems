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
                brokerSocket = new ServerSocket(Integer.parseInt(args[0]));
            } else {
                System.err.println("Error: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        /* Spawn single dispather thread? */

        /* Listen for new remote clients */
        while (listening) {
            //new MazewarServerHandlerThread();
        }
        mazewarServer.close();
    }

    public static class ServerData implements Serializable {
        //eventqueue
        //clientqueue
        BlockingQueue<MazePacket> eventQueue = new LinkedBlockingQueue();
        ConcurrentMap<String, String> clientTable = new ConcurrentHashMap<>(); //Might need reference to actual thread here, for dispatcher
    }
}
