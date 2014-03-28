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
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.data.Stat;


public class JobTracker extends Thread implements Watcher {

	private static final String ZK_PRIMARY = null;
	private static Integer port;
	private static String addrId;		// used for path to self: /tracker/[addrId]
	static ServerSocket sock = null;


	// ZooKeeper resources 
	static ZkConnector zkc;
	static ZooKeeper zk; 

	static String zkhost;
	static Integer zkport;

	static String ZK_TRACKER = "/tracker";
	static String ZK_WORKER = "/worker";
	static String ZK_FSERVER = "/fserver";
	static String ZK_TASKS = "/tasks";  // like an event queue, task can be submit or query
	static String ZK_JOBS = "/jobs";	// for submit tasks (jobs) only, used by worker
	static String ZK_RESULTS = "/results";

	private Semaphore jobSem = new Semaphore(1);

	// JobTracker constants 
	static String myPath;
	static String TRACKER_PRIMARY = "primary";
	static String TRACKER_BACKUP = "backup";
	static String mode;
    static CountDownLatch modeSignal = new CountDownLatch(1);


	static boolean debug = true;


	/**
	 * JobTracker
	 * 
	 * Watcher will keep track of switch between primary/backup in case of failure
	 */
	public JobTracker() {
		zkc = new ZkConnector();
		try {
			debug("Connecting to ZooKeeper instance zk");
			String hosts = String.format("%s:%d", zkhost, zkport);
			//debug(hosts);
			zkc.connect(hosts);
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
		Stat status;
		try {
			// create /tracker, and  set self as primary or backup
			String trackerPath;
			status = zk.exists(ZK_TRACKER, false);
			if (status == null) {
				zk.create(ZK_TRACKER, 
						null, ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.PERSISTENT);

				mode = TRACKER_PRIMARY;
				myPath = ZK_TRACKER + "/" + TRACKER_PRIMARY;
				zk.create(myPath, 
						null, 
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.EPHEMERAL);
				debug("Created /tracker znode and set self as primary");
			} else {
				// znode /tracker exists, check if it has a child
				// if it does, it must be the primary -> make self backup.
				// else make myself primary
				if (status.getNumChildren() == 0) {
					mode = TRACKER_PRIMARY;
					myPath = ZK_TRACKER + "/" + TRACKER_PRIMARY;
					zk.create(myPath, 
							null, 
							ZooDefs.Ids.OPEN_ACL_UNSAFE, 
							CreateMode.EPHEMERAL);
					debug("in /tracker znode set self as primary");
				} else {
					mode = TRACKER_BACKUP;
					myPath = ZK_TRACKER + "/" + TRACKER_BACKUP;
					zk.create(myPath, 
							null, 
							ZooDefs.Ids.OPEN_ACL_UNSAFE, 
							CreateMode.EPHEMERAL);
					debug("in /tracker znode set self as backup");	
				}
			}

			// create /worker
			if (zk.exists(ZK_WORKER, false) == null) {
				zk.create(ZK_WORKER, 
						null, 
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.EPHEMERAL);
				debug("Created /worker znode");
			}

			// create /fserver
			if (zk.exists(ZK_FSERVER, false) == null) {
				zk.create(ZK_FSERVER, 
						null, 
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.EPHEMERAL);
				debug("Created /fserver znode");
			}


			// create /tasks
			if (zk.exists(ZK_TASKS, false) == null) {
				zk.create(ZK_TASKS, 
						null, 
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.PERSISTENT);
				debug("Created /tasks znode");
			}

			// create /jobs
			if (zk.exists(ZK_JOBS, false) == null) {
				zk.create(ZK_JOBS, 
						null, 
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.PERSISTENT);
				debug("Created /jobs znode");
			}

			// create /results
			if (zk.exists(ZK_RESULTS, false) == null) {
				zk.create(ZK_RESULTS, 
						null, 
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.PERSISTENT);
				debug("Created /results znode");
			}

		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	public Runnable listenForTasks() throws KeeperException, InterruptedException, UnsupportedEncodingException {
		List<String> tasks;
		Stat status;
		String dataStr;
		byte[] data;

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
			// for now listing tasks
			Collections.sort(tasks);
			debug(tasks.toString());

			for (String path : tasks) {
				status = new Stat();
				data = zk.getData("/tasks/" + path, false, status);
				if (status != null) {
					dataStr = byteToString(data);
					//debug(String.format("task in path %s/%s is %s", ZK_TASKS, path, dataStr));
					handleTask(new TaskPacket(dataStr), path);
				}
			}

			debug("--------------------");		

			jobSem.acquire();
		}
	}

	private void handleTask(TaskPacket p, String path) {

		// check if task is job or query
		
		if (p.packet_type == TaskPacket.TASK_SUBMIT) handleJob(p, path);
		else if (p.packet_type == TaskPacket.TASK_QUERY) handleQuery(p, path);
		else debug("packet type could not be recognized");

	}
	
	private void handleQuery(TaskPacket p, String path) {
		debug(String.format("handling query on '%s'", p.hash));
		String resultPath = ZK_RESULTS + "/" + p.hash;
		String clientResponsePath = ZK_TASKS + "/" + path + "/res";
		String response = null;
		try {
			// for query, check /result/[hash]
			Stat stat = zk.exists(resultPath, false);
			
			if (stat == null) {
				// if not in result, return "still in progress"
				response = "job in progress";
			} else {
				// if in result, return result
				byte[] data = zk.getData(resultPath, false, null);
				response = "password found: " + byteToString(data);
			}
			//debug(String.format("adding result '%s' to path %s", response, clientResponsePath));
			String res = zk.create(clientResponsePath, 
					response.getBytes(), 
					ZooDefs.Ids.OPEN_ACL_UNSAFE, 
					CreateMode.EPHEMERAL);
			//debug("created in " + res);
						
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private void handleJob(TaskPacket p, String tpath) {
		// check if task already exists in /job/[hash]
		// if not, create /job/[hash]
		debug(String.format("handling job '%s'", p.hash));
		String jobPath = ZK_JOBS + "/" + p.hash;

		try {
			// check if task already exists in /job/[hash]
			Stat stat = zk.exists(jobPath, false);
			
			if (stat == null) {
				// if not in result, return "still in progress"
				String res = zk.create(jobPath, 
						p.hash.getBytes(),  //redundant to put hash here again
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.PERSISTENT);
			} else {
				// if in result, return result
				debug("job for '"+ p.hash + "' already exists");
			}
			
			zk.delete(ZK_TASKS + "/" + tpath, 0);		//delete job from /tasks/t#			
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
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


	@Override
	public void process(WatchedEvent event) {
		// JobTracker watcher: watches if primary jt fails, makes self primary
		boolean isPrimaryDeleted;
		try {
			isPrimaryDeleted = event.getType().equals(EventType.NodeDeleted);
			Stat status = zk.exists(ZK_TRACKER, false);
			if (mode.equals(TRACKER_BACKUP) 
					&& isPrimaryDeleted 
					&& status != null 
					&& status.getNumChildren() == 1) {
				debug("detected primary failure, setting self as new primary");
				zk.delete(myPath, 0); 							// remove self as backup
				myPath = ZK_TRACKER + "/" + TRACKER_PRIMARY;	// add self as primary
				mode = TRACKER_PRIMARY;
				zk.create(myPath, 
						null, 
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.EPHEMERAL);
								
				modeSignal.countDown();
			}
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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

			//String addrId = String.format("%s:%d", InetAddress.getLocalHost().getHostAddress(), port);
			//myPath = String.format("%s/%s", ZK_TRACKER, addrId);
		} else {
			System.err.println("ERROR: Invalid arguments!");
			System.exit(-1);
		}

		JobTracker jt = new JobTracker();
		if (mode == TRACKER_BACKUP) {
			debug("backup setting watch on " + ZK_TRACKER + "/" + TRACKER_PRIMARY);
			zk.exists(ZK_TRACKER + "/" + TRACKER_PRIMARY, jt);	// primary watch
		    modeSignal.await();
		}
		new Thread(jt.listenForTasks()).start();

		// handle clients in a loop here		
		/*while (true) {
			new JobTrackerHandler(sock.accept(), zk, zklock).start();
		}*/

	}

	private static void debug (String s) {
		if (debug && mode != null) {
			System.out.println(String.format("TRACKER_%s: %s", mode.toUpperCase(), s));
		} else {
			System.out.println(String.format("TRACKER_?: %s", s));		
		}
	}



}
