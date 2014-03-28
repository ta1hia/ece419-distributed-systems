package unhasher;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;
import java.util.List;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class ZkConnector implements Watcher {

    // ZooKeeper Object
    ZooKeeper zooKeeper;

    // To block any operation until ZooKeeper is connected. It's initialized
    // with count 1, that is, ZooKeeper connect state.
    CountDownLatch connectedSignal = new CountDownLatch(1);
    
    // ACL, set to Completely Open
    protected static final List<ACL> acl = Ids.OPEN_ACL_UNSAFE;

    /**
     * Connects to ZooKeeper servers specified by hosts.
     */
    public void connect(String hosts) throws IOException, InterruptedException {

        zooKeeper = new ZooKeeper(
                hosts, // ZooKeeper service hosts
                5000,  // Session timeout in milliseconds
                this); // watcher - see process method for callbacks
	    connectedSignal.await();
    }

    /**
     * Closes connection with ZooKeeper
     */
    public void close() throws InterruptedException {
	    zooKeeper.close();
    }

    /**
     * @return the zooKeeper
     */
    public ZooKeeper getZooKeeper() {
        // Verify ZooKeeper's validity
        if (null == zooKeeper || !zooKeeper.getState().equals(States.CONNECTED)) {
	        throw new IllegalStateException ("ZooKeeper is not connected.");
        }
        return zooKeeper;
    }

    protected Stat exists(String path, Watcher watch) {
        
        Stat stat =null;
        try {
            stat = zooKeeper.exists(path, watch);
        } catch(Exception e) {
        }
        
        return stat;
    }

    protected KeeperException.Code create(String path, String data, CreateMode mode) {
        
        try {
            byte[] byteData = null;
            if(data != null) {
                byteData = data.getBytes();
            }
            zooKeeper.create(path, byteData, acl, mode);
            
        } catch(KeeperException e) {
            return e.code();
        } catch(Exception e) {
            return KeeperException.Code.SYSTEMERROR;
        }
        
        return KeeperException.Code.OK;
    }

    public void process(WatchedEvent event) {
        // release lock if ZooKeeper is connected.
        if (event.getState() == KeeperState.SyncConnected) {
            connectedSignal.countDown();
        }
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
    
    public void listenToPath(final String path){
		final CountDownLatch nodeCreatedSignal = new CountDownLatch(1);

		try {
			zooKeeper.exists(
					path, 
					new Watcher() {       // Anonymous Watcher
						@Override
						public void process(WatchedEvent event) {
							// check for event type NodeCreated
							boolean isNodeCreated = event.getType().equals(EventType.NodeCreated);
							// verify if this is the defined znode
							boolean isMyPath = event.getPath().equals(path);
							if (isNodeCreated && isMyPath) {
								System.out.println(path + " created!");
								nodeCreatedSignal.countDown();
							}
						}
					});
		} catch(KeeperException e) {
			System.out.println(e.code());
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Waiting for " + path + " to be created ...");

		try{       
			nodeCreatedSignal.await();
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}
}

