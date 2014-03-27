package unhasher;


public class ClientDriver {
	
	static Integer port;
	static String zkhost;
	static Integer zkport;
	
	ClientDriver() {
		
		//connect to ZooKeeper
		
		
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args.length == 4) {
			port = Integer.parseInt(args[0]);
			zkhost = args[1];
			zkport = Integer.parseInt(args[2]);

		} else {
			System.err.println("ERROR: Invalid arguments!");
			System.exit(-1);
		}
		
		// in loop, wait for client input 
		
	
	}

}
