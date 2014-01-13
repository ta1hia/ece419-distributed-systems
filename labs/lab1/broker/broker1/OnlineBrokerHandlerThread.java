import java.net.*;
import java.io.*; 
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineBrokerHandlerThread extends Thread {
    private Socket socket = null;
    private static ConcurrentHashMap<String, Long> nasdaq; /* thread-safe hashmap structure */

    public OnlineBrokerHandlerThread(Socket socket) {
        super("OnlineBrokerHandlerThread");
        this.socket = socket;
        System.out.println("Created new Thread to handle broker client");
    }


    /* Accessors */
    public static void setNasdaq (ConcurrentHashMap <String, Long> quotes) {
        OnlineBrokerHandlerThread.nasdaq = quotes;
    }


    

}
