import java.io.Serializable;

/* MazePacket

   Contains basic variables that client and server shall use to communicate with each other.
*/

// Contains an event that the client/robot shall take.
class MazeEvent implements Serializable {
    public String client_host;
    public String client_port;
    public String client_command;

    public MazeEvent(String host, Integer port, MazePacket.client_command){
	this.client_host = host;
	this.client_port = port;
	this.client_command = client_command;
    }
}

public class MazePacket implements Serializable {
    // Error code
    public static final int MAZE_NULL = 0;

    // Client actions
    public int client_command;

    public int error_code;

    // Event
    // Contains an event that will occur    
    public MazeEvent event;

}
