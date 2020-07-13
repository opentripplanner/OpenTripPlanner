#!/bin/sh
set -e

# it's important for MBTA_GFTS to be first file in the folder,
# otherwise realtime alerts won't work: https://github.com/mbta/OpenTripPlanner/pull/8

wget -N https://mbta-gtfs-s3.s3.amazonaws.com/google_transit.zip -O var/graphs/mbta/1_MBTA_GTFS.zip
wget -N http://www.massport.com/media/4109/massport_gtfs.zip -O var/graphs/mbta/2_loganexpress-ma-us.zip
