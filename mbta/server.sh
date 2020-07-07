#!/bin/sh
IN_MEMORY="--inMemory"
if [ -f "var/graphs/mbta/Graph.obj" ]; then
    IN_MEMORY=""
fi
java -Xmx4G -jar otp-1.4.0-SNAPSHOT-shaded.jar --server --basePath var/ $IN_MEMORY --router mbta
