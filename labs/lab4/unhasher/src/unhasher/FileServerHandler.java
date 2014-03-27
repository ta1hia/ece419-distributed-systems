package unhasher;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.Lock;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

public class FileServerHandler extends Thread implements Watcher {
	
    Socket cSocket = null;
    ObjectInputStream cin;
    ObjectOutputStream cout;
    
	TaskPacket packetFromCD;
	ZooKeeper zk;
	Lock zklock;


	/**
	 * @param args
	 */
	public FileServerHandler (Socket socket, ZooKeeper zooKeeper, Lock lock) throws IOException {
        super("FileServerHandler");
        try {
            this.cSocket = socket;
            this.cout = new ObjectOutputStream(cSocket.getOutputStream());
            this.zk = zooKeeper;
            this.zklock = lock;

            System.out.println("Created new FileServerHandler to handle remote client ");
        } catch (IOException e) {
            System.out.println("IO Exception");
        }
    }



	@Override
	public void process(WatchedEvent event) {
		// TODO Auto-generated method stub
		
	}
	


}
