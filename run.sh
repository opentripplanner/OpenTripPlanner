#!/bin/bash

JAR=`ls target/*-shaded*`
echo JAR=$JAR
ROUTE_DATA_URL=$OTP_DATA_CONTAINER_URL
SLEEP_TIME=5

function build_graph {
  echo "building graph..."
  GRAPHNAME=$1
  FILE=$2
  DIR="graphs/$NAME"
  mkdir -p $DIR
  unzip -o -j -d $DIR $FILE
  java -Xmx7500M -jar $JAR --build $DIR
}

function process {
  NAME=$1
  URL="$ROUTE_DATA_URL/router-$NAME.zip"
  FILE="$NAME.zip"
  MD5FILE=$FILE.md5

  echo "Retrieving graph bundle from $URL"
  until curl -f -s $URL -o $FILE
  do
    echo "Error retrieving graph bundle from otp-data-server... retrying in $SLEEP_TIME s..."
    sleep $SLEEP_TIME
  done

  build_graph $NAME $FILE
}


function getRouteConfig {
  URL=$ROUTE_DATA_URL/routers.txt
  echo "Retrieving router config metadata from $URL"
  until CONFIG=`curl -f -s $URL`
  do
    echo "Error retrieving graph config from otp-data-server... retrying in $SLEEP_TIME s..."
    sleep $SLEEP_TIME
  done
}

GRAPH_STRING=""
getRouteConfig

for GRAPHFILE in $CONFIG
do
  GRAPH=`echo $GRAPHFILE|cut -d '.' -f 1|cut -d'-' -f2` 
  process $GRAPH
  GRAPH_STRING="$GRAPH_STRING --router $GRAPH"
done

echo "graphString is: $GRAPH_STRING"
java -Xmx7500M -Duser.timezone=Europe/Helsinki -jar $JAR --server --port $PORT --securePort $SECURE_PORT --basePath ./ --graphs ./graphs $GRAPH_STRING
