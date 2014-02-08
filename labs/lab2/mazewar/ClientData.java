import java.io.*;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;

/* ClientData
 * wrapper for clientTable entries
 * each clientTable key holds maps to ClientData 
 */
public class ClientData implements Serializable {
    Point client_location;
    ObjectOutputStream csocket_out;
}
