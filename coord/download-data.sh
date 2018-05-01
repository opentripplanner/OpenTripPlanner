#!/bin/bash

# Get DC metro road network, which was generated once using this tool: http://extract.bbbike.org
# Bounding box: (-77.458,38.678) (-76.607,39.114)
curl -L -o coord/otp-base/graphs/dc/dc-metro.osm.pbf https://storage.googleapis.com/coord-osm/dc-metro.osm.pbf

# Get DC transit network data.
curl -L -o coord/otp-base/graphs/dc/dc.gtfs.zip https://transitfeeds.com/p/wmata/75/latest/download
