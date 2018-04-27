#!/bin/bash

# Get downtown DC road-network data.
curl -L -o coord/otp-base/graphs/dc/district-of-columbia-latest.osm.pbf http://download.geofabrik.de/north-america/us/district-of-columbia-latest.osm.pbf

# Get DC transit network data.
curl -L -o coord/otp-base/graphs/dc/dc.gtfs.zip https://transitfeeds.com/p/wmata/75/latest/download

# Build the binary.
mvn package -Dmaven.test.skip=true

# Build the graph.
java -Xmx4G -jar target/otp-1.3.0-SNAPSHOT-shaded.jar --build coord/otp-base/graphs/dc
