import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import java.io.*;
import java.net.*;

/* MazewarServer class
 *
 * Initializes dispatcher
 * Daemon for ServerHandlerThreads 
 *
 */
public class MazewarServer {

    public static void main(String[] args) throws IOException {
        ServerSocket mazewarServer = null;
        boolean listening = true;

        /* Create MazewarServer socket */
        try { 
            if (args.length == 1) {
                mazewarServer = new ServerSocket(Integer.parseInt(args[0]));
                System.out.println("Mazewar server started");
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
        Dispatcher dispatcher = new Dispatcher(gameData);
        dispatcher.start();

        /* Listen for new remote clients */
        while (listening) {
            new MazewarServerHandlerThread(mazewarServer.accept(), gameData).start();
        }
        mazewarServer.close();
    }


}

