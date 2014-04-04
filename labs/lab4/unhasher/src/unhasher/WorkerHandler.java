package unhasher;

import java.io.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.String;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

// Hashing library
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;

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

// Each WorkerHandler is assigned a job
// Try and find if the password hash is equal to the word hash in worker's dictionary partition
public class WorkerHandler extends Thread{

    static String myPath = "/workers";
    static String resultsPath = "/results";

    Socket cSocket = null;
    ObjectInputStream cin;
    ObjectOutputStream cout;

    TaskPacket packetFromCD;
    ZooKeeper zk;
    Lock zklock;

    ZkConnector zkc;
    String path;

    List <String> dictionary;
    Lock dlock = new ReentrantLock();

    List <String> workers;

    static String mode;
    static boolean debug = true;

    static Integer w_id = null;
    String w_id_string;
    int numWorkers;
    int partition_id;

    int i;
    int end;
    int size; 

    String FS_path = "/fserver";
    Socket FS_socket;
    int FS_port;
    String FS_hostname;
    ObjectInputStream FS_in;
    ObjectOutputStream FS_out;

    PartitionPacket FS_packet;
    PartitionPacket packet;
    boolean isNewPartition = false;
    boolean isDiffWorkers = false;

    String client_hash;
    String hash_result;

    boolean finishedWorking = false;

    Semaphore sem = new Semaphore(1);

    /**
     * @param args
     */
    public WorkerHandler (ZkConnector zkc, String path, String w_id_string) throws IOException {
	super("WorkerHandler");

	debug("WorkherHandler thread created for " + path);

	// Save variables
	this.zkc = zkc;
	zk = zkc.getZooKeeper();

	this.path = path;
	this.client_hash = path.split("/")[2];

	this.w_id_string = w_id_string;
	this.w_id = Integer.parseInt(w_id_string);

	// Register to /jobs/[job name]
	registerToJob();

	waitUntilExists(resultsPath + "/" + client_hash);

	setup();
	// Keep a watch on the workers for any changes
	//listenToPathChildren(myPath);
    }

    private void setup(){
	// Save a copy of current workers
	saveCurrentWorkers();

	boolean gotNewPartition = false;

	while(!gotNewPartition){
	    // Get hostname and port of fileserver from Zookeeper
	    getFileServerInfo();

	    // Connect to FileServer
	    connectToFileServer();

	    // Get num of workers
	    getNumWorkers();

	    // Request for a library partition
	    gotNewPartition = getDictPartition();
	}
    }

    // Keep track of all workers currently present
    private void registerToJob(){
	// Create your folder in the path
	try{
	    // Wait until /worker is created
	    waitUntilExists(path);

	    String p;
	    p = zk.create(
			     path + "/" + w_id_string,         // Path of znode
			     null,           // Data not needed.
			     ZooDefs.Ids.OPEN_ACL_UNSAFE,
			     CreateMode.EPHEMERAL  
			     );
	} catch (Exception e){
	    debug("registerWorker: Couldn't register :(");
	}
    }

