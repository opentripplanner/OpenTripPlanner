#!/bin/bash
# should be run as ./mbta/semaphore/build.sh
set -e

./mbta/update_pbf.sh
./mbta/update_gtfs.sh
./mbta/build.sh
./mbta/make_deploy.sh
