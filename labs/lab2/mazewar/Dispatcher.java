import java.util.ArrayList;
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
    ArrayList socketOutList = null;

    public Dispatcher(ServerData data) {
        this.eventQueue = data.eventQueue;
        this.clientTable = data.clientTable;
        this.socketOutList = data.socketOutList;
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
                    System.out.println("DISPATCHER: sending packet type " + event.packet_type);

                    // Go through each client	    
                    for(int i=0;i < socketOutList.size(); i++){
                        ((ObjectOutputStream)socketOutList.get(i)).writeObject(event);
                    }
                    seqNum++;
                    if(seqNum == 501)
                        seqNum = 0;
                }
                 Thread.sleep(500);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}

