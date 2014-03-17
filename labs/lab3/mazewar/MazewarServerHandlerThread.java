import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import java.io.*;
import java.net.*;

/* MazewarServerHandlerThread class
 *
 * MazewarServer spawns this thread for each remote
 * client that connects
 */

public class MazewarServerHandlerThread extends Thread {
    Socket rcSocket = null;
    ServerData data = null;

    ObjectInputStream cin;
    ObjectOutputStream cout;

    MazePacket packetFromRC;
    MazePacket packetToRC;

    Random rand = new Random();

    Integer myId;
    int seqNum = 1;
    boolean quitting = false;

    int lamportClock;
    Dispatcher dispatcher;
    ClientHandlerThread chandler;

    boolean debug = true;

    public MazewarServerHandlerThread (Socket socket, ServerData sdata, Dispatcher dispatcher, ClientHandlerThread chandler) throws IOException {
        super("MazewarServerHandlerThread");
        try {
            this.rcSocket = socket;
            this.cout = new ObjectOutputStream(rcSocket.getOutputStream());
            this.data = sdata;
            this.dispatcher = dispatcher;
            //data.addSocketOutToList(cout);

            this.chandler = chandler;

            System.out.println("Created new MazewarServerHandlerThread to handle remote client ");
        } catch (IOException e) {
            System.out.println("IO Exception");
        }
    }

