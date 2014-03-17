import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Set;
import java.io.*;
import java.net.*;

/* Dispatcher class
 * Dispatches messages from event queue and broadcasts
 * events to all remote clients.
 *
 */
public class Dispatcher extends Thread {
    BlockingQueue<MazePacket> eventQueue = null;
    ConcurrentHashMap<String, ClientData> clientTable = null;
    ConcurrentHashMap<Integer, ObjectOutputStream> socketOutList = new ConcurrentHashMap(); 
    int seqNum;

    int lamportClock;
    Semaphore sem;
    ServerData data;
    ClientHandlerThread chandler;

    Lock lock = new ReentrantLock();

    boolean debug = true;

    public Dispatcher(ServerData data, ClientHandlerThread chandler) {
        this.data = data;
        this.chandler = chandler;
        this.eventQueue = data.eventQueue;
        this.clientTable = data.clientTable;
        this.socketOutList = data.socketOutList;
        this.lamportClock = data.lamportClock;
        this.sem = data.sem;
    }

    // Continually check eventqueue
    // Broadcast whenever queue is not empty
    // public void run(){
    //     MazePacket event;
    // 	seqNum = 1;

    //     try {

    //         while(true){
    //             if(eventQueue.peek() != null){
    //                 event = eventQueue.take();
    //                 System.out.println("DISPATCHER: sending packet type " + event.packet_type + " with sequence number " + seqNum);

    // 		    event.sequence_num = seqNum;

    //                 // Go through each client	    
    //                 for(int i=0;i < socketOutList.size(); i++){
    //                     ((ObjectOutputStream)socketOutList.get(i)).writeObject(event);


    // 		    }


    // 		    seqNum++;
    // 		    if(seqNum == 21)
    // 			seqNum = 1;
    // 		}

    //             // Thread.sleep(200);
    //         }
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }

    // }

    public void connectToPeer(Integer id, String host, int port) {
        Socket socket = null;
        ObjectOutputStream t_out = null;

        // Save socket out!
        try{
            socket = new Socket(host, port);

            t_out = new ObjectOutputStream(socket.getOutputStream());
            data.addSocketOutToList(id, t_out);
        } catch(Exception e){
            System.err.println("ERROR: Coudn't connect to currently existing client");
        }				    
    }

    public void sendToClient(int client_id, MazePacket packetToClient){
        try{
            ((ObjectOutputStream)socketOutList.get(client_id)).writeObject(packetToClient);

            debug("sending packet "+ packetToClient.packet_type + ", called client " + client_id);
        } catch (IOException e) {
            e.printStackTrace();
        }  
    }

    public void send(MazePacket packetToClients){
        // Try and get a valid lamport clock!
        MazePacket getClock = new MazePacket();

        int requested_lc = lamportClock;

        debug("number of peers to broadcast to: " + socketOutList.size());
        if(socketOutList.size() > 0){
            try{
                // Request a lamport clock if there is more than one client.
                if(packetToClients.packet_type != MazePacket.CLIENT_REGISTER){
                    while(true){
                        getClock.packet_type = MazePacket.CLIENT_CLOCK;
                        getClock.lamportClock = requested_lc;
                        getClock.client_id = packetToClients.client_id;

                        // Request awknowledgement from everyone, but yourself
                        // Go through each remote client	    
                        for (ObjectOutputStream out : socketOutList.values()) {
                            out.writeObject(getClock);
                            debug("calling client for clock: " + requested_lc);	    
                        }

                        // Wait until all clients have aknowledged!
                        data.acquireSemaphore(socketOutList.size() - 1);

                        // You've finally woken up
                        // Check if the lamport clock is valid
                        // If lamport clock is the same as before, it is valid
                        // If it is not, it is invalid and you have to do it all over again
                        if(requested_lc == lamportClock){
                            break;
                        }
                    }

                    packetToClients.lamportClock = requested_lc;
                    data.setLamportClock(requested_lc + 1);

                }

                // Go through each remote client	    
                for (ObjectOutputStream out : socketOutList.values()) {
                    out.writeObject(packetToClients);
                    debug("sending a packet to client");		    
                }
                debug("sending packets to other clients finished, now adding to own queue");
                addEventToOwnQueue(packetToClients);

                if(packetToClients.packet_type == MazePacket.CLIENT_REGISTER){
                    data.acquireSemaphore(socketOutList.size() - 1);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addEventToOwnQueue(MazePacket packetToSelf) {
        debug("adding own event to queue");
        if (packetToSelf.packet_type != MazePacket.CLIENT_REGISTER) {
            MazePacket myEvent = new MazePacket();
            myEvent.packet_type = packetToSelf.packet_type;
            myEvent.client_name = packetToSelf.client_name;
            myEvent.client_id = packetToSelf.client_id;
            myEvent.lamportClock = packetToSelf.lamportClock;

            chandler.addEventToQueue(myEvent);
            chandler.runEventFromQueue(packetToSelf.lamportClock);
        }
    }

    public void debug(String s) {
        if (debug) {
            System.out.println("DISPATCHER: " + s);
        }
    }


}

