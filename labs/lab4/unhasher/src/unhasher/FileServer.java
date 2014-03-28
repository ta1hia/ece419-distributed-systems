package unhasher;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

/*

  - Keep watcher on new jobs
  - When a new job appears
  - Partition work
  - Send to workers

*/

public class FileServer {

    ZkConnector zkc;
    static String myPath = "/FileServer";
    static String jobPath = "/Job";

    boolean isPrimary = false;

    Watcher watcher;

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
	
    boolean debug = true;
	
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
	fs.listenJobs();
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
    private void handleEvent(WatchedEvent event, String jobPath) {
        String path = event.getPath();
        EventType type = event.getType();
        if(path.equalsIgnoreCase(jobPath)) {
            if (type == EventType.NodeDeleted) {
                System.out.println(jobPath + " deleted! Let's go!");       
                setPrimary(); // try to become the boss
            }
            if (type == EventType.NodeCreated) {
                System.out.println(jobPath + " created!");       
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
		System.out.println("I'm the boss!");
		return true;
	    } 
        } 

	return false;
    }

    // Create a thread when a new job (ie. a child in the /Job node) has been created
    // Place a watch on /Job
    // Sequential, so keep track of the counter
    // When a new job spawns, create a new thread FileServerHandler
    private boolean listenJobs() {
        Stat stat = zkc.exists(jobPath, watcher);
        if (stat == null) {              // znode doesn't exist; let's try creating it
            System.out.println("Creating " + myPath);
            Code ret = zkc.create(
                        myPath,         // Path of znode
                        null,           // Data not needed.
                        CreateMode.EPHEMERAL   // Znode type, set to EPHEMERAL.
                        );
            if (ret == Code.OK){
		System.out.println("I'm the boss!");
		return true;
	    } 
        } 

	return false;
    }

    public void processResult(int rc, String path, Object ctx, Stat stat) {

    }


}
