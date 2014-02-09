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
            //this.cin = new ObjectInputStream(rcSocket.getInputStream());
            this.data = sdata;
            data.addSocketOutToList(cout);
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
            while ((packetFromRC = (MazePacket) cin.readObject()) != null) {
                System.out.println("S_HANDLER: packet type is " + packetFromRC.packet_type);

                /* Process each packet */
                switch (packetFromRC.packet_type) {
                    case MazePacket.CLIENT_REGISTER:
                        registerClientEvent();
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
                    default:
                        System.out.println("S_HANDLER: Could not recognize packet type");
                }
                
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }

    private void clientForwardEvent() {
        try { 
            MazePacket eventPacket = new MazePacket();
            String rc_name = packetFromRC.client_name;
            System.out.println("S_HANDLER: " + rc_name + " forward");

            eventPacket.client_name = rc_name;
            eventPacket.packet_type = MazePacket.CLIENT_FORWARD;

            data.addEventToQueue(eventPacket);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }


    private void clientBackEvent() {
        try { 
            MazePacket eventPacket = new MazePacket();
            String rc_name = packetFromRC.client_name;
            System.out.println("S_HANDLER: " + rc_name + " back");

            eventPacket.client_name = rc_name;
            eventPacket.packet_type = MazePacket.CLIENT_BACK;

            data.addEventToQueue(eventPacket);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }

    private void clientLeftEvent() {
        try { 
            MazePacket eventPacket = new MazePacket();
            String rc_name = packetFromRC.client_name;
            System.out.println("S_HANDLER: " + rc_name + " left");

            eventPacket.client_name = rc_name;
            eventPacket.packet_type = MazePacket.CLIENT_LEFT;

            data.addEventToQueue(eventPacket);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }

    private void clientRightEvent() {
        try { 
            MazePacket eventPacket = new MazePacket();
            String rc_name = packetFromRC.client_name;
            System.out.println("S_HANDLER: " + rc_name + " right");

            eventPacket.client_name = rc_name;
            eventPacket.packet_type = MazePacket.CLIENT_RIGHT;

            data.addEventToQueue(eventPacket);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }

    private void clientFireEvent() {
        try { 
            MazePacket eventPacket = new MazePacket();
            String rc_name = packetFromRC.client_name;
            System.out.println("S_HANDLER: " + rc_name + " fire");

            eventPacket.client_name = rc_name;
            eventPacket.packet_type = MazePacket.CLIENT_FIRE;

            data.addEventToQueue(eventPacket);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }

    /* Register new client
     * - add to client list
     * - send client list 
     */
    private void registerClientEvent() {
        try {
            /* Wait for handshaking packet from client, store client state in 
             * global client table */
            MazePacket eventPacket = new MazePacket();

            /* Add to client list */
            String rc_name = packetFromRC.client_name;
            Point rc_point = packetFromRC.client_location;
            Direction rc_direction = packetFromRC.client_direction;
            System.out.println("S_HANDLER: Connected with " + rc_name );
            data.addClientToTable(rc_name, rc_point, rc_direction, ClientData.REMOTE);

            /* Prepare event packet for event queue */
            eventPacket.client_name = rc_name;
            eventPacket.client_location = rc_point;
            eventPacket.packet_type = MazePacket.CLIENT_REGISTER;
            eventPacket.client_list = data.clientTable; //Add client list to event packet

            /* Add packet to event queue */
            data.addEventToQueue(eventPacket);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("server done broke");
        }
    }
}
