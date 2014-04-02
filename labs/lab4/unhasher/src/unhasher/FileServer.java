package unhasher;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
	static String myPath = "/fserver";
	static String requestsPath = "/requests";

	boolean isPrimary = false;

	Watcher watcher;
	Semaphore requestSem = new Semaphore(1);

	File dictionaryFile;
	String dictionaryPath = "unhasher/src/unhasher/dictionary/lowercase.rand";
	InputStream is;
	BufferedReader br;

	List <String> requests;
	List <String> oldRequests = new ArrayList();
	List <String> dictionary;

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

		FileServer fs = new FileServer(args[0],args[1]);
		fs.setPrimary();

		// You've reached this far into the code
		// You are the primary!
		// Now, get to work.
		fs.start();
	}

	private void start(){

		ServerSocket serverSocket = null;
		boolean listening = true;

		try {
			serverSocket = new ServerSocket(port);

			// Debug
			//new FileServerHandler(true);

			debug("start: Listening for incoming connections...");
			while (listening) {
				new FileServerHandler(serverSocket.accept(), zkc, zk, dictionary).start();
			}

			serverSocket.close();
		} catch (IOException e) {
			System.err.println("ERROR: Could not listen on port!");
			System.exit(-1);
		}
	}


	private void getDictionary(){
		debug("getDictionary: Retrieving dictionary");

		dictionary = new ArrayList<String>();

		try{
			//is = new FileInputStream(dictionaryPath);
			br = new BufferedReader(new FileReader(dictionaryPath));

			String line = null;
			int i = 0;
			// Traverse through dictionary and save it into the list
			while((line = br.readLine()) != null){
			    //debug(line);
			    dictionary.add(i,line);
			    i++;
			}

			debug((i-1) + " == " + dictionary.get(i-1));

			debug("getDictionary: Finished retrieving dictionary");
		} catch(Exception e){
			debug("getDictionary:Boo-hoo. Couldn't import dictionary");
			e.printStackTrace();
		}

	}


	// Start up ZooKeeper connection
	public FileServer(String hosts, String port){
		debug("Connecting to Zookeeper");

		this.port = Integer.parseInt(port);

		// Try to connect to ZkConnector
		zkc = new ZkConnector();		
		try {
			zkc.connect(hosts);
		} catch(Exception e) {
			System.out.println("FileServer: Zookeeper connect "+ e.getMessage());
		}

		zk = zkc.getZooKeeper();        		    
	}

	private boolean setPrimary() {
		// Store whole dictionary as a list
		getDictionary();

		Stat stat = zkc.exists(myPath, watcher);
		if (stat == null) {              // znode doesn't exist; let's try creating it
			System.out.println("Creating " + myPath);
			Code ret = zkc.create(
					myPath,         // Path of znode
					null,           // Data not needed.
					CreateMode.EPHEMERAL   // Znode type, set to EPHEMERAL.
					);
			if (ret == Code.OK){
				System.out.println("setPrimary: I'm the primary file server.");

				// Place hostname and port into that folder
				try{
					String data = InetAddress.getLocalHost().getHostName() + ":" + port;
					debug("setPrimary: " + data);

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
			System.out.println(String.format("FS_%s: %s", mode.toUpperCase(), s));
		} else {
			System.out.println(String.format("FS_?: %s", s));		
		}
	}


}

