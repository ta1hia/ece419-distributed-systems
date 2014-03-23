build:
	javac -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar ZkConnector.java A.java B.java
runA:
	java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. A
runB:
	java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. B
clean:
	rm -f *.class
