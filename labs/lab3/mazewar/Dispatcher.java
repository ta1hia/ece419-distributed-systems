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

    public void sendToClient(int client_id, MazePacket packetToClient){
	try{
	    ((ObjectOutputStream)socketOutList.get(client_id)).writeObject(packetToClient);
	  
        } catch (IOException e) {
            e.printStackTrace();
        }  
    }

    public void send(MazePacket packetToClients){
	try{
	    // Try and get a valid lamport clock!
	    MazePacket getClock = new MazePacket();
	    getClock.packet_type = MazePacket.CLIENT_CLOCK;
	    getClock.lamportClock = lamportClock + 1;

	    for(int i=0;i < socketOutList.size(); i++){
		((ObjectOutputStream)socketOutList.get(i)).writeObject(getClock);
	    }

	    //while(lamportClockQueue.isEmpty()){
		// Broadcast you want a lamport clock at (lamportClock + 1)

		// Event handler checks if lamport clock is valid
		// If it is, add it to the queue
		// If not, broadcast another lamport clock request
		// Wakeup threads sleeping on lamport clock
	    //}
	
	    // Go through each client	    
	    for(int i=0;i < socketOutList.size(); i++){
		((ObjectOutputStream)socketOutList.get(i)).writeObject(packetToClients);
	    }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

