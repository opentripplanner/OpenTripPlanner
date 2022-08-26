#! /bin/bash -e

cd smoke-tests
make build-atlanta
make run-atlanta &

# OTP needs a little while to start up so we sleep
sleep 15

cd ..

# run the actual smoke tests
# we run surefire:test in order to not recompile the tests for each city
mvn surefire:test -D groups=smoke-test -P prettierSkip