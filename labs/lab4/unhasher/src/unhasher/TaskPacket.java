package unhasher;

import java.io.Serializable;

@SuppressWarnings("serial")
public class TaskPacket implements Serializable {

	// Packet Type
	public static final int TASK_SUBMIT =	100;
	public static final int TASK_QUERY 	=	101;

	public int packet_type;
	public String hash;
	
	public TaskPacket (Integer type, String hash) {
		this.packet_type = type;
		this.hash = hash;
	}
	
	public TaskPacket(String s) {
		if (s != null) {
			this.packet_type = Integer.parseInt(s.split(":")[0]);
			this.hash = s.split(":")[1];
		}
		
	}
	
	public String taskToString() {
		String me = null;
		if (packet_type != 0 && hash != null) {
			me = String.format("%d:%s", packet_type, hash);
		}
		return me;
	}
	
}
