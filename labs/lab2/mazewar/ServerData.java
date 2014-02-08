import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ServerData implements Serializable {
    //eventqueue
    //clientqueue
    BlockingQueue<MazePacket> eventQueue = new LinkedBlockingQueue();
    ConcurrentHashMap<String, ClientData> clientTable = new ConcurrentHashMap<>(); //Might need reference to actual thread here, for dispatcher

    public void addClientToTable(String name, Point position, ObjectOutputStream out) {
        if (!clientTable.containsKey(name)) {
            /* Create ClientData */
            ClientData clientData = new ClientData();
            clientData.client_location = position;
            clientData.csocket_out = out;
            /* Add to table */
            clientTable.put(name, clientData);
        } else {
            System.out.println("Client with name " + name + " already exists.");
        }
    }

    public void addEventToQueue(MazePacket event){
        eventQueue.offer(event);
    }

    
}
