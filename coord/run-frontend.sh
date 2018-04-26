#!/bin/bash

trap 'kill $(jobs -p)' EXIT

cd src/client
python3 -m http.server 9000
