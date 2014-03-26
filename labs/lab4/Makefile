build:
	javac -classpath unhasher/lib/zookeeper-3.3.2.jar:unhasher/lib/log4j-1.2.15.jar unhasher/src/unhasher/JobTracker.java unhasher/src/unhasher/JobTrackerHandler.java unhasher/src/unhasher/TaskPacket.java
runJobTracker:
	java -classpath unhasher/lib/zookeeper-3.3.2.jar:unhasher/lib/log4j-1.2.15.jar:. JobTracker
# runB:
# 	java -classpath unhasher/lib/zookeeper-3.3.2.jar:unhasher/lib/log4j-1.2.15.jar:. B
clean:
	rm -f unhasher/src/unhasher/*.class
