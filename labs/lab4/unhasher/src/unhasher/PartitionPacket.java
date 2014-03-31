package unhasher;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings("serial")
    public class PartitionPacket implements Serializable {

	// Packet Type
	public static final int PARTITION_REQUEST = 100;
	public static final int PARTITION_REPLY = 101;

	public String w_id;
	public int numWorkers;
	public int packet_type;
	public List dictionary;
	
	public PartitionPacket (int type, String id, int numWorkers) {
	    this.w_id = id;
	    this.numWorkers = numWorkers;

	    this.packet_type = type;
	}
    }
