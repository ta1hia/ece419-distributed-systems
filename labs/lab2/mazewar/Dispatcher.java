
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.LinkedBlockingQueue;

// /* Dispatcher class
//  * Dispatches messages from event queue and broadcasts
//  * events to all remote clients.
//  *
//  */
// public class Dispatcher extends Thread {
//     ConcurrentHashMap <String,String> clientTable;
//     LinkedBlockingQueue eventQueue;

//     // Continually check eventqueue
//     // Broadcast whenever queue is not empty
//     public void run(){
// 	String new_event = null;
// 	int seqNum = 0;

// 	while(true){ //???
// 	    if(new_event = eventQueue.peek() != null){
// 		// Go through each client	    
// 		for(int i=0;i < clientTable.size()){
// 		    clientTable.get(i).send(seqNum, command);
// 		}
// 		seqNum++;
// 		if(seqNum == 501)
// 		    seqNum = 0;
// 	    }
// 	}

//     }

//     public static void setTable (ConcurrentHashMap <String,String> clientTable, LinkedBlockingQueue eventQueue){
// 	Dispatcher.eventQueue = eventQueue;
// 	Dispatcher.clientTable = clientTable;
//     }

// }

