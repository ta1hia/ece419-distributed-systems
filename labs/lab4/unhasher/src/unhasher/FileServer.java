package unhasher;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.ArrayList;

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

  - Keep watcher on new requests
  - When a new request appears
  - Partition work
  - Send to workers

*/

public class FileServer {

    ZkConnector zkc;
    static String myPath = "/FileServer";
    static String requestsPath = "/request";

    boolean isPrimary = false;

    Watcher watcher;
    Semaphore requestSem = new Semaphore(1);


    List <String> requests;
    List <String> oldRequests = new ArrayList();

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
    static String ZK_REQUESTS = "/requests";
    static String ZK_RESULTS = "/results";
	
	
    // RequestTracker constants 
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
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. Test zkServer:clientPort");
            return;
        }

	FileServer fs = new FileServer(args[0]);
	fs.setPrimary();

	// You've reached this far into the code
	// You are the primary!
	// Now, get to work.
	fs.start();
    }

    // Start up ZooKeeper connection
    public FileServer(String hosts){
	// Try to connect to ZkConnector
	zkc = new ZkConnector();		
	try {
	    zkc.connect(hosts);
	} catch(Exception e) {
	    System.out.println("Zookeeper connect "+ e.getMessage());
	}

	zk = zkc.getZooKeeper();        		    
    }
 	
    // Watcher handler
    // Wake up when a node changes in ZooKeeper
    private void handleEvent(WatchedEvent event, String requestsPath) {
        String path = event.getPath();
        EventType type = event.getType();
        if(path.equalsIgnoreCase(requestsPath)) {
            if (type == EventType.NodeDeleted) {
                System.out.println(requestsPath + " deleted! Let's go!");       
                setPrimary(); // try to become the boss
            }
            if (type == EventType.NodeCreated) {
                System.out.println(requestsPath + " created!");       
                try{ Thread.sleep(5000); } catch (Exception e) {}
                setPrimary(); // re-enable the watch
            }
        }
    }

    private boolean setPrimary() {
        Stat stat = zkc.exists(myPath, watcher);
        if (stat == null) {              // znode doesn't exist; let's try creating it
            System.out.println("Creating " + myPath);
            Code ret = zkc.create(
                        myPath,         // Path of znode
                        null,           // Data not needed.
                        CreateMode.EPHEMERAL   // Znode type, set to EPHEMERAL.
                        );
            if (ret == Code.OK){
		System.out.println("I'm the primary backup.");
		return true;
	    } 
        } 

	return false;
    }

    // Give out partitions of the dictionary depending on the request.
    private void start(){
	while(true){
	    listenToPathChildren(requestsPath);
	    
	    // Wait until children of path are modified
	    try{
		requestSem.acquire();
	    } catch (Exception e){
		debug("Couldn't release semaphore");
	    }

	    // Get any new request
	    List <String> newRequests;
	    newRequests = getNewRequests();

	    // Work on new request
	    handle(newRequests);
	}
    }

    // Place a watch on the children of a given path
    private void listenToPathChildren(final String path){
	try {
	    requests = zk.getChildren(
		      path, 
		      new Watcher() {       // Anonymous Watcher
			  @Override
			      public void process(WatchedEvent event) {
				      try{
					  requestSem.release();
				      } catch (Exception e){
					  debug("Couldn't release semaphore");
				      }
			      }
			  
		      });

	    debug("listenToPathChildren: Created a watch on " + path + " children.");
	} catch(Exception e) {
	    e.printStackTrace();
	}                          
    }


    // Return a list of string continue new requests
    private List<String> getNewRequests(){
	List<String> newRequests = new ArrayList();
	List<String> removedRequests = new ArrayList();
	Stat status;
	String dataStr;
	byte[] data;

	debug("getNewRequests: Traversing through requests.");

	for (String path : requests) {
	    try{
		debug("getNewRequests: " + path);

		status = new Stat();
		data = zk.getData(requestsPath + "/" + path, false, status);

		if (status != null) {
		    dataStr = byteToString(data);

		    // Add requests that are new
		    if(!oldRequests.contains(dataStr)){
			newRequests.add(dataStr);
		    }
		}
	    } catch (Exception e){
		debug("getNewRequests: A path has been deleted! " + path);

		// This request has been deleted!
		oldRequests.remove(path);
	    }
	}

	for (String path : newRequests){
	    requests.remove(path);
	}

	return newRequests;
    }


    // Handle all new requests
    private void handle(List <String> newRequests){
	for(String path : newRequests){
	    debug("handle: Sending job " + path);

	    // Spawn a thread
	    try{
		new WorkerHandler(zkc,requestsPath + "/" + path).start();
	    } catch (Exception e){
		debug("handle: Couldn't spawn WorkerHandler");
	    }

	    // Add to oldRequests list
	    oldRequests.add(path);
	}
    }


    public String byteToString(byte[] b) {
	String s = null;
	if (b != null) {
	    try {
		s = new String(b, "UTF-8");
	    } catch (UnsupportedEncodingException e) {
		e.printStackTrace();
	    }
	}
	return s;
    }

    private static void debug (String s) {
	if (debug && mode != null) {
	    System.out.println(String.format("TRACKER_%s: %s", mode.toUpperCase(), s));
	} else {
	    System.out.println(String.format("TRACKER_?: %s", s));		
	}
    }


}



    // // Create a thread when a new request (ie. a child in the /Request node) has been created
    // // Place a watch on /Request
    // // Sequential, so keep track of the counter
    // // When a new request spawns, create a new thread FileServerHandler
    // private boolean listenRequests() {
    //     Stat stat = zkc.exists(requestsPath, watcher);
    //     if (stat == null) {              // znode doesn't exist; let's try creating it
    //         System.out.println("Creating " + myPath);
    //         Code ret = zkc.create(
    //                     myPath,         // Path of znode
    //                     null,           // Data not needed.
    //                     CreateMode.EPHEMERAL   // Znode type, set to EPHEMERAL.
    //                     );
    //         if (ret == Code.OK){
    // 		System.out.println("I'm the boss!");
    // 		return true;
    // 	    } 
    //     } 

    // 	return false;
    // }
