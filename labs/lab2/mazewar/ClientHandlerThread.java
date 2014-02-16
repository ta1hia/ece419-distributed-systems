import java.awt.event.KeyEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.net.*;

/* Client handler 
 * Each client will be registered with a client handler
 * Listens for actions by GUI client and notifies server
 * Receives game events queue from server and executes events 
 * 
 */

public class ClientHandlerThread extends Thread {
    Socket cSocket;
    Client me;
    Maze maze;
    ObjectOutputStream out;
    ObjectInputStream in;
    BlockingQueue<MazeEvent> eventQueue;
    ConcurrentHashMap<String, Client> clientTable;
    int seqNum;
    MazePacket []eventArray = new MazePacket[21];

    // Score table
    //ScoreTableModel scoreTable;


    MazePacket packetFromServer;

    public ClientHandlerThread(String host, int port){
        /* Connect to central game server. */
        try {
            /* Using this hardcoded port for now, eventually make this userinput at GUI interface in Mazewar.java*/
            System.out.println("Connecting to Mazewar Server...");

            cSocket = new Socket(host,port);
            out = new ObjectOutputStream(cSocket.getOutputStream());
            in = new ObjectInputStream(cSocket.getInputStream());
            clientTable = new ConcurrentHashMap();
	    //scoreTable = st;

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void registerMaze(Maze maze) {
        this.maze = maze;

	sendPacketToServer(MazePacket.GET_SEQ_NUM);
    }

    public void registerClientWithMazewar(){
        MazePacket packetToServer = new MazePacket();

        try{

            /* Initialize handshaking with server */
            Random rand = new Random();

            packetToServer.packet_type = MazePacket.CLIENT_REGISTER;
            packetToServer.client_name = me.getName();
            packetToServer.client_location = maze.getClientPoint(me);
            packetToServer.client_direction = me.getOrientation();
            packetToServer.client_type = MazePacket.REMOTE;
            System.out.println("CLIENT REGISTER: " + me.getName());
            out.writeObject(packetToServer);

            /* Init client table with yourself */
            clientTable.put(me.getName(), me);

        }catch (IOException e){
            e.printStackTrace();
            System.out.println("ERROR: registering with server");
        }

    }

    public void registerRobotWithMazewar(Client name){
        MazePacket packetToServer = new MazePacket();

        try{

            /* Initialize handshaking with server */
            Random rand = new Random();

            packetToServer.packet_type = MazePacket.CLIENT_REGISTER;
            packetToServer.client_name = me.getName();
            packetToServer.client_location = maze.getClientPoint(name);
            packetToServer.client_direction = me.getOrientation();
            packetToServer.client_type = MazePacket.REMOTE;
            System.out.println("CLIENT REGISTER: " + me.getName());
            out.writeObject(packetToServer);

            /* Init client table with yourself */
            clientTable.put(me.getName(), me);

        }catch (IOException e){
            e.printStackTrace();
            System.out.println("ERROR: registering with server");
        }

    }
    public void run() {
        /* Listen for packets */
        packetFromServer = new MazePacket();
	

        try {
            while((packetFromServer = (MazePacket) in.readObject()) != null || eventArray[seqNum] != null) {
		int packet_type = -1;


            	System.out.println("seqNum: " + seqNum);
		if(packetFromServer != null && packetFromServer.packet_type == MazePacket.GET_SEQ_NUM){
		    seqNum = packetFromServer.sequence_num + 1;

		    System.out.println("Sequence number set as: " + seqNum);
		    continue;
		}		    

		// Check if event should be run right away or put into queue
		if(eventArray[seqNum]!= null){		    
		    System.out.println("De-queue event.");
		    packetFromServer = eventArray[seqNum];
		    packet_type = packetFromServer.packet_type;	
		    eventArray[seqNum] = null;
		} else if(packetFromServer.sequence_num == seqNum){    
		    System.out.println("Run event right away.");
		    packet_type = packetFromServer.packet_type;
		} else {   
		    System.out.println("Store event into queue.");
		    eventArray[seqNum] = packetFromServer;
		    continue;
		}

                switch (packet_type) {
                    case MazePacket.CLIENT_REGISTER:
                        addClientEvent();
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
                    default:
                        System.out.println("Could not recognize packet type");
			break;
                }

		seqNum++;
		if(seqNum == 21)
		    seqNum = 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clientRespawn(){
        System.out.println("Respawning client");
        String name = packetFromServer.tc;

        if (clientTable.containsKey(name)) { 
            Client tc = clientTable.get(name);
	    Client sc = clientTable.get(packetFromServer.sc);
	    Point p = packetFromServer.client_location;
	    Direction d = packetFromServer.client_direction;

	    maze.setClient(sc, tc, p,d);
        } else {
            System.out.println("CLIENT: no client named " + name + " in backup");
        }
    }

    /**
     * Process server packet eventsi
     * */
    private void addClientEvent() {
        String name = packetFromServer.client_name;
        ConcurrentHashMap<String, ClientData> clientTableFromServer = packetFromServer.client_list;
        System.out.println("CLIENT: Server sent addClient event");

        if (name.equals(me.getName())) {
            System.out.println("CLIENT: Server added me!");
        }
        else {
            System.out.println("CLIENT: Server adding new client " + name);
            int clientType = packetFromServer.client_type;

            switch (clientType) {
                case ClientData.REMOTE:
                    //add remote client
                    RemoteClient c = new RemoteClient(name);
                    clientTable.put(name, c);
                    maze.addRemoteClient(c, packetFromServer.client_location, packetFromServer.client_direction);
                    break;
                case ClientData.ROBOT:
                    //add robot client
                    break;
                default:
                    System.out.println("CLIENT: no new clients on add client event");
                    break;
            }
        }
	
	seqNum = packetFromServer.sequence_num;

        // else server is telling you to add a new client
        // create new clients into clientTable based on any
        // new clients seen in clientTableFromServer
        for (Map.Entry<String, ClientData> entry : clientTableFromServer.entrySet()) {
            String key = entry.getKey();
            System.out.println(key);
            if (!clientTable.containsKey(key)) {
                ClientData cData = entry.getValue();
                
                switch (cData.client_type) {
                    case ClientData.REMOTE:
                        //add remote client
                        RemoteClient c = new RemoteClient(key);
                        clientTable.put(key, c);
                        maze.addRemoteClient(c, cData.client_location, cData.client_direction);
                        break;
                    case ClientData.ROBOT:
                        //add robot client
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void clientForwardEvent() {
        // get client with name from client list
        // client.foward
        String name = packetFromServer.client_name;

        if (clientTable.containsKey(name)) { 
            clientTable.get(name).forward();
        } else {
            System.out.println("CLIENT: no client named " + name + " in forward");
        }
    }

    private void clientBackEvent() {
        // get client with name from client list
        // client.foward
        String name = packetFromServer.client_name;

        if (clientTable.containsKey(name)) { 
            clientTable.get(name).backup();
        } else {
            System.out.println("CLIENT: no client named " + name + " in backup");
        }
    }

    private void clientLeftEvent() {
        // get client with name from client list
        // client.foward
        String name = packetFromServer.client_name;

        if (clientTable.containsKey(name)) { 
            clientTable.get(name).turnLeft();
        } else {
            System.out.println("CLIENT: no client named " + name + " in left");
        }
    }

    private void clientRightEvent() {
        // get client with name from client list
        // client.foward
        String name = packetFromServer.client_name;

        if (clientTable.containsKey(name)) { 
            clientTable.get(name).turnRight();
        } else {
            System.out.println("CLIENT: no client named " + name + " in right");
        }
    }


    private void clientFireEvent() {
        // get client with name from client list
        // client.foward
        String name = packetFromServer.client_name;

        if (clientTable.containsKey(name)) { 
            clientTable.get(name).fire();

	    // Decrement score.
	    //scoreTable.clientFired(clientTable.get(name));

        } else {
            System.out.println("CLIENT: no client named " + name + " in fire");
        }
    }

    /**
     * Listen for client keypress and send server packets 
     * */
    public void handleKeyPress(KeyEvent e) {
        // If the user pressed Q, invoke the cleanup code and quit. 
        if((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
            //Mazewar.quit();
            // Up-arrow moves forward.
        } else if(e.getKeyCode() == KeyEvent.VK_UP) {
            sendPacketToServer(MazePacket.CLIENT_FORWARD);
            // Down-arrow moves backward.
        } else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
            sendPacketToServer(MazePacket.CLIENT_BACK);
            //backup();
            // Left-arrow turns left.
        } else if(e.getKeyCode() == KeyEvent.VK_LEFT) {
            sendPacketToServer(MazePacket.CLIENT_LEFT);
            //turnLeft();
            // Right-arrow turns right.
        } else if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
            sendPacketToServer(MazePacket.CLIENT_RIGHT);
            //turnRight();
            // Spacebar fires.
        } else if(e.getKeyCode() == KeyEvent.VK_SPACE) {
            sendPacketToServer(MazePacket.CLIENT_FIRE);
            //fire();
        }
    }

    private void sendPacketToServer(int packetType) {
        try {
            MazePacket packetToServer = new MazePacket();
            packetToServer.packet_type = packetType;
            packetToServer.client_name = me.getName();
            out.writeObject(packetToServer);
	    //Wait... Else If another remote client is in front of you, it will glitch!
	    Thread.sleep(200);
	
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Try and reserve a point!
    public boolean reservePoint(Point point){
       MazePacket packetToServer = new MazePacket();

        try{
            packetToServer.packet_type = MazePacket.RESERVE_POINT;
            packetToServer.client_name = me.getName();
            packetToServer.client_location = point;
            packetToServer.client_direction = null;
            packetToServer.client_type = MazePacket.REMOTE;
            System.out.println("CLIENT " + me.getName() + " RESERVING POINT");
            out.writeObject(packetToServer);

	    packetFromServer = new MazePacket();
	    packetFromServer = (MazePacket) in.readObject();

	    int error_code = packetFromServer.error_code;

	    if(error_code == 0)
		return true;
	    else
	    	return false;
	 

        }catch (Exception e){
            e.printStackTrace();
            System.out.println("ERROR: reserving point");
	    return false;
        }

    }

    public boolean clientIsMe(Client c){
	if(c == me)
	    return true;
	else
	    return false;
    }


    public void sendClientRespawn(String sc, String tc, Point p, Direction d) {
        try {
            MazePacket packetToServer = new MazePacket();
            packetToServer.packet_type = MazePacket.CLIENT_RESPAWN;
            packetToServer.sc = sc;
	    packetToServer.tc = tc;
	    packetToServer.client_location = p;
	    packetToServer.client_direction = d;
            out.writeObject(packetToServer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}


