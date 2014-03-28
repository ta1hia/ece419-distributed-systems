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

import java.util.concurrent.CountDownLatch;

/*

  - Keep watcher on new jobs
  - When a new job appears
  - Partition work
  - Send to workers

*/

public class Worker {

    ZkConnector zkc;

    static String myPath = "/Workers";
    static String jobPath = "/j";
    int counter = 1;

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

	Worker w = new Worker(args[0]);
	
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
	    System.out.println("Zookeeper connect "+ e.getMessage());
	}

	zk = zkc.getZooKeeper();        		    
    } 	

    // Try to spawn a worker thread when a new job is created
    private boolean start() {
	while(true){
	    String path = myPath + jobPath + counter;
	    
	    // Wait until job path is created
	    listenToPath(path, counter);

	    // Congrats! Job path creathed.
	    // Spawn a worker thread for it.
	    System.out.println("Creating a new worker thread for " + path);
	    //new WorkerHandlerThread(path).start();

	    counter++;
	}
    }

    private void listenToPath(final String path, int i){
	final CountDownLatch nodeCreatedSignal = new CountDownLatch(1);

	try {
	    zk.exists(
		      path, 
		      new Watcher() {       // Anonymous Watcher
			  @Override
			      public void process(WatchedEvent event) {
			      // check for event type NodeCreated
			      boolean isNodeCreated = event.getType().equals(EventType.NodeCreated);
			      // verify if this is the defined znode
			      boolean isMyPath = event.getPath().equals(path);
			      if (isNodeCreated && isMyPath) {
				  System.out.println(myPath + " created!");
				  nodeCreatedSignal.countDown();
			      }
			  }
		      });
	} catch(KeeperException e) {
	    System.out.println(e.code());
	} catch(Exception e) {
	    System.out.println(e.getMessage());
	}
                            
	System.out.println("Waiting for " + path + " to be created ...");
        
	try{       
	    nodeCreatedSignal.await();
	} catch(Exception e) {
	    System.out.println(e.getMessage());
	}
    }
}
