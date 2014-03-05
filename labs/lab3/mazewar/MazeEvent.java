import java.io.Serializable;

public class MazeEvent implements Serializable {
    public String client_host;
    public int client_port;

    public MazeEvent(String host, int port){
        this.client_host = host;
        this.client_port = port;
    }
}
