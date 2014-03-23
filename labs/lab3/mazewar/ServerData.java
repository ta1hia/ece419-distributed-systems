import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.io.Serializable;
import java.util.concurrent.Semaphore;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerData implements Serializable {

    /**
     * Game state 
     */
    BlockingQueue<MazePacket> eventQueue = new LinkedBlockingQueue();
    ConcurrentHashMap<String, ClientData> clientTable = new ConcurrentHashMap();  // TODO: get rid of this old stuff
    ConcurrentHashMap<Integer, ObjectOutputStream> socketOutList = new ConcurrentHashMap(); 
    ConcurrentHashMap<Integer, ClientData> lookupTable = new ConcurrentHashMap(); // Contains all client data

    /**
     * Client Data
     */
    int myId;
    ConcurrentHashMap<String, Point> clientPosition = new ConcurrentHashMap();

    /**
     * Distributed synchronization resources
     */
    int eventIndex = 0;     // Global lamport clock
    int lamportClock;       // This client's lamport clock
    Semaphore sem = new Semaphore(0); 

    Lock l = new ReentrantLock();


    // public MazePacket getNextEvent() {
    //     // remove next event and return
    //     MazePacket p = new MazePacket();
    //     int i = -1;
    //     while (p == null && i < 20) {
    //         i++;
    //         p = eventArray[i];
    //     }
    //     eventArray[i] = null;
    //     return p;
    // }


    public void setId(int num){
        myId = num;
    }

    public int getId(){
        return myId; 
    }

    public void acquireSemaphore(int num){
	System.out.println("The amount in sem is " + sem.availablePermits() +  ". Amount required is " + num); 
        try{
            sem.acquire(num);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public void releaseSemaphore(int num){
	System.out.println("Releasing " + num + " semaphore(s)");

        try{
            sem.release(num);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void addLookupTable(ConcurrentHashMap lookupTable){
        this.lookupTable = lookupTable;
    }

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

    public void removeClientFromTable(String name){
        if(clientTable.containsKey(name)){
            clientTable.remove(name);
            clientPosition.remove(name);
        }
    }

    public void addEventToQueue(MazePacket event){
        eventQueue.offer(event);
    }

    public void addSocketOutToList(Integer key, ObjectOutputStream out) {
        socketOutList.put(key, out);
    }

    public void removeSocketOutFromList(Integer key) {
        socketOutList.remove(key);
    }

    public boolean setPosition(String name, Point position){
        if(!clientPosition.containsValue(position)){
            clientPosition.put(name,position);	  	    
            return true;
        }else{
            Point clientPos = clientPosition.get(name);
            if(clientPos == position)
                return true;
            else
                return false;
        }
    }

    public void setLamportClock(int value){
	l.lock();
        lamportClock = value;
	l.unlock();
    }

    public Integer incrementLamportClock() {
	l.lock();
	lamportClock++;
	if(lamportClock == 20)
	    lamportClock = 0;
	l.unlock();
        return lamportClock;
    }

    public Integer getLamportClock(){
	return lamportClock;
    }

    public Integer getEventIndex(){
	return eventIndex;
    }

    public void setEventIndex(Integer i){
	l.lock();
	eventIndex = i;
	l.unlock();
    }


    public void setClockAndIndex(int value){
	l.lock();
        lamportClock = value;
	eventIndex = value;
	l.unlock();
    }

}
