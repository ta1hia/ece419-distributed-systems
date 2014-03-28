package unhasher;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.Lock;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;

import java.util.concurrent.ConcurrentHashMap;

// Hashing library
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;

// Each WorkerHandler is assigned a job
// Try and find if the password hash is equal to the word hash in worker's dictionary partition
public class WorkerHandler extends Thread{
	

    Socket cSocket = null;
    ObjectInputStream cin;
    ObjectOutputStream cout;
    
    TaskPacket packetFromCD;
    ZooKeeper zk;
    Lock zklock;

    ConcurrentHashMap <String,String> dictionary;

    /**
     * @param args
     */
    public WorkerHandler (ZkConnector zkc, String path, ConcurrentHashMap dictionary) throws IOException {
        super("WorkerHandler");
      
	String w_path = path + "/w";

	// Create your folder in the path
	zkc.create(
		   w_path,         // Path of znode
		   null,           // Data not needed.
		   CreateMode.EPHEMERAL_SEQUENTIAL   
		   );
    }

    //Get to work
    public void run(){
	// Hash each word in the partition.
	// Check if it exists
	Object[] keys = dictionary.keySet().toArray();
	int size = dictionary.size(); 

	for(int i = 0; i < size; i++){
	    String key = keys[i].toString();
	    String word = dictionary.get(key);

	    String hash = getHash(word);

	    // if(hash == client_hash){
	    // 	// The client's hash is the same as one in the dictionary!
	    // 	// Return the password!
		
	    // }
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
}
