package unhasher;

import java.io.*;
import java.net.InetAddress;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;


public class JobTracker extends Thread implements Watcher{
	
	private static Integer port;
	private static String addrId;
	
	/* Zookeeper vars */
	private static String zkhost;
	private static Integer zkport;
	private static ZooKeeper zk;
	
	static String ZK_TRACKER = "/tracker";
	static String ZK_WORKER = "/worker";
	static String ZK_FSERVER = "/fserver";
	static String ZK_JOBS = "/jobs";
	static String ZK_RESULTS = "/results";
	
	/* JobTracker constants */
	static String TRACKER_PRIMARY = "primary";
	static String TRACKER_BACKUP = "backup";
	

	/**
	 * @param args
	 * 
	 * arg0		port for JobTracker
	 * arg1 	hostname for Zookeeper
	 * arg2		port for Zookeeper
	 */
	public static void main(String[] args) throws IOException {
		
		if(args.length == 4) {
			port = Integer.parseInt(args[0]);
			zkhost = args[1];
			zkport = Integer.parseInt(args[2]);
			
			addrId = String.format("%s:%d", InetAddress.getLocalHost().getHostAddress(), port);
		} else {
			System.err.println("ERROR: Invalid arguments!");
			System.exit(-1);
		}
		
		// handle client packets in a loop here
		

	}
	
	/**
	 * JobTracker
	 * 
	 * Watcher will keep track of switch between primary/backup in case of failure
	 */
	public JobTracker() {
		
		try {
			zk = new ZooKeeper(String.format("%s:%d", zkhost, zkport), 5000, null); //TODO: add watcher
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	private void initZNodes() {
		
		try {
			
			// create /tracker, and  set self as primary or backup
			String trackerNodePath;
			if (zk.exists(ZK_TRACKER, false) == null) {
				zk.create(ZK_TRACKER, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				
				trackerNodePath = String.format("%s/%s", ZK_TRACKER, addrId);
				zk.create(trackerNodePath, TRACKER_PRIMARY.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
			} else {
				//i am backup
				
			}
			
			// create /worker
			if (zk.exists(ZK_WORKER, false) == null) {
				zk.create(ZK_WORKER, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
			
			// create /fserver
			if (zk.exists(ZK_FSERVER, false) == null) {
				zk.create(ZK_FSERVER, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
			
			// create /jobs
			if (zk.exists(ZK_JOBS, false) == null) {
				zk.create(ZK_JOBS, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
			
			// create /results
			if (zk.exists(ZK_RESULTS, false) == null) {
				zk.create(ZK_RESULTS, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
			
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

	@Override
	public void process(WatchedEvent event) {
		// TODO Auto-generated method stub
		
	}

}
