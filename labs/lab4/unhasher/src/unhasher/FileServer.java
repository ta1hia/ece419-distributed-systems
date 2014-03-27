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

/*

- Keep watcher on new jobs
- When a new job appears
	- Partition work
	- Send to workers

 */

public class FileServer {

	private static Integer port;
	private static String addrId;
	static ServerSocket sock = null;
	
	
	// ZooKeeper resources 
	static String zkhost;
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
	 * arg0		host name for ZooKeeper
	 * arg1		port for ZooKeeper
	 */
	public static void main(String[] args) throws IOException {
		if(args.length == 3) {
			port = Integer.parseInt(args[0]);
			zkhost = args[1];
			zkport = Integer.parseInt(args[2]);
			
			zklock = new ReentrantLock();
			sock = new ServerSocket(port);

			addrId = String.format("%s:%d", InetAddress.getLocalHost().getHostAddress(), port);
		} else {
			System.err.println("ERROR: Invalid arguments!");
			System.exit(-1);
		}
		
		// handle clients in a loop here		
		while (true) {
			new FileServerHandler(sock.accept(), zk, zklock).start();
		}
		
	}




}
