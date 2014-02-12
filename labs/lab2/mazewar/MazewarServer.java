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
    private final int pointSeed = (int) System.currentTimeMillis();//42;
    private final int mazeSeed = 42;

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

	// // Spawn two robots as dummy clients.
	// private Maze maze = null;
	
        // // Create the maze
        // maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed, pointSeed);

        // ClientHandlerThread clientHandler = new ClientHandlerThread(host, port);
	// String robotName1 = "Norby";
	// String robotName2 = "Robbie";

	// RobotClient robot1 = new RobotClient(robotName1);
	// RobotClient robot2 = new RobotClient(robotName2);

	// maze.addClient(robot1);
	// maze.addClient(robot2);

	boolean addedRobots = false;

        /* Listen for new remote clients */
        while (listening) {
            new MazewarServerHandlerThread(mazewarServer.accept(), gameData).start();

	    // if(!addedRobots){
	    // 	clientHandler.registerRobotWithMazewar(robot1);
	    // 	clientHandler.registerRobotWithMazewar(robot2);

	    // 	addedRobots = true;
	    // }
	    
        }
        mazewarServer.close();
    }


}

