#!/bin/bash

./download-data.sh

# Build the binary.
mvn package -Dmaven.test.skip=true

# Build the graph.
java -Xmx4G -jar target/otp-1.3.0-SNAPSHOT-shaded.jar --build coord/otp-base/graphs/dc
