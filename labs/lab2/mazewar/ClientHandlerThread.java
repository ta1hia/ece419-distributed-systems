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
    ConcurrentHashMap<String, ClientData> clientTable;


    MazePacket packetFromServer;

    public ClientHandlerThread(String host, int port){
        /* Connect to central game server. */
        try {
            /* Using this hardcoded port for now, eventually make this userinput at GUI interface in Mazewar.java*/
            System.out.println("Connecting to Mazewar Server...");

            cSocket = new Socket(host,port);
            out = new ObjectOutputStream(cSocket.getOutputStream());
            in = new ObjectInputStream(cSocket.getInputStream());

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
            System.out.println("CLIENT: Registering 1");
            /* Initialize handshaking with server */
            Random rand = new Random();

            packetToServer.packet_type = MazePacket.CLIENT_REGISTER;
            packetToServer.client_name = me.getName();
            //packetToServer.client_location = maze.getClientPoint(me);
            out.writeObject(packetToServer);
            System.out.println("CLIENT: Registering 2");

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
                        addClient();
                        break;
                    default:
                        System.out.println("Could not recognize packet type");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Process server packet events */

    private void addClient() {
        String name = packetFromServer.client_name;

        if (name == me.getName()) {
            System.out.println("Server added me!");
            return;
        }

        // else server is telling you to add a new client
        // which we won't implement till lab 3
    }

    /* Listen for client keypress and send server packets */
    public void handleKeyPress(KeyEvent e) {
        // If the user pressed Q, invoke the cleanup code and quit. 
        if((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
            //Mazewar.quit();
            // Up-arrow moves forward.
        } else if(e.getKeyCode() == KeyEvent.VK_UP) {
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
        MazePacket packetToServer = new MazePacket();
        packetToServer.packet_type = packetType;
        packetToServer.client_name = me.getName();
    }
}


