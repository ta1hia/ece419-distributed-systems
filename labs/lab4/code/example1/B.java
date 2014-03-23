import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher.Event.EventType;

import java.util.concurrent.CountDownLatch;

public class B {
    
    static CountDownLatch nodeCreatedSignal = new CountDownLatch(1);
    static String myPath = "/ece419";
    
    public static void main(String[] args) {
  
        if (args.length != 1) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. B zkServer:clientPort");
            return;
        }
    
        ZkConnector zkc = new ZkConnector();
        try {
            zkc.connect(args[0]);
        } catch(Exception e) {
            System.out.println("Zookeeper connect "+ e.getMessage());
        }

        ZooKeeper zk = zkc.getZooKeeper();
        
        try {
            zk.exists(
                myPath, 
                new Watcher() {       // Anonymous Watcher
                    @Override
                    public void process(WatchedEvent event) {
                        // check for event type NodeCreated
                        boolean isNodeCreated = event.getType().equals(EventType.NodeCreated);
                        // verify if this is the defined znode
                        boolean isMyPath = event.getPath().equals(myPath);
                        if (isNodeCreated && isMyPath) {
                            System.out.println(myPath + " created!");
                            nodeCreatedSignal.countDown();
                        }
                    }
                });
        } catch(KeeperException e) {
            System.out.println(e.code());
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
                            
        System.out.println("Waiting for " + myPath + " to be created ...");
        
        try{       
            nodeCreatedSignal.await();
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        System.out.println("DONE");
    }
}
