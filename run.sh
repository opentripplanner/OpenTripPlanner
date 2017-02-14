#!/bin/bash

# Fail fast if graph building fails
set -e

JAR=`ls target/*-shaded*`
echo JAR=$JAR
SLEEP_TIME=5

function build_graph {
  GRAPHNAME=$1
  FILE=$2
  echo "building graph..."
  DIR="graphs/$NAME"
  mkdir -p $DIR
  unzip -o -j -d $DIR $FILE
  java $JAVA_OPTS -jar $JAR --build $DIR
}

function process {
  NAME=$1
  URL="$ROUTER_DATA_CONTAINER_URL/router-$NAME.zip"
  FILE="$NAME.zip"
  MD5FILE=$FILE.md5

  echo "Retrieving graph bundle from $URL"
  until curl -f -s $URL -o $FILE
  do
    echo "Error retrieving graph bundle $URL from otp-data-server... retrying in $SLEEP_TIME s..."
    sleep $SLEEP_TIME
  done

  build_graph $NAME $FILE
}

#workaround for azure DNS issue

if [ "$EUID" -eq 0 ]
  then echo "search marathon.l4lb.thisdcos.directory" >> /etc/resolv.conf
fi

process $ROUTER_NAME

echo "graphString is: $ROUTER_NAME"
java $JAVA_OPTS -Duser.timezone=Europe/Helsinki -jar $JAR --server --port $PORT --securePort $SECURE_PORT --basePath ./ --graphs ./graphs --router $ROUTER_NAME
