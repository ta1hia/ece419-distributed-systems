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

    DataInputStream cin;
    DataOutputStream cout;

    public MazewarServerHandlerThread (Socket socket) {
        super("MazewarServerHandlerThread");
        rc_socket = socket;
        cin = new DataInputStream(rc_socket.getInputStream());
        cout = new DataOutputStream(rc_socket.getOutputStream());
        System.out.println("Created new MazewarServerHandlerThread to handle remote client ");
    }

    public void run() {
        /* Wait for handshaking packet from client, store client state in 
         * global client table */


        /* Send game state to client */

        /* Loop: 
         * Wait for client event
         * Add event to global event queue
         */
    }

}
