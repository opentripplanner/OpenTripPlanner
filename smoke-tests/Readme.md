## IBI Group OTP Smoke Tests

This folder contains scripts and configuration files for running an OTP instance that can then be
use to run smoke tests against. These tests are intended as a last sanity check when we merge
upstream changes as these merges tend to be very large. It helps to reduce the burden on the
reviewer and increases the confidence that the upstream merge won't break anything.

### Commands

If you want to prepare an OTP instance for the smoke tests, run the following commands to build the
jar, download all OSM and GTFS files and then build the graph:

```
# start in root OTP dir
cd OpenTripPlanner
mvn package -DskipTests
cd smoke-tests
make build-atlanta
```

After the graph is build you can then run OTP with:

```
make run-atlanta
```

And finally run the smoke tests with

```
mvn test -P smoke-tests
```

### CI

The CI workflow that does all of this automatically can be found
at [smoke-tests.yml](../.github/workflows/smoke-tests.yml).
