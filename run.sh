#!/bin/bash

# Fail fast if graph building fails
set -e

JAR=`ls *-shaded*`
echo JAR=$JAR
SLEEP_TIME=5

function url {
  echo $ROUTER_DATA_CONTAINER_URL/$1
}

function build_graph {
  GRAPHNAME=$1
  FILE=$2
  echo "building graph..."
  DIR="graphs"
  mkdir -p $DIR
  unzip -o -d $DIR $FILE
  mv $DIR/router-$GRAPHNAME $DIR/$GRAPHNAME
  java $JAVA_OPTS -jar $JAR --build $DIR/$GRAPHNAME
}

function download_graph {
  NAME=$1
  VERSION=$2
  GRAPH_FILE=graph-$NAME.zip
  URL=$(url "graph-$NAME-$VERSION.zip")
  echo "Downloading graph from $URL"
  for i in {1..6}; do
    HTTP_STATUS=$(curl --write-out %{http_code} --silent --output $GRAPH_FILE $URL)

    if [ "$HTTP_STATUS" -eq 404 ]; then
        rm  $GRAPH_FILE > /dev/null
        echo "Graph not found";
       return 1;
    fi

    if [ "$HTTP_STATUS" -eq 200 ]; then break;
    fi;

    rm  $GRAPH_FILE > /dev/null
    sleep $SLEEP_TIME;

  done

  if [ -f graph-$NAME.zip ]; then
    unzip $GRAPH_FILE  -d ./graphs
    return $?;
  else
    return 1;
  fi
}

function version {
  java -jar $JAR --version|grep commit|cut -d' ' -f2
}

function process {
  NAME=$1
  URL=$(url "router-$NAME.zip")
  FILE="$NAME.zip"

  echo "Retrieving graph source bundle from $URL"
  until curl -f -s $URL -o $FILE
  do
    echo "Error retrieving graph source bundle $URL from otp-data-server... retrying in $SLEEP_TIME s..."
    sleep $SLEEP_TIME
  done

  build_graph $NAME $FILE
}

#workaround for azure DNS issue

if [ "$EUID" -eq 0 ]
  then echo "search marathon.l4lb.thisdcos.directory" >> /etc/resolv.conf
fi


VERSION=$(version)

echo VERSION $VERSION
echo ROUTER $ROUTER_NAME


download_graph $ROUTER_NAME $VERSION || process $ROUTER_NAME

echo "graphString is: $ROUTER_NAME"
java $JAVA_OPTS -Duser.timezone=Europe/Helsinki -jar $JAR --server --port $PORT --securePort $SECURE_PORT --basePath ./ --graphs ./graphs --router $ROUTER_NAME
