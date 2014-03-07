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
    int seqNum;

    int lamportClock;

    public Dispatcher(ServerData data) {
        this.eventQueue = data.eventQueue;
        this.clientTable = data.clientTable;
        this.socketOutList = data.socketOutList;
	this.lamportClock = data.lamportClock;
    }

    // Continually check eventqueue
    // Broadcast whenever queue is not empty
    public void run(){
        MazePacket event;
	seqNum = 1;

        try {

            while(true){
                if(eventQueue.peek() != null){
                    event = eventQueue.take();
                    System.out.println("DISPATCHER: sending packet type " + event.packet_type + " with sequence number " + seqNum);

		    event.sequence_num = seqNum;

                    // Go through each client	    
                    for(int i=0;i < socketOutList.size(); i++){
                        ((ObjectOutputStream)socketOutList.get(i)).writeObject(event);


		    }


		    seqNum++;
		    if(seqNum == 21)
			seqNum = 1;
		}

                // Thread.sleep(200);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void send(MazePacket packetToClients){
	try{
	    // Try and get a valid lamport clock!
	

	
	    // Go through each client	    
	    for(int i=0;i < socketOutList.size(); i++){
		((ObjectOutputStream)socketOutList.get(i)).writeObject(packetToClients);
	    }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