    public void run() {
        System.out.println("Connecting to client...");
        int lastPacketNum;

        try {
            /* Loop: 
             */
            cin = new ObjectInputStream(rcSocket.getInputStream());
            while (!quitting && ((packetFromRC = (MazePacket) cin.readObject()) != null)) {
                System.out.println("S_HANDLER: packet type is " + packetFromRC.packet_type);

                /* Process each packet */
                switch (packetFromRC.packet_type) {
                    case MazePacket.CLIENT_CLOCK:
                        clientClock();
                        break;
                    case MazePacket.CLIENT_ACK:
                        clientAwk();
                        break;
                    case MazePacket.CLIENT_REGISTER:
                        registerClientEvent();
                        break;
                    case MazePacket.CLIENT_SPAWN:
                        clientSpawn();
                        break;
                    case MazePacket.CLIENT_FORWARD:
                        clientForwardEvent();
                        break;
                    case MazePacket.CLIENT_BACK:
                        clientBackEvent();
                        break;
                    case MazePacket.CLIENT_LEFT:
                        clientLeftEvent();
                        break;
                    case MazePacket.CLIENT_RIGHT:
                        clientRightEvent();
                        break;
                    case MazePacket.CLIENT_FIRE:
                        clientFireEvent();
                        break;
                    case MazePacket.CLIENT_RESPAWN:
                        clientRespawn();
                        break;
                    case MazePacket.RESERVE_POINT:
                        reservePoint();
                        continue;	  
                    case MazePacket.GET_SEQ_NUM:
                        getSeqNum();
                        continue;
                    case MazePacket.CLIENT_QUIT:
                        clientQuit();
                        break;
                    default:
                        System.out.println("S_HANDLER: Could not recognize packet type");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }

    // Client is requesting for a valid clock!
    private void clientClock(){
        MazePacket eventPacket = new MazePacket();
        int requested_lc = packetFromRC.lamportClock;    
        //int lamportClock = dispatcher.getLamportClock();
        eventPacket.packet_type = MazePacket.CLIENT_ACK;


        debug("S_HANDER: requested_lc: " + requested_lc + " current lamportClock: " + data.getLamportClock());
        eventPacket.lamportClock = data.getLamportClock();

        if(requested_lc == lamportClock){
            debug("incrementing my lc after recieving CLIENT_CLOCK packet");
            // Clock is valid!
            data.incrementLamportClock();
            //Set up and send awknowledgement packet
            //eventPacket.lamportClock = lamportClock;
            eventPacket.isValidClock = true;

        } else{
            // Oh no! The lamport clock is not valid.
            // Send the latest lamport clock and disawknowledgement packet
            eventPacket.isValidClock = false;
        }

        dispatcher.sendToClient(packetFromRC.client_id, (MazePacket) eventPacket);
    }

    private void clientSpawn(){

        //chandler.spawnClient(packetFromRC.client_id,packetFromRC.lookupTable);
        if (packetFromRC.for_new_client) {
            data.releaseSemaphore(1);
	    
	    // Update to the latest lamport clock
	    data.setLamportClock(packetFromRC.lamportClock);
	    
	    chandler.spawnClient(packetFromRC.client_id,packetFromRC.lookupTable);
	  
        } else { 
	    chandler.addEventToQueue(packetFromRC);
	    chandler.runEventFromQueue(packetFromRC.lamportClock);
	}

    }

    // This client is awknowledging/disawk for lamport clock validation
    // Count the amount of awks
    private void clientAwk(){
        int lamportClock = packetFromRC.lamportClock;
        boolean clockIsValid = packetFromRC.isValidClock;

        if(!clockIsValid){
            // Update the current lamport clock
            data.setLamportClock(packetFromRC.lamportClock);
        }

        data.releaseSemaphore(1);
    }

    // The client is quitting.
    private void clientQuit(){
        try{
            MazePacket eventPacket = new MazePacket();
            String rc_name = packetFromRC.client_name;
            System.out.println("S_HANDLER: " + rc_name + " is quitting");

            eventPacket.client_name = rc_name;
            eventPacket.packet_type = MazePacket.CLIENT_QUIT;

            quitting = true;

            // Remove that client from the client table!
            data.removeClientFromTable(rc_name);
            //data.removeSocketOutFromList(cout);

            // Close all connections!
            cin.close();
            cout.close();
            rcSocket.close();

            data.addEventToQueue(eventPacket);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }

    }

    private void getSeqNum(){
        try { 
            MazePacket eventPacket = new MazePacket();
            System.out.println("S_HANDLER: Send sequence number.");

            eventPacket.packet_type = MazePacket.GET_SEQ_NUM;

            // TODO: handle this?
            chandler.addEventToQueue(eventPacket);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }

    private void clientForwardEvent() {
        try { 
            MazePacket eventPacket = new MazePacket();
            Integer id = packetFromRC.client_id;
            System.out.println("S_HANDLER: " + id + " forward");

            eventPacket.client_id = id;
            eventPacket.packet_type = MazePacket.CLIENT_FORWARD;
            eventPacket.lamportClock = packetFromRC.lamportClock;

	    System.out.println("LISTENER: THIS IS THE LAMPORT CLOCK: "+ eventPacket.lamportClock);

            chandler.addEventToQueue(eventPacket);
            chandler.runEventFromQueue(packetFromRC.lamportClock);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }

    private void clientBackEvent() {
        try { 
            MazePacket eventPacket = new MazePacket();
            Integer id = packetFromRC.client_id;
            System.out.println("S_HANDLER: " + id + " back");

            eventPacket.client_id = id;
            eventPacket.packet_type = MazePacket.CLIENT_BACK;
            eventPacket.lamportClock = packetFromRC.lamportClock;

            chandler.addEventToQueue(eventPacket);
            chandler.runEventFromQueue(packetFromRC.lamportClock);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }

    private void clientLeftEvent() {
        try { 
            MazePacket eventPacket = new MazePacket();
            Integer id = packetFromRC.client_id;
            System.out.println("S_HANDLER: " + id + " left");

            eventPacket.client_id = id;
            eventPacket.packet_type = MazePacket.CLIENT_LEFT;
            eventPacket.lamportClock = packetFromRC.lamportClock;

            chandler.addEventToQueue(eventPacket);
            chandler.runEventFromQueue(packetFromRC.lamportClock);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }

    private void clientRightEvent() {
        try { 
            MazePacket eventPacket = new MazePacket();
            Integer id = packetFromRC.client_id;
            System.out.println("S_HANDLER: " + id + " right");

            eventPacket.client_id = id;
            eventPacket.packet_type = MazePacket.CLIENT_RIGHT;
            eventPacket.lamportClock = packetFromRC.lamportClock;

            chandler.addEventToQueue(eventPacket);
            chandler.runEventFromQueue(packetFromRC.lamportClock);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }

    private void clientFireEvent() {
        try { 
            MazePacket eventPacket = new MazePacket();
            Integer id = packetFromRC.client_id;
            System.out.println("S_HANDLER: " + id + " fire");

            eventPacket.client_id = id;
            eventPacket.packet_type = MazePacket.CLIENT_FIRE;
            eventPacket.lamportClock = packetFromRC.lamportClock;

            chandler.addEventToQueue(eventPacket);
            chandler.runEventFromQueue(packetFromRC.lamportClock);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }

    /* A new client wants to register
     * - Give your current point and direction!
     */
    private void registerClientEvent() {
        try {
            System.out.print("registerClientEvent");

            /* Wait for handshaking packet from client, store client state in 
             * global client table */
            MazePacket eventPacket = new MazePacket();

            /* Add to client list */
            eventPacket.client_id = chandler.getMyId();
            eventPacket.packet_type = MazePacket.CLIENT_SPAWN;
            eventPacket.for_new_client = true;
            eventPacket.lookupTable = new ConcurrentHashMap();
            eventPacket.lookupTable.put(chandler.getMyId(), chandler.getMe());
	    eventPacket.lamportClock = data.getLamportClock();

            /* Get new client socket info */
            String hostname = packetFromRC.client_host;
            Integer id = packetFromRC.client_id;
            int port = packetFromRC.client_port;
            dispatcher.connectToPeer(id, hostname, port);

            /* Add packet to event queue */
            dispatcher.sendToClient(id,eventPacket);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }

    /* When a client spawns.moves, it needs to reserve its position (aka. Point). You don't want multiple clients spawning in the same spot!
     */
    private void reservePoint(){
        // Check if another client currently has that position.

        /* Add to client list */
        String rc_name = packetFromRC.client_name;
        Point rc_point = packetFromRC.client_location;


        MazePacket serverResponse = new MazePacket();
        serverResponse.packet_type = MazePacket.RESERVE_POINT; 	    

        if(data.setPosition(rc_name,rc_point)){
            serverResponse.error_code = 0;
            System.out.println("S_HANDLER: Reserving position successful. " + rc_name );
        }else{	
            serverResponse.error_code = MazePacket.ERROR_RESERVED_POSITION;
            System.out.println("S_HANDLER: Reserving position failed. " + rc_name );
        }

        try{
            cout.writeObject(serverResponse);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }

    private void clientRespawn(){
        try { 
            MazePacket eventPacket = new MazePacket();
            Point p = packetFromRC.client_location;
            Direction d = packetFromRC.client_direction;

            System.out.println("S_HANDLER: " + packetFromRC.tc + " respawning");

            eventPacket.sc = packetFromRC.sc;
            eventPacket.tc = packetFromRC.tc;
            eventPacket.client_location = p;
            eventPacket.client_direction = d;
            eventPacket.packet_type = MazePacket.CLIENT_RESPAWN;

            data.addEventToQueue(eventPacket);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }

    public void debug(String s) {
        if (debug) {
            System.out.println("S_HANDLER: " + s);
        }
    }

}
