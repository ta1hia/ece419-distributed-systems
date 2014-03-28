package unhasher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;


public class ClientDriver {

	static BufferedReader br = null;

	static Integer port;
	static String zkhost;
	static Integer zkport;

	ZkConnector zkc;
	static ZooKeeper zk;  //need to lock this
	
	//ZooKeeper job directory
	static String ZK_TASK = "/tasks/t";


	static boolean debug = true;

	public ClientDriver() {

		//connect to ZooKeeper
		zkc = new ZkConnector();
		try {			
			debug("Connecting to ZooKeeper instance zk");
			String hosts = String.format("%s:%d", zkhost, zkport);
			debug(hosts);
			zkc.connect(hosts);
			zk = zkc.getZooKeeper();
			debug("Connected to ZooKeeper instance zk");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendPacket(TaskPacket p) {
		String data = p.taskToString();
		
		Code ret = createPath(data);
		
		if (ret == Code.OK) debug("task sent!"); 
	}
	
	private KeeperException.Code createPath(String data) {
        try {
            byte[] byteData = null;
            if(data != null) {
                byteData = data.getBytes();
            }
            zk.create(ZK_TASK, 
            		byteData, 
            		ZooDefs.Ids.OPEN_ACL_UNSAFE, 
            		CreateMode.EPHEMERAL_SEQUENTIAL);
            
        } catch(KeeperException e) {
            return e.code();
        } catch(Exception e) {
            return KeeperException.Code.SYSTEMERROR;
        }
        return KeeperException.Code.OK;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		if(args.length == 2) {
			zkhost = args[0];
			zkport = Integer.parseInt(args[1]);

		} else {
			System.err.println("ERROR: Invalid arguments!");
			System.exit(-1);
		}
		
		ClientDriver cd = new ClientDriver();

		br = new BufferedReader (new InputStreamReader(System.in));

		// in loop, wait for client input 
		String buf, cmd, hash;
		TaskPacket toZk = null;

		System.out.print("> ");
		while ((buf = br.readLine()) != null ) {
			String[] tokens = buf.split("[ ]+");
			cmd = tokens[0].toLowerCase();
			
			debug("client command is -> " + buf);
			
			if (cmd.equals("quit") || cmd.equals("q") ) {
				debug("quitting...");
				return;
			}
			
			hash = tokens[1];
			
			if (cmd.equals("job")) {
				toZk = new TaskPacket(TaskPacket.TASK_SUBMIT, hash);
			} else if (cmd.equals("status")) {
				toZk = new TaskPacket(TaskPacket.TASK_QUERY, hash);
			} 
			cd.sendPacket(toZk);
			System.out.print("> ");
		}
	}

	private static void debug (String s) {
		if (debug) {
			System.out.println(String.format("CLIENT: %s", s ));
		}
	}


}
