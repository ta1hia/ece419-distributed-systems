package unhasher;

import java.io.*;
import org.apache.zookeeper.ZooKeeper;


public class JobTracker {
	
	private static String hname;
	private static Integer port;
	
	/* Zookeeper vars */
	private static String zkhname;
	private static Integer zkport;
	ZooKeeper zk;

	/**
	 * @param args
	 * 
	 * arg0		port for JobTracker
	 * arg1 	hostname for Zookeeper
	 * arg2		port for Zookeeper
	 */
	public static void main(String[] args) throws IOException {
		
		if(args.length == 4) {
			port = Integer.parseInt(args[0]);
			zkhname = args[1];
			zkport = Integer.parseInt(args[2]);
		} else {
			System.err.println("ERROR: Invalid arguments!");
			System.exit(-1);
		}
		
		// handle client packets in a loop here
		

	}

}
