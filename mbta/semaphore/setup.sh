#!/bin/bash
set -e

# Add more swap space.
sudo swapoff -a
sudo dd if=/dev/zero of=/swapfile bs=1M count=8192
sudo chmod 0600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

export M2_CACHE="${SEMAPHORE_CACHE_DIR}/.m2/"
mkdir -p "${M2_CACHE}"

pushd "${SEMAPHORE_PROJECT_DIR}/../"
rm -rf onebusaway-gtfs-modules
git clone git@github.com:mbta/onebusaway-gtfs-modules.git --depth 1
cd onebusaway-gtfs-modules
mvn -Dmaven.repo.local="${M2_CACHE}" clean install -Dmaven.test.skip=true -Dlicense.skip=true
popd
