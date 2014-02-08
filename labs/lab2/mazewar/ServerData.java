// import java.io.*;
// import java.net.*;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.LinkedBlockingQueue;
// import java.util.concurrent.BlockingQueue;

// public class ServerData implements Serializable {
//     //eventqueue
//     //clientqueue
//     BlockingQueue<MazePacket> eventQueue = new LinkedBlockingQueue();
//     ConcurrentHashMap<String, Point> clientTable = new ConcurrentHashMap<>(); //Might need reference to actual thread here, for dispatcher

//     public void addClientToTable(String name, Point position) {
//         if (!clientTable.containsKey(name)) {
//             clientTable.put(name, position);
//         } else {
//             System.out.println("Client with name " + name + " already exists.");
//         }

//     }

//     public void addEventToQueue(String command){
// 	eventQueue.offer(command);
//     }
// }