    private void waitUntilExists(final String p){
	final CountDownLatch nodeCreatedSignal = new CountDownLatch(1);
	Stat stat = null;

	try {
            stat = zk.exists(
			     p, 
			     new Watcher() {       // Anonymous Watcher
				 @Override
				     public void process(WatchedEvent event) {
				     // check for event type NodeCreated
				     boolean isNodeCreated = event.getType().equals(EventType.NodeCreated);
				     // verify if this is the defined znode
				     boolean isMyPath = event.getPath().equals(p);
				     if (isNodeCreated && isMyPath) {
					 System.out.println(p + " created!");
					 nodeCreatedSignal.countDown();
				     }
				 }
			     });
        } catch(KeeperException e) {
            System.out.println(e.code());
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
                            
        System.out.println("Waiting for " + p + " to be created ...");
        
	if(stat != null){
	    debug("waitUntilExists: " + p + " is already created.");
	    return;
	}

        try{       
            nodeCreatedSignal.await();
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

	debug("waitUntilExists: " + p + " exists.");
    }

    // Get hostname and port of fileserver
    private void getFileServerInfo(){
	boolean isConnected = false;

	while(!isConnected){
	    try{

		// Wait until /fserver is created
		waitUntilExists(FS_path);

		byte[] data = null;

		while(data == null)
		    data = zk.getData(FS_path, false, null);

		String string_data = byteToString(data);
		debug("getFileServerInfo: " + string_data);

		FS_hostname = string_data.split(":")[0];	 
		FS_port = Integer.parseInt(string_data.split(":")[1]);  

		isConnected = true;
	    } catch (Exception e){
		debug("getFileServerInfo: Abort! Didn't work.");
		e.printStackTrace();
	    }
	
	}
    }

    private void connectToFileServer(){
	try{
	    FS_socket = new Socket(FS_hostname,FS_port);

	    FS_in = new ObjectInputStream(FS_socket.getInputStream());
	    FS_out = new ObjectOutputStream(FS_socket.getOutputStream());
	} catch (Exception e){
	    debug("connectToFileServer: Couldn't add the streams.");
	}
    }

    private int getNumWorkers(){
	try{
	    // Count amount of workers
	    List <String> workers;

	    workers = zk.getChildren(myPath, null);

	    numWorkers = 0;
	    for(String worker : workers){
		debug(worker);
		numWorkers++;

		if(Integer.parseInt(worker) == w_id){
		    partition_id = numWorkers;
		}
	    }
	    return numWorkers;
	} catch (Exception e){
	    debug("getNumWorkers: Didn't work");
	}

	return -1;
    }


    private void saveCurrentWorkers(){
	try{
	    workers = zk.getChildren(myPath, null);
	} catch (Exception e){
	    debug("getNumWorkers: Didn't work");
	}
    }

    private boolean getDictPartition(){
	try{
	    debug("getDictPartition: partition_id = " + partition_id + " numWorkers = " + numWorkers);

	    // Create dictionary request packet
	    packet = new PartitionPacket(PartitionPacket.PARTITION_REQUEST, partition_id, numWorkers);

	    // Send out packet
	    FS_out.writeObject(packet);

	    // Read reply
	    FS_packet = (PartitionPacket) FS_in.readObject();

	    // Close all connections!
	    FS_out.close();
	    FS_in.close();
	    FS_socket.close();
	} catch (Exception e){
	    debug("getDictPartition: Gulp. Didn't work!");
	    e.printStackTrace();

	    return false;
	}

	dictionary = FS_packet.dictionary;
	size = FS_packet.size;
	return true;
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

    //Get to work
    // Start traversing through the partition and find the hash!
    public void run(){

	while(true){
	    if(!finishedWorking){
		boolean wordMatched;
		wordMatched = checkWord();

		if(wordMatched){
		    postResult(wordMatched);
		    exit();
		    break;
		} else{
		    // If it got here, the word didn't match!
		    // Set /jobs/[job name]/[ID] as -1
		    debug("run: I couldn't find the hashed code! :C");
		    setData(path + "/" + w_id_string, "-1");
		}

		finishedWorking = true;
	    }

	    // Keep polling 
	    boolean isNewMembers = checkNewMembers(); // Check if new members have entered
	    if(isNewMembers){
		finishedWorking = false;

		// Get partition again
		setup();
		continue;
	    }

	    boolean canExit = mayExit();
	    if(canExit){
		exit();
		break;
	    }
	}
    }

    private void exit(){
	// Delete your node
	try{
	    zk.delete(path + "/" + w_id_string,-1);
	    debug("exit: " + path);
	} catch (Exception e){
	    debug("exit: Couldn't delete node!");
	    e.printStackTrace();

	}
    }

    private void setData(String p, String message){
	try{
	    debug("setData: " + message);
	    zk.setData(p, message.getBytes(), -1);
	} catch (Exception e){
	    debug("setData: Couldn't post message " + message + " at path " + p);
	}
    }

    private boolean mayExit(){
	// Check results node
	try{
	    byte[] data = null;

	    while(data == null)
		data = zk.getData(resultsPath + "/" + client_hash, false, null);

	    String string_data = byteToString(data);
	    if(string_data.contains("success") || string_data.equals("fail")){
		debug("mayExit: The job is either success or fail");
		return true;
	    }
	} catch (Exception e){
	    // Node has been deleted! You may exit
	    debug("mayExit: node delete");
	    e.printStackTrace();
	    return true;
	}
    
	int num = 0;
	// Check /jobs/[job name]/[workers] nodes
	try{
	    for(String w: workers){
		// Get data in each worker
		byte[] data = null;

		data = zk.getData(path + "/" + w, false, null);

		if(data == null)
		    return false;

		String string_data = byteToString(data);
		if(string_data.equals("-1"))
		   num++;		
	    }
	
	    if(num == workers.size()){
		debug("mayExit: All workers signaled they can't find password");
		postResult(false);    
		return true;
	    }
	} catch (Exception e){
	    // A worker has been deleted!
	    return false;
	}

	return false;
    }


    private boolean checkNewMembers(){
	try{
	    // Count amount of workers
	    List <String> curWorkers;

	    curWorkers = zk.getChildren(myPath, null);

	    // Check if num of workers went down
	    if(curWorkers.size() < workers.size()){
		debug("checkNewMembers: There are less workers!");
		workers = curWorkers;
		return true;
	    }

	       // Check if new workers joined
	    for(i = 0; i < curWorkers.size() && i < workers.size(); i++){
		if(!curWorkers.get(i).contains(workers.get(i))){
		    debug("checkNewMembers: A previous worker quit/failed");
		    return true;
		}
	    }
	    
	} catch (Exception e){
	    debug("getNumWorkers: Didn't work");	    
	}

	return false;
    }

    private void postResult(boolean wordMatched){
	debug("postResult: " + wordMatched);

	if(wordMatched){
	    // Return the password!
	    try{
		String result = "success:" + hash_result;
		debug("postResult: Congrats! The password exists: " + result);
		zk.setData(resultsPath + "/" + client_hash, result.getBytes(), -1);
	    } catch (Exception e){
		debug("run: Couldn't post success message.");
	    }
	} else if (!wordMatched){
	    // Couldn't find password
	    try{
		debug("postResult: Couldn't find hash " + client_hash);
		String result = "fail";
		zk.setData(resultsPath + "/" + client_hash, result.getBytes(), -1);		
	    } catch (Exception e) {
		debug("postResult: Couldn't post result");
		e.printStackTrace();
	    }
	}
    }

    private boolean checkWord(){
	// Hash each word in the partition.
	// Check if it exists

	for(int index = 0; index < size; index++){
	    if(isNewPartition){
		dlock.lock();

		index = 0;

		isNewPartition = false;

		dlock.unlock();
	    }


	    if(isDiffWorkers){
		dlock.lock();

		isDiffWorkers = false;
		//listenToPathChildren(myPath);

		dlock.unlock();
	    }

	    String word = dictionary.get(index);

	    String hash = getHash(word);

	    if(hash.equals(client_hash)){
		// The client's hash is the same as one in the dictionary!	
		hash_result = word;
		return true;
	    }
	}       

	debug("run: Couldn't find the hashed password!");
	return false;
    }

    // Place a watch on the children of a given path
    // Watch the other workers
    private void listenToPathChildren(final String p){
	debug("listenToPathChildren: " + p);

	try {
	    workers = zk.getChildren(
				     p, 
				     new Watcher() {       
					 @Override
					     public void process(WatchedEvent event) {

					     dlock.lock();

					     isDiffWorkers = true;

					     int num = getNumWorkers();
					     if(num < numWorkers){
						 isNewPartition = true;

						 // Wait until /fserver is created
						 waitUntilExists(FS_path);

						 // Get hostname and port of fileserver from Zookeeper
						 getFileServerInfo();

						 // Connect to FileServer
						 connectToFileServer();

						 // Oh no! The amount of workers has scaled.
						 // Request a new patition.
						 getDictPartition();
					     }

					     dlock.unlock();

					 }

				     });

	    debug("listenToPathChildren: Created a watch on " + path + " children.");
	} catch(Exception e) {
	    e.printStackTrace();
	}                          
    }

    public static String getHash(String word) {

	String hash = null;
	try {
	    MessageDigest md5 = MessageDigest.getInstance("MD5");
	    BigInteger hashint = new BigInteger(1, md5.digest(word.getBytes()));
	    hash = hashint.toString(16);
	    while (hash.length() < 32) hash = "0" + hash;
	} catch (NoSuchAlgorithmException nsae) {
	    // ignore
	}
	return hash;
    }


    private static void debug (String s) {
	if (debug && w_id != null) {
	    System.out.println(String.format("WORKER_HANDLER_%d: %s", w_id, s));
	} else {
	    System.out.println(String.format("WORKER_HANDLER_?: %s", s));		
	}
    }
}
