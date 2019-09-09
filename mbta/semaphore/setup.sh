#!/bin/bash
set -e

# ensure we use Java 8; other versions have an issue building the graph:
# https://groups.google.com/forum/#!topic/opentripplanner-users/pvtm3BSyS9g
source /opt/change-java-version.sh
change-java-version 8

export M2_CACHE="${SEMAPHORE_CACHE_DIR}/.m2/"
mkdir -p "${M2_CACHE}"

pushd "${SEMAPHORE_PROJECT_DIR}/../"
rm -rf onebusaway-gtfs-modules
git clone git@github.com:mbta/onebusaway-gtfs-modules.git --depth 1
cd onebusaway-gtfs-modules
mvn -Dmaven.repo.local="${M2_CACHE}" clean install -Dmaven.test.skip=true -Dlicense.skip=true
popd