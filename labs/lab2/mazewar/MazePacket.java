import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import java.io.*;
import java.net.*;

/* MazePacket

   Contains basic variables that client and server shall use to communicate with each other.
   */


public class MazePacket implements Serializable {
    // Actions
    public static final int SERVER_ACK = 100; 
    public static final int SERVER_CLIENT_LIST = 101;
    public static final int SERVER_EVENT_LIST = 102;

    public static final int CLIENT_REGISTER = 201; // Client wants to register! IP of client shall be passed in.

    // Error code
    public static final int ERROR_INVALID_ARG = -101;

    // Client actions
    public String client_host;
    public String client_name;
    public Point client_location;

    //Server actions
    int ack_num;

    // Event
    public MazeEvent event;

    // Game data
    ConcurrentHashMap<String, Point> client_list;

    // Packet data
    int sequence_num;
    int packet_type;
    public int error_code;

}
