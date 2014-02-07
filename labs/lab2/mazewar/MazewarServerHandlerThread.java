import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

/* MazewarServerHandlerThread class
 *
 * MazewarServer spawns this thread for each remote
 * client that connects
 */

public class MazewarServerHandlerThread extends Thread {
    Socket rc_socket = null;
    ServerData data = null;
    int cid = -1;
    int lastSeqNum;

    DataInputStream cin;
    DataOutputStream cout;

    public MazewarServerHandlerThread (Socket socket, ServerData sdata, int clientID) {
        super("MazewarServerHandlerThread");
        rc_socket = socket;
        cin = new DataInputStream(rc_socket.getInputStream());
        cout = new DataOutputStream(rc_socket.getOutputStream());
        cid = clientID;
        data = sdata;
        System.out.println("Created new MazewarServerHandlerThread to handle remote client ");
    }

    public void run() {
        System.out.println("Connecting to client " + cid);
        MazePacket packet_from_rc = new MazePacket();
        MazePacket packet_to_rc = new MazePacket();

        try {
            /* Wait for handshaking packet from client, store client state in 
             * global client table */
            while ((packet_from_rc = cin.readObject()) != null) {
                switch (packet_from_rc.packet_type) {
                    case MazePacket.CLIENT_REGISTER:
                        //client_register();
                        break;
                    default:
                        System.out.println("Could not recognize packet type");
                }
            }




            /* Send game state to client */

            /* Loop: 
             * Wait for client event
             * Add event to global event queue
             */
        } catch (Exception e) {
            System.out.println("server done broke");
        }
    }

}
