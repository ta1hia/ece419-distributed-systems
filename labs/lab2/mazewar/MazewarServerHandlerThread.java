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
        packetToRC = new MazePacket();
        packetFromRC = new MazePacket();

        try {
            /* Loop: 
            */
            while ((packetFromRC = (MazePacket) cin.readObject()) != null) {
                switch (packetFromRC.packet_type) {
                    case MazePacket.CLIENT_REGISTER:
                        registerClient();
                        break;
                    default:
                        System.out.println("Could not recognize packet type");
                }
            }
        } catch (Exception e) {
            System.out.println("server done broke");
        }
    }

    /* Register new client
     * - add to client list
     * - send client list 
     */
    private void registerClient() {
        try {
            /* Wait for handshaking packet from client, store client state in 
             * global client table */

            /* Add to client list */
            String rc_name = packetFromRC.client_name;
            Point rc_point = packetFromRC.client_location;
            System.out.println("Connected with " + rc_name);
            data.addClientToTable(rc_name, rc_point);

            /* Send game state to client */
            packetToRC.client_list = data.clientTable;
            packetToRC.packet_type = MazePacket.SERVER_ACK;
            packetToRC.ack_num = packetFromRC.sequence_num;
            cout.writeObject(packetToRC);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }

    }
}
