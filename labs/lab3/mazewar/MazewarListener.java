import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import java.io.*;
import java.net.*;

/* MazewarListener class
 *
 * Recieve all client event packets
 *
 */
public class MazewarListener extends Thread{

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

    Dispatcher dispatcher;
    ListenerData data;
    ClientHandlerThread chandler;
    int client_port;

    public MazewarListener(int client_port, ListenerData data, Dispatcher dispatcher, ClientHandlerThread chandler){
	this.client_port = client_port;
	this.dispatcher = dispatcher;
	this.data = data;
	this.chandler = chandler;
	boolean addedRobots = false;
            
    }

    public void run(){
        ServerSocket mazewarListener = null;
        boolean listening = true;

	try{

            mazewarListener = new ServerSocket(client_port);

	    /* Listen for new remote clients */
	    while (listening) {
		new MazewarListenerHandlerThread(mazewarListener.accept(), data, dispatcher, chandler).start();	    
	    }

	    mazewarListener.close();
  
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }
    }
}

