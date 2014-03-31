package unhasher;

import java.io.Serializable;

@SuppressWarnings("serial")
public class TaskPacket implements Serializable {

	// Packet Type
	public static final int TASK_SUBMIT =	100;
	public static final int TASK_QUERY 	=	101;

	public String c_id;
	public int packet_type;
	public String hash;
	
	public TaskPacket (String id, Integer type, String hash) {
		this.c_id = id;
		this.packet_type = type;
		this.hash = hash;
	}
	
	public TaskPacket(String s) {
		if (s != null) {
			this.c_id = s.split(":")[0];
			this.packet_type = Integer.parseInt(s.split(":")[1]);
			this.hash = s.split(":")[2];
		}
	}
	
	public String taskToString() {
		String me = null;
		if (packet_type != 0 && hash != null) {
			me = String.format("%s:%d:%s", c_id,packet_type, hash);
		}
		return me;
	}
	
	
	
}
