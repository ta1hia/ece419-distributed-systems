import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.io.Serializable;

public class ServerData implements Serializable {
    //eventqueue
    //clientqueue
    BlockingQueue<MazePacket> eventQueue = new LinkedBlockingQueue();
    ConcurrentHashMap<String, ClientData> clientTable = new ConcurrentHashMap(); //Might need reference to actual thread here, for dispatcher
    ArrayList socketOutList = new ArrayList();

    public void addClientToTable(String name, Point position, Direction direction, int type) {
        if (!clientTable.containsKey(name)) {
            /* Create ClientData */
            ClientData clientData = new ClientData();
            clientData.client_location = position;
            clientData.client_direction = direction;
            clientData.client_type = type;

            /* Add to table */
            clientTable.put(name, clientData);

        } else {
            System.out.println("Client with name " + name + " already exists.");
        }
    }

    public void addEventToQueue(MazePacket event){
        eventQueue.offer(event);
    }

    public void addSocketOutToList(ObjectOutputStream out) {
        socketOutList.add(out);
    }


}
