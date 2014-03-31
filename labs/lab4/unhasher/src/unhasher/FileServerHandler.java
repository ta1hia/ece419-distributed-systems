package unhasher;

import java.io.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.security.SecureRandom;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

public class FileServerHandler extends Thread {
	
    File dictionaryFile;
    String dictionaryPath = "unhasher/src/unhasher/dictionary/lowercase.rand";
    List <String> dictionary;

    InputStream is;
    BufferedReader br;

    ZkConnector zkc;

    Socket cSocket = null;
    ObjectInputStream cin;
    ObjectOutputStream cout;
    
    TaskPacket packetFromCD;
    ZooKeeper zk;
    Lock zklock;

    static boolean debug = true;

    // Setup
    public FileServerHandler (Socket socket, ZkConnector zkc, ZooKeeper zk) throws IOException {
        super("FileServerHandler");
        try {
	    // Store variables
            this.cSocket = socket;
            this.cout = new ObjectOutputStream(cSocket.getOutputStream());
	    this.cin = new ObjectInputStream(cSocket.getInputStream());

            this.zk = zk;
	    this.zkc = zkc;

	    // Store whole dictionary as a list
	    getDictionary();
	    
            debug("Created new FileServerHandler to handle remote client");
        } catch (IOException e) {
            System.out.println("IO Exception");
        }
    }

    public FileServerHandler(boolean debug){
	getDictionary();
    }

    private void getDictionary(){
	debug("Retrieving dictionary");

	dictionary = new <String>ArrayList();

	try{
	    //is = new FileInputStream(dictionaryPath);
	    br = new BufferedReader(new FileReader(dictionaryPath));

	    String line = null;
	    int i = 0;
	    // Traverse through dictionary and save it into the list
	    while((line = br.readLine()) != null){
		debug(line);
		dictionary.add(i,line);
	    }
	} catch(Exception e){
	    debug("Boo-hoo. Couldn't import dictionary");
	    e.printStackTrace();
	}
	
    }

    public void run(){
	// Read in packet.
	PartitionPacket packetFromWorker;

	try{
	    while((packetFromWorker = (PartitionPacket) cin.readObject()) != null){
		// Got a packet!
		// Reply back with a partition.

		PartitionPacket packetToWorker = new PartitionPacket(PartitionPacket.PARTITION_REPLY);
	    
	    

	    }
	} catch (Exception e){
	    debug("Oh no! Could not work out PartitionPacket");
	}
    }


    private static void debug (String s) {
	if (debug) {
	    System.out.println(String.format("FS: %s", s ));
	}
    }

}
