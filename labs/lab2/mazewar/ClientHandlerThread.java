import java.awt.event.KeyEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
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

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void registerMaze(Maze maze) {
        this.maze = maze;
    }

    public void registerClientWithMazewar(){
        MazePacket packetToServer = new MazePacket();

        try{
            System.out.println("CLIENT: Registering ");

            /* Initialize handshaking with server */
            Random rand = new Random();

            packetToServer.packet_type = MazePacket.CLIENT_REGISTER;
            packetToServer.client_name = me.getName();
            //packetToServer.client_location = maze.getClientPoint(me);
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
            while((packetFromServer = (MazePacket) in.readObject()) != null) {


                switch (packetFromServer.packet_type) {
                    case MazePacket.CLIENT_REGISTER:
                        addClientEvent();
                        break;
                    case MazePacket.CLIENT_FORWARD:
                        clientForwardEvent();
                        break;
                    default:
                        System.out.println("Could not recognize packet type");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Process server packet eventsi
     * */
    private void addClientEvent() {
        String name = packetFromServer.client_name;
        ConcurrentHashMap<String, ClientData> clientTableFromServer = packetFromServer.client_list;

        if (name.equals(me.getName())) {
            System.out.println("Server added me!");
            return;
        }

        // else server is telling you to add a new client
        // create new clients into clientTable based on any
        // new clients seen in clientTableFromServer
        // which we won't implement till lab 3
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

    /**
     * Listen for client keypress and send server packets 
     * */
    public void handleKeyPress(KeyEvent e) {
        // If the user pressed Q, invoke the cleanup code and quit. 
        if((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
            //Mazewar.quit();
            // Up-arrow moves forward.
        } else if(e.getKeyCode() == KeyEvent.VK_UP) {
            System.out.println("CLIENT: sending event forward");
            sendPacketToServer(MazePacket.CLIENT_FORWARD);
            //forward();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


