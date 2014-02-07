import java.io.Serializable;

import java.io.*;
import java.net.*;

/* MazePacket

   Contains basic variables that client and server shall use to communicate with each other.
*/

// Contains an event that the client/robot shall take.
class MazeEvent implements Serializable {
    public String client_host;
    public int client_port;

    public MazeEvent(String host, int port){
	this.client_host = host;
	this.client_port = port;
    }
}

public class MazePacket implements Serializable {
    // Actions
    public static final int SERVER_ACK = 100; 
    public static final int CLIENT_REGISTER = 101; // Client wants to register! IP of client shall be passed in.

    // Error code
    public static final int ERROR_INVALID_ARG = -101;

    // Client actions
    public String client_host;
    public String client_name;

    public int error_code;
    
    //Server actions
    int ack_num;
    int remote_client_id; /* For now I'm making this a server-assigned client id instead (instead of client sending their own IP)*/

    // Event
    // Contains an event that will occur    
    public MazeEvent event;

    // Packet data
    int sequence_num;
    int packet_type;

}
