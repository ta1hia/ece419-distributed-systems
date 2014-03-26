#!/bin/bash
# Run Zookeper
MYZK=myzk

${MYZK}/conf/INITCFG.sh
${MYZK}/bin/zkServer.sh start
