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

    /**
     * Client sends packet with name, position (?)
     * Server adds to event queue with client list included
     */
    public static final int CLIENT_REGISTER = 201; // Client wants to register! IP of client shall be passed in.
    public static final int CLIENT_FORWARD = 202;
    public static final int CLIENT_BACK = 203;
    public static final int CLIENT_LEFT = 204;
    public static final int CLIENT_RIGHT = 205;
    public static final int CLIENT_FIRE = 206;
    public static final int CLIENT_RESPAWN = 207;
    public static final int CLIENT_QUIT = 208;
    public static final int CLIENT_ACK = 209;
    public static final int CLIENT_CLOCK = 210;
    public static final int CLIENT_SPAWN = 211;
    public static final int CLIENT_REL_SEM = 212;

    // Lookup service
    public static final int LOOKUP_REPLY = 300;
    public static final int LOOKUP_REGISTER = 301;
    public static final int LOOKUP_QUIT = 302;
    public static final int LOOKUP_UPDATE = 303;
    public static final int LOOKUP_GET = 304;

    // Misc.
    public static final int RESERVE_POINT = 401;
    public static final int GET_SEQ_NUM = 402;

    // Robot 
    public static final int ROBOT_REGISTER = 501; // Client wants to register! IP of client shall be passed in.
    public static final int ROBOT_FORWARD = 502;
    public static final int ROBOT_BACK = 503;
    public static final int ROBOT_LEFT = 504;
    public static final int ROBOT_RIGHT = 505;
    public static final int ROBOT_FIRE = 506;
    public static final int ROBOT_RESPAWN = 507;
    public static final int ROBOT_QUIT = 508;
    public static final int ROBOT_SPAWN = 509;

    // Error code
    public static final int ERROR_INVALID_ARG = -101;
    public static final int ERROR_RESERVED_POSITION = -102;

    // Client type
    public static final int ROBOT = 1;
    public static final int REMOTE = 2;

    // Client actions
    public String client_host;
    public int client_port;

    public int client_id;
    public String client_name;
    public Point client_location;
    public Direction client_direction;
    public int client_type;
    public int client_score; 

    public boolean for_new_client = false;
    public boolean is_robot_controller = false;

    // Client shot
    public Integer shooter; // Source / killer
    public Integer target; // Targer / victim

    //Server actions
    int ack_num;

    // Event
    public MazeEvent event;

    // Game data
    // Contains all client information within Client data
    ConcurrentHashMap<String, ClientData> client_list;

    ConcurrentHashMap<Integer, ClientData> lookupTable;
    ConcurrentHashMap<Integer, ClientData> robotTable;

    // Packet data
    int sequence_num;
    int packet_type;
    public int error_code;

    // Lamport clock
    int lamportClock;
    boolean isValidClock;   
}
