#!/bin/bash
# Run Zookeper
MYZK=myzk
# JAVA_HOME=${MYZK}/java/jdk1.6.0/

echo -n "Enter port of JobTracker: "
read jt_port
echo -n "Enter hosename of Zookeeper: "
read zk_hostname
echo -n "Enter port of Zookeper: "
read zk_port

${MYZK}/bin/zkCli.sh -server $hostname:$port


# #!/bin/bash
# # server.sh
# ECE419_HOME=/cad2/ece419s/
# JAVA_HOME=${ECE419_HOME}/java/jdk1.6.0/

# ${JAVA_HOME}/bin/java EchoServer 8000



