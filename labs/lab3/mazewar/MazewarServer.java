import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import java.io.*;
import java.net.*;

/* MazewarServer class
 *
 * Recieve all client event packets
 *
 */
public class MazewarServer extends Thread{

    /**
     * The default width of the {@link Maze}.
     */
    private final int mazeWidth = 20;

    /**
     * The default height of the {@link Maze}.
     */
    private final int mazeHeight = 10;

    /**
     * The default random seed for the {@link Maze}.
     * All implementations of the same protocol must use 
     * the same seed value, or your mazes will be different.
     */
    private final int pointSeed = (int) System.currentTimeMillis();
    private final int mazeSeed = 42;

    public MazewarServer(int client_port, ServerData data, Dispatcher dispatcher, ClientHandlerThread chandler){
        ServerSocket mazewarServer = null;
        boolean listening = true;

        /* Create MazewarServer socket */
        try { 
            mazewarServer = new ServerSocket(client_port);

	    /* Init game resources */
	    //ServerData data = new ServerData();

	    /* Spawn single dispather thread? */
	    //Dispatcher dispatcher = new Dispatcher(data);
	    //dispatcher.start();

	    boolean addedRobots = false;

	    /* Listen for new remote clients */
	    while (listening) {
		new MazewarServerHandlerThread(mazewarServer.accept(), data, dispatcher, chandler).start();	    
	    }

	    mazewarServer.close();
            
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }
    }


}

