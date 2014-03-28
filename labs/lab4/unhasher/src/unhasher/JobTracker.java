package unhasher;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;


public class JobTracker extends Thread implements Watcher {
	
	private static Integer port;
	private static String addrId;
	static ServerSocket sock = null;
	
	
	// ZooKeeper resources 
	ZkConnector zkc;
	static String zkhost;
	static Integer zkport;
	static ZooKeeper zk;  //need to lock this
	static Lock zklock;
	
	static String ZK_TRACKER = "/tracker";
	static String ZK_WORKER = "/worker";
	static String ZK_FSERVER = "/fserver";
	static String ZK_TASKS = "/tasks";
	static String ZK_RESULTS = "/results";
	
	private Semaphore jobSem = new Semaphore(1);

	// JobTracker constants 
	static String TRACKER_PRIMARY = "primary";
	static String TRACKER_BACKUP = "backup";
	
	boolean debug = true;
	
	
	/**
	 * JobTracker
	 * 
	 * Watcher will keep track of switch between primary/backup in case of failure
	 */
	public JobTracker() {
		zkc = new ZkConnector();
		try {
			zkc.connect(String.format("%s:%d", zkhost, zkport));
			zk = zkc.getZooKeeper();
			debug("Connected to ZooKeeper instance zk");

			initZNodes();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	private void initZNodes() {
		//TODO: a bunch of these things are set to ephemeral, will switch to 
		//to persistent after implementing fault tolerance 
		try {
			
			zklock.lock();
			
			// create /tracker, and  set self as primary or backup
			String trackerNodePath;
			if (zk.exists(ZK_TRACKER, false) == null) {
				zk.create(ZK_TRACKER, 
						null, ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.EPHEMERAL);
				
				trackerNodePath = String.format("%s/%s", ZK_TRACKER, addrId);
				zk.create(trackerNodePath, 
						TRACKER_PRIMARY.getBytes(), 
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.EPHEMERAL);
				debug("Created /tracker znode and set self as primary");
			} else {
				//i am backup - create myself and set watch on primary
				
			}
			
			// create /worker
			if (zk.exists(ZK_WORKER, false) == null) {
				zk.create(ZK_WORKER, 
						null, 
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.EPHEMERAL);
			}
			
			// create /fserver
			if (zk.exists(ZK_FSERVER, false) == null) {
				zk.create(ZK_FSERVER, 
						null, 
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.EPHEMERAL);
			}
			
			// create /jobs
			if (zk.exists(ZK_TASKS, false) == null) {
				zk.create(ZK_TASKS, 
						null, 
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.EPHEMERAL);
			}
			
			// create /results
			if (zk.exists(ZK_RESULTS, false) == null) {
				zk.create(ZK_RESULTS, 
						null, 
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.EPHEMERAL);
			}
			
			zklock.unlock();
			
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public Runnable listenForTasks() throws KeeperException, InterruptedException {
		List<String> tasks;

		jobSem.acquire();
		while (true) {
			
			tasks = zk.getChildren(ZK_TASKS, new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					// TODO Auto-generated method stub
					if (event.getType() == Event.EventType.NodeChildrenChanged) {
				          jobSem.release();
				        }
					
				}
			});
						
			if (tasks.isEmpty()) {
				debug("No current tasks in /tasks");
			}
			
			//handle tasks 
			// for now just listing tasks
		    Collections.sort(tasks);
		    System.out.println(tasks);
		    System.out.println("--------------------");		
		    
			jobSem.acquire();
		}
	}
	
	
	/* 
	 
	 public void listForever(String groupName)
          throws KeeperException, InterruptedException {
    semaphore.acquire();
    while (true) {
      list(groupName);
      semaphore.acquire();
    }
  }

  private void list(String groupName)
          throws KeeperException, InterruptedException {
    String path = "/" + groupName;
    List<String> children = zooKeeper.getChildren(path, new Watcher() {
      @Override
      public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeChildrenChanged) {
          semaphore.release();
        }
      }
    });
    if (children.isEmpty()) {
      System.out.printf("No members in group %s\n", groupName);
      return;
    }
    Collections.sort(children);
    System.out.println(children);
    System.out.println("--------------------");
}
	 

	 */
	
	@Override
	public void process(WatchedEvent event) {
		// JobTracker watcher: watches if primary JT fails, makes self primary
	}
	
	
	/**
	 * @param args
	 * 
	 * arg0		port for JobTracker
	 * arg1 	host name for ZooKeeper
	 * arg2		port for ZooKeeper
	 * @throws InterruptedException 
	 * @throws KeeperException 
	 */
	public static void main(String[] args) throws IOException, KeeperException, InterruptedException {
		
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
		
		JobTracker jt = new JobTracker();
		new Thread(jt.listenForTasks()).start();

		// handle clients in a loop here		
		/*while (true) {
			new JobTrackerHandler(sock.accept(), zk, zklock).start();
		}*/
		
	}
	
	private void debug (String s) {
		if (debug) {
			System.out.println(String.format("TRACKER_%d: %s", port, s ));
		}
	}



}
