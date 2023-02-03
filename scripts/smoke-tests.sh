#! /bin/bash -e

LOCATION=$1
SLEEP_SECS=$2

cd smoke-tests
make build-"${LOCATION}"

make run-"${LOCATION}" &

# OTP needs a little while to start up so we sleep
sleep "$SLEEP_SECS"

cd ..

# run the actual smoke tests
# we run surefire:test in order to not recompile the tests for each city
mvn surefire:test -Djunit.tags.included="${LOCATION}" -Djunit.tags.excluded="" -P prettierSkip

# shutting down the OTP instance running in the background (via make)
killall make
