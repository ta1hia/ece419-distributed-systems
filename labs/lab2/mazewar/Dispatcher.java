import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/* Dispatcher class
 * Dispatches messages from event queue and broadcasts
 * events to all remote clients.
 *
 */
public class Dispatcher extends Thread {
    ServerData data = null; /* eventQueue and clientTable are accessed through here */

    public Dispatcher(ServerData gameData) {
        this.data = gameData;
    }

    // Continually check eventqueue
    // Broadcast whenever queue is not empty
    public void run(){
        boolean new_event;
        int seqNum = 0;

        while(true){ //???
         //   if(new_event = eventQueue.peek() != null){
                // Go through each client	    
             //   for(int i=0;i < clientTable.size(); i++){
             //       clientTable.get(i).send(seqNum, command);
               // }
                seqNum++;
                if(seqNum == 501)
                    seqNum = 0;
           // }
           // Thread.sleep(500);
        }

    }


}

