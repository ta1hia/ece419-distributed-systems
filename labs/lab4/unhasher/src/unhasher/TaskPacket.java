package unhasher;

import java.io.Serializable;

@SuppressWarnings("serial")
public class TaskPacket implements Serializable {

	// Packet Type
	public static Integer TASK_SUBMIT =		100;
	public static Integer TASK_QUERY =		101;

	public static Integer packet_type;
	public static String hash;

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
		if (packet_type != null && hash != null) {
			me = String.format("%d:%s", packet_type, hash);
		}
		return me;
	}
	
}
