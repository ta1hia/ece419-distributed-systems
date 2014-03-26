JOBTRACKER=unhasher/src/unhasher/

echo -n "Enter port of JobTracker: "
read jt_port
echo -n "Enter hosename of Zookeeper: "
read zk_hostname
echo -n "Enter port of Zookeper: "
read zk_port

java JOBTRACKER/JobTracker $jt_port $zk_hostname $zk_port

