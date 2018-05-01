#!/bin/bash

export API_BIKE_SERVICE_HOST="api.coord.co"
export API_BIKE_SERVICE_PORT="443"


# TODO(danieljy): Figure out why loading a pre-existing graph spits warnings.
# java -Xmx4G -jar target/otp-1.3.0-SNAPSHOT-shaded.jar --server --basePath coord/otp-base --router dc

# Build the graph from scratch.
java -Xmx4G -jar target/otp-1.3.0-SNAPSHOT-shaded.jar --build coord/otp-base/graphs/dc --inMemory
