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
    static ZooKeeper zk;  //need to lock this`
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
        if (args.length != 2) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. Test zkServer:clientPort port");
            return;
        }

	FileServer fs = new FileServer(args[0]);
	fs.setPrimary();

	// You've reached this far into the code
	// You are the primary!
	// Now, get to work.
	fs.start(args[1]);
    }

    private void start(String port){
        ServerSocket serverSocket = null;
        boolean listening = true;

        try {
	    serverSocket = new ServerSocket(Integer.parseInt(port));
        
	    new FileServerHandler(true);

	    while (listening) {
	    	new FileServerHandler(serverSocket.accept(), zkc, zk).start();
	    }

	    serverSocket.close();
	} catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }
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

		// Place hostname and port into that folder
		try{
		    String data = InetAddress.getLocalHost().getHostName() + ":" + port;
		    stat = zk.setData(myPath, data.getBytes(), -1);
		} catch (Exception e){
		    debug("setPrimary: Woops. Couldn't get hostname.");
		}
		return true;
	    } 
	} 

	return false;
    } 

    private static void debug (String s) {
	if (debug && mode != null) {
	    System.out.println(String.format("TRACKER_%s: %s", mode.toUpperCase(), s));
	} else {
	    System.out.println(String.format("TRACKER_?: %s", s));		
	}
    }


}

