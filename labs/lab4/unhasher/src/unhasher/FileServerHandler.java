package unhasher;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.Lock;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

public class FileServerHandler extends Thread {
	
    ZkConnector zkc;

    Socket cSocket = null;
    ObjectInputStream cin;
    ObjectOutputStream cout;
    
    TaskPacket packetFromCD;
    ZooKeeper zk;
    Lock zklock;

    // Setup
    public FileServerHandler (Socket socket, ZkConnector zkc, ZooKeeper zk) throws IOException {
        super("FileServerHandler");
        try {
            this.cSocket = socket;
            this.cout = new ObjectOutputStream(cSocket.getOutputStream());
	    this.cin = new ObjectInputStream(cSocket.getInputStream());

            this.zk = zk;
	    this.zkc = zkc;

            System.out.println("Created new FileServerHandler to handle remote client");
        } catch (IOException e) {
            System.out.println("IO Exception");
        }
    }

    public void run(){

    }

}
