#!/bin/bash

mypath=$(readlink -f $0)

dirpath=$(dirname $mypath)

CFG=${dirpath}/zoo.cfg

echo
cfgdone=$(grep "dataDir" $CFG)
if [ -z "$cfgdone" ]; then
    # the directory where the snapshot is stored.
    datadir="/tmp/${USER}/server1/data"
    mkdir -p $datadir
    echo "dataDir=${datadir}" >> $CFG
    # the port at which the clients will connect
    echo "clientPort=$((RANDOM%1000+8000))" >> $CFG
else
    echo "$CFG already initialized"
fi
echo
echo "Use clientPort to connect to zookeeper. For instance: bin/zkCli.sh -server serverIP:clientPort"
tail -n 1 $CFG
echo
