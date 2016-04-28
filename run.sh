#!/bin/bash

JAR=`ls target/*-shaded*`
echo JAR=$JAR
ROUTE_DATA_URL=$OTP_DATA_CONTAINER_URL

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
  echo "Name is: $NAME"
  echo "URL is: $URL"

  if [[ "$(curl $URL -z $FILE -o $FILE -s -L -w %{http_code})" == "200" ]]; then
    build_graph $NAME $FILE
  else
    echo "file is the same, skipping graph-building"
  fi
}

GRAPH_STRING=""
for GRAPH in `curl -s $ROUTE_DATA_URL/routers.txt|cut -d'-' -f2|cut -d'.' -f1`
do
  process $GRAPH
  GRAPH_STRING="$GRAPH_STRING --router $GRAPH"
done

echo "graphString is: $GRAPH_STRING"
java -Xmx7500M -Duser.timezone=Europe/Helsinki -jar $JAR --server --port $PORT --securePort $SECURE_PORT --basePath ./ --graphs ./graphs $GRAPH_STRING
