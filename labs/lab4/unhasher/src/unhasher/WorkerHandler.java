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

public class WorkerHandler extends Thread{
	

    Socket cSocket = null;
    ObjectInputStream cin;
    ObjectOutputStream cout;
    
    TaskPacket packetFromCD;
    ZooKeeper zk;
    Lock zklock;


    /**
     * @param args
     */
    public WorkerHandler (ZkConnector zkc, String path) throws IOException {
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

    }

}
