import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
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

    public MazewarServerHandlerThread (Socket socket, ServerData sdata) throws IOException {
        super("MazewarServerHandlerThread");
        try {
        this.rcSocket = socket;
        this.cout = new ObjectOutputStream(rcSocket.getOutputStream());
        this.cin = new ObjectInputStream(rcSocket.getInputStream());
        this.data = sdata;
        System.out.println("Created new MazewarServerHandlerThread to handle remote client ");
        } catch (IOException e) {
            System.out.println("IO Exception");
        }
    }

    public void run() {
        System.out.println("Connecting to client...");
        int lastPacketNum;
        MazePacket packetFromRC = new MazePacket();
        MazePacket packetToRC = new MazePacket();

        try {
            /* Wait for handshaking packet from client, store client state in 
             * global client table */
            packetFromRC = (MazePacket) cin.readObject();
            
            if (packetFromRC == null) {
                System.out.print("Error connecting client");
                return;
            }

            /* Add to client list and give it a random start position */
            String rc_name = packetFromRC.client_name;
            System.out.println("Connected with " + rc_name);


            

            /* Send game state to client */
            // need to send  list of players/locations

            packetToRC.event_list = data.eventQueue;
            packetToRC.client_list = data.clientTable;
            packetToRC.packet_type = MazePacket.SERVER_ACK;
            packetToRC.ack_num = packetFromRC.sequence_num;
            cout.writeObject(packetToRC);

            /* Loop: 
             */
            while ((packetFromRC = (MazePacket) cin.readObject()) != null) {
                switch (packetFromRC.packet_type) {
                    default:
                        System.out.println("Could not recognize packet type");
                }
            }
        } catch (Exception e) {
            System.out.println("server done broke");
        }
    }

}
