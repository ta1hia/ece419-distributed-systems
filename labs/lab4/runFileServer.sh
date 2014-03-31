JOBTRACKER=unhasher/src
LIBRARY_ZK=unhasher/lib/zookeeper-3.3.2.jar
LIBRARY_LOG=unhasher/lib/log4j-1.2.15.jar

echo -n "Enter hostname:port of Zookeeer: "
read zk
echo -n "Enter port to listen into: "
read port

java -classpath $LIBRARY_ZK:$LIBRARY_LOG:$JOBTRACKER unhasher.FileServer $zk $port

