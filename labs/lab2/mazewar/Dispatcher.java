import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.Set;
import java.io.*;

/* Dispatcher class
 * Dispatches messages from event queue and broadcasts
 * events to all remote clients.
 *
 */
public class Dispatcher extends Thread {
    BlockingQueue<MazePacket> eventQueue = null;
    ConcurrentHashMap<String, ClientData> clientTable = null;

    public Dispatcher(ServerData data) {
        this.eventQueue = data.eventQueue;
        this.clientTable = data.clientTable;
    }

    // Continually check eventqueue
    // Broadcast whenever queue is not empty
    public void run(){
        MazePacket event;
        int seqNum = 0;

        try {

            while(true){ //???
                if(eventQueue.peek() != null){
                    event = eventQueue.take();

                    // Go through each client	    
                    Set<String> clientSet = clientTable.keySet();

                    for (String client: clientSet) {
                        clientTable.get(client).csocket_out.writeObject(event);

                    }
                    //   for(int i=0;i < clientTable.size(); i++){
                    //       clientTable.get(i).send(seqNum, command);
                    // }
                    seqNum++;
                    if(seqNum == 501)
                        seqNum = 0;
                }
                // Thread.sleep(500);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}

