package unhasher;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
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

	// ZooKeeper resources 
	static ZkConnector zkc;
	static ZooKeeper zk; 

	static String zkhost;
	static Integer zkport;

	static String ZK_TRACKER = "/tracker";
	static String ZK_WORKER = "/workers";
	static String ZK_FSERVER = "/fserver";
	static String ZK_TASKS = "/tasks";  // like an event queue, task can be submit or query
	static String ZK_JOBS = "/jobs";	// for submit tasks (jobs) only, used by worker
	static String ZK_CLIENTS = "/clients";
	static String ZK_RESULTS = "/results";

	private Semaphore jobSem = new Semaphore(1);

	// JobTracker constants 
	static String myPath;
	static String TRACKER_PRIMARY = "primary";
	static String TRACKER_BACKUP = "backup";
	static String mode;
	static CountDownLatch modeSignal = new CountDownLatch(1);

	// Client tracking
	static ArrayList<String> clientList = new ArrayList<String>();
	static HashMap <String, ArrayList<String>> clientJobs = 
			new HashMap<String, ArrayList<String>>(); // id: [hash1, hash2, hash3...]
	private Semaphore clientSem = new Semaphore(1);

	static boolean debug = true;
	static Lock debugLock = new ReentrantLock();


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

			// create /client
			if (zk.exists(ZK_CLIENTS, false) == null) {
				zk.create(ZK_CLIENTS, 
						null, 
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.PERSISTENT);
				debug("Created /client znode");
			}

			// create /worker
			if (zk.exists(ZK_WORKER, false) == null) {
				zk.create(ZK_WORKER, 
						null, 
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.PERSISTENT);
				debug("Created /worker znode");
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

	public class RunListenForClients implements Runnable, Watcher {
		@Override
		public void run() {
			try {
				listenForClients();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (KeeperException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

		public void listenForClients() throws KeeperException, InterruptedException, UnsupportedEncodingException {
			List<String> clients;
			Stat status;
			String dataStr;
			byte[] data;

			clientSem.acquire();
			while (true) {

				clients = zk.getChildren(ZK_CLIENTS, new Watcher() {
					@Override
					public void process(WatchedEvent event) {
						// TODO Auto-generated method stub
						if (event.getType() == Event.EventType.NodeChildrenChanged) {
							clientSem.release();
						}					
					}
				});

				if (clients.isEmpty()) {
					debug("No clients in /clients");
				}

				//handle clients 
				Collections.sort(clients);
				debug("clients: " + clients.toString());

				for (String id : clients) {
					if (!clientList.contains(id)) {
						clientList.add(id);
						String clientPath = ZK_CLIENTS + "/" + id;
						zk.exists(clientPath, this);	// watch for client disconnect
					}
				}

				debug("--------------------");		

				clientSem.acquire();
			}
		}
		
		@Override
		public void process(WatchedEvent event) {
			// TODO Auto-generated method stut
			try {
				boolean isNodeDeleted = event.getType().equals(EventType.NodeDeleted);
				String nodeName = event.getPath().split("/")[2];

				if (isNodeDeleted && !nodeName.equals(TRACKER_PRIMARY)) {
					debug("client " + nodeName + " disconnected, clearing its usecounts");
					ArrayList<String> jobs = clientJobs.get(nodeName);
					if (jobs != null) {
						for (String job : jobs) {
							String jobPath = ZK_JOBS + "/" + job;
							debug ("clearing " + nodeName + "'s job at " + jobPath);
							UseCount ucount = new UseCount(jobPath);
							debug("usecount " + ucount.count);
							ucount.decrementUseCount();
							debug("usecount " + ucount.count);
							// if use count is now 0, delete the job
							if (ucount.count == 0) {
								debug("usecount=0 for job at " + jobPath + ", deleting job");
								zk.delete(jobPath, ucount.version);
							}
						}
					} else {
						debug("client " + nodeName + " had no jobs to clear");
					}
					clientJobs.remove(nodeName);
					clientList.remove(nodeName);
					debug("clientJobs: " + clientJobs.toString());
					debug("clientList: " + clientList.toString());
				} 
			} catch (KeeperException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
	
	public class RunListenForTasks implements Runnable {

		@Override
		public void run() {
			try {
				listenForTasks();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeeperException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void listenForTasks() throws KeeperException, InterruptedException, UnsupportedEncodingException {
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
				debug("tasks :" + tasks.toString());

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
	}


	private void handleTask(TaskPacket p, String path) {

		// check if task is job or query
		debug("client " + p.c_id + " sent a request");
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
			
			if (stat != null) {
				byte[] data = zk.getData(resultPath, false, null);
				
				String[] tokens = byteToString(data).split(":");
				
				if (tokens[0].equals("0")) {
					response = "error: job '" + p.hash + "' could not be processed";
				} else if (tokens[0].equals("success")) {
					response = "password found: " + tokens[1];
				} else if (tokens[0].equals("fail")) {
					response = "error: job could not be processed";
				} else if (Integer.parseInt(tokens[0]) > 0) {
					response = "job in progress";
				}
			} else {
				response = "job '" + p.hash + "' does not exist";
			}
			debug(String.format("adding result '%s' to path %s", response, clientResponsePath));

			String res = zk.create(clientResponsePath, 
					response.getBytes(), 
					ZooDefs.Ids.OPEN_ACL_UNSAFE, 
					CreateMode.EPHEMERAL);

		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private void handleJob(TaskPacket p, String tpath) {
		// check if task already exists in /job/[hash]
		// if not, create /job/[hash]
		debug(String.format("handling job '%s'", p.hash));
		String jobPath = ZK_JOBS + "/" + p.hash;
		String resultPath = ZK_RESULTS + "/" + p.hash;

		try {
			// check if task already exists in /job/[hash]
			Stat statJob = zk.exists(jobPath, false);
			Stat statResult = zk.exists(resultPath, false);

			// create job if it doesn't already exist
			if (statJob == null) {
				addJobToMap(p);
				String res = zk.create(jobPath, 
						String.valueOf(1).getBytes(),  //1 node is using this job
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.PERSISTENT);
			} else {
				// if in result, return result
				if (!clientJobs.get(p.c_id).contains(p.hash)) {
					addJobToMap(p);
					UseCount ucount = new UseCount(jobPath);
					debug(String.format("job for %s exists with usecount %d, incrementing count", p.hash, ucount.count));
					boolean success = ucount.incrementUseCount(jobPath);
				} else {
					debug("client " + p.c_id + " already submitted " + p.hash);
				}
			}
			
			if (statResult == null) {
				String res = zk.create(resultPath, 
						String.valueOf(0).getBytes(),  //init to 0
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.PERSISTENT);
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

	private void addJobToMap(TaskPacket p) {
		if (clientList.contains(p.c_id)) {
			if ( clientJobs.get(p.c_id) == null) {
				clientJobs.put(p.c_id, new ArrayList<String>()); //no ArrayList assigned, create new ArrayList
			}
			clientJobs.get(p.c_id).add(p.hash); 
		} else {
			debug("cant find client " + p.c_id + " in list");
		}
	}

	@Override
	public void process(WatchedEvent event) {
		// JobTracker watcher: watches if primary jt fails, makes self primary
		boolean isNodeDeleted;
		try {
			isNodeDeleted = event.getType().equals(EventType.NodeDeleted);
			String nodeName = event.getPath().split("/")[1];
			Stat trackerStat = zk.exists(ZK_TRACKER, false);

			if (isNodeDeleted 
					&& !nodeName.equals(TRACKER_PRIMARY)) {
				debug("client " + nodeName + "disconnected, clearing its usecounts");
				ArrayList<String> jobs = clientJobs.get(nodeName);
				if (jobs != null) {
					for (String job : jobs) {
						String jobPath = ZK_JOBS + "/" + job;
						String resultPath = ZK_RESULTS + "/" + job;
						debug ("clearing " + nodeName + "'s job at " + jobPath);
						UseCount ucount = new UseCount(jobPath);
						ucount.decrementUseCount();
						// if use count is now 0, delete the job
						if (ucount.count == 0) {
							zk.delete(jobPath, 0);
						}
					}
				} else {
					debug("client " + nodeName + " had no jobs to clear");
				}
				clientJobs.remove(nodeName);
				clientList.remove(nodeName);
			}
			if (mode.equals(TRACKER_BACKUP) // primary failure handling
					&& isNodeDeleted 
					&& nodeName.equals(TRACKER_PRIMARY)
					&& trackerStat != null 
					&& trackerStat.getNumChildren() == 1) {
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

	public class UseCount {

		public Integer count;
		public String jobpath;
		public Integer version = null; 

		public UseCount(String jobPath) {
			try {
				this.jobpath = jobPath;
				byte[] data = zk.getData(jobPath, false, null);
				String countStr = byteToString(data);
				this.count = Integer.parseInt(countStr);
				Stat state = zk.exists(jobpath, false);
				this.version = state.getVersion();
			} catch (KeeperException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}


		private boolean setUseCount(Integer newCount) {			
			try {
				this.count = newCount;
				String countStr = String.valueOf(count);
				Stat stat = zk.setData(jobpath, countStr.getBytes(), version);
				version = stat.getVersion();
				return true;
			} catch (KeeperException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return false;
		}

		public boolean incrementUseCount(String jobPath) throws KeeperException, InterruptedException {
			boolean success = setUseCount(count + 1);	
			return success;
		}

		public boolean decrementUseCount() {
			boolean success = setUseCount(count - 1);	
			return success;
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

		if(args.length == 2) {
			zkhost = args[0];
			zkport = Integer.parseInt(args[1]);

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
		debug("starting client listerner");
		new Thread(jt.new RunListenForClients()).start();
		debug("starting task listerner");
		new Thread(jt.new RunListenForTasks()).start();

		// handle clients in a loop here		
		/*while (true) {
			new JobTrackerHandler(sock.accept(), zk, zklock).start();
		}*/

	}

	private static void debug (String s) {
		debugLock.lock();
		if (debug && mode != null) {
			System.out.println(String.format("TRACKER_%s: %s", mode.toUpperCase(), s));
		} else {
			System.out.println(String.format("TRACKER_?: %s", s));		
		}
		debugLock.unlock();
	}



}
