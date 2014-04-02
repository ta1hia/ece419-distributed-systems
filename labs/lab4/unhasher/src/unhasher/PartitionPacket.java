package unhasher;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings("serial")
    public class PartitionPacket implements Serializable {

	// Packet Type
	public static final int PARTITION_REQUEST = 100;
	public static final int PARTITION_REPLY = 101;

	public int partition_id;
	public int numWorkers;
	public int packet_type;
	public List dictionary;

	// Index for the dictionary
	public int i;
	public int end;
	
	public PartitionPacket (int type, int id, int numWorkers) {
	    this.partition_id = id;
	    this.numWorkers = numWorkers;

	    this.packet_type = type;
	}


	public PartitionPacket (int type) {
	    this.packet_type = type;
	}
    }
