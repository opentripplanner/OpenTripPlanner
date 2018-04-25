#!/bin/bash

trap 'kill $(jobs -p)' EXIT

./coord/run-test-gbfs.sh &
./coord/run-frontend.sh
