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
    ObjectOutputStream out;
    ObjectInputStream in;
    BlockingQueue<MazeEvent> eventQueue;
    ConcurrentHashMap<String, Point> clientTable;

    public ClientHandlerThread(String host, int port){
        /* Connect to central game server. */
        try {
            /* Using this hardcoded port for now, eventually make this userinput at GUI interface in Mazewar.java*/
            System.out.println("Connecting to Mazewar Server...");

            cSocket = new Socket(host,port);
            out = new ObjectOutputStream(cSocket.getOutputStream());
            in = new ObjectInputStream(cSocket.getInputStream());

            registerClient();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    private void registerClient(){
        MazePacket packetToServer = new MazePacket();
        MazePacket packetFromServer = new MazePacket();

        try{
            /* Initialize handshaking with server */
            Random rand = new Random();

            packetToServer.packet_type = MazePacket.CLIENT_REGISTER;
            packetToServer.sequence_num = rand.nextInt(1000) + 1; /* Where to store ? should this even be in Maze.java? (not user data) */
            packetToServer.client_name = "Kanye";        /* Using a hardcoded value for now - add to GUI interface eventually */
            out.writeObject(packetToServer);

            /* Wait for server acknowledgement */
            packetFromServer = (MazePacket) in.readObject();
            if (packetFromServer == null || 
                    packetFromServer.client_list == null ||
                    //packetFromServer.event_list == null ||
                    packetFromServer.packet_type != MazePacket.SERVER_ACK) {
                System.out.println("Client error registering with server");
                    }

            clientTable = packetFromServer.client_list;
            eventQueue = packetFromServer.event_list;

            System.out.println("Server verified connection!");

        }catch (Exception e){
            System.out.println("Client error registering with server");

        }

    }


}


