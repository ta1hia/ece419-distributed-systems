CLIENTDRIVER=unhasher/src
LIBRARY_ZK=unhasher/lib/zookeeper-3.3.2.jar
LIBRARY_LOG=unhasher/lib/log4j-1.2.15.jar

echo -n "Enter hosename of Zookeeper: "
read zk_hostname
echo -n "Enter port of Zookeper: "
read zk_port

java -classpath $LIBRARY_ZK:$LIBRARY_LOG:$CLIENTDRIVER unhasher.ClientDriver $zk_hostname $zk_port

