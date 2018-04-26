#!/bin/bash

trap 'kill $(jobs -p)' EXIT

cd coord/test-gbfs
python3 -m http.server 10000
