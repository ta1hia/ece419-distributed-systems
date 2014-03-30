package unhasher;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/*

  - Keep watcher on new jobs
  - When a new job appears
  - Partition work
  - Send to workers

*/

public class Worker{

    ZkConnector zkc;

    static String myPath = "/Workers/w";
    static String tasksPath = "tasks";
    int counter = 1;

    boolean isPrimary = false;

    Watcher watcher;

    Semaphore workerSem = new Semaphore(1);

    List <String> tasks;

    private static Integer port;
    private static String addrId;
    static ServerSocket sock = null;
	
	
    // ZooKeeper resources 
    static Integer zkport;
    static ZooKeeper zk;  //need to lock this
    static Lock zklock;
	
    static String ZK_TRACKER = "/tracker";
    static String ZK_WORKER = "/worker";
    static String ZK_FSERVER = "/fserver";
    static String ZK_JOBS = "/jobs";
    static String ZK_RESULTS = "/results";
	
	
    // JobTracker constants 
    static String TRACKER_PRIMARY = "primary";
    static String TRACKER_BACKUP = "backup";
	
    static String mode;
    static boolean debug = true;
	
    /**
     * @param args
     * 
     * arg0		host name and port of Zookeeper
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            debug("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. Test zkServer:clientPort");
            return;
        }

	Worker w = new Worker(args[0]);

	// Make your own subfolder in the Worker folder
	// Keeps count of the amount of workers currently present
	w.registerWorker();

	// Start working!
	w.start();
    }


    // Start up ZooKeeper connection
    public Worker(String hosts){
	// Try to connect to ZkConnector
	zkc = new ZkConnector();		
	try {
	    zkc.connect(hosts);
	} catch(Exception e) {
	    debug("Zookeeper connect "+ e.getMessage());
	}

	zk = zkc.getZooKeeper();        		    
    } 	

    // Keep track of all workers currently present
    private void registerWorker(){
	// Create your folder in the path
	zkc.create(
		   myPath,         // Path of znode
		   null,           // Data not needed.
		   CreateMode.EPHEMERAL_SEQUENTIAL   
		   );

	debug("Successfuly registered as a /Worker/wX");
    }

    // Try to spawn a worker thread when a new job is created
    private boolean start() {
	while(true){
	    String path = tasksPath;
	    
	    // Wait until job path is created
	    listenToPathChildren(path);
	
	    // Wait until children of path are modified
	    try{
		workerSem.acquire();
	    } catch (Exception e){
		debug("Couldn't release semaphore");
	    }

	    // Get any new tasks
	    //tasks = getNewTasks();
	    
	    // Work on new task

	}
    }

    // Place a watch on the children of a given path
    private void listenToPathChildren(final String path){
	try {
	    tasks = zk.getChildren(
		      path, 
		      new Watcher() {       // Anonymous Watcher
			  @Override
			      public void process(WatchedEvent event) {
				      try{
					  workerSem.release();
				      } catch (Exception e){
					  debug("Couldn't release semaphore");
				      }
			      }
			  
		      });

	    debug("Worker: Created a watch on " + path + " children.");
	} catch(Exception e) {
	    e.printStackTrace();
	}
                            
        
    }

    private static void debug (String s) {
	if (debug && mode != null) {
	    System.out.println(String.format("TRACKER_%s: %s", mode.toUpperCase(), s));
	} else {
	    System.out.println(String.format("TRACKER_?: %s", s));		
	}
    }


}
