# Speed test

This folder contains configuration and expectations to run the OTP speed test. The test runs 
automatically after each merged PR and the results are visualised on a [Grafana instance](https://otp-performance.leonard.io).

Each run is listed under the [GitHub Actions](https://github.com/opentripplanner/OpenTripPlanner/actions/workflows/performance-test.yml). 

If you need to run the test locally you
need to download the fixed datasets (OSM, transit) listed below and build a graph from them first.

After the graph is built, copy it into `./test/performance/${location}` and execute the following
command to run the speed test (also make sure the tests are compiled with e.g. `mvn test-compile`):

```
mvn --projects application exec:java -Dexec.mainClass="org.opentripplanner.transit.speed_test.SpeedTest" -Dexec.classpathScope=test -Dexec.args="--dir=test/performance/${location} -p md -n 4 -i 3 -0"
```

The results will be displayed in the console.

## CI

The test is run after every merge to dev-2.x. Its GitHub Actions workflow is defined
in [performance-test.yml](/.github/workflows/performance-test.yml).

### Running the test manually

Sometimes it is desirable to run the test configuration in the CI environment manually, for
example, when making changes to the tests. To do this, perform these steps:
1. Create a local branch in git that contains the desired performance test configuration.
2. In [performance-test.yml](/.github/workflows/performance-test.yml) add your test branch name to
the configuration and change all those locations that you want to run on the branch to `core`:

```diff
name: Performance test

on:
  push:
    branches:
      - dev-2.x
+     - test-branch-name

jobs:
  perf-test:
    if: github.repository_owner == 'opentripplanner' && !startsWith(github.event.head_commit.message ,'Bump serialization version id for') && !startsWith(github.event.head_commit.message ,'Upgrade debug client to version')
    runs-on: performance-test
    strategy:
      fail-fast: false
      matrix:
        include:
          ...
          - location: helsinki
            iterations: 1
            jfr-delay: "50s"
-           profile: extended
+           profile: core
...
```
3. Commit the changes to [performance-test.yml](/.github/workflows/performance-test.yml).
4. Push the changes to a branch in the **upstream** [OpenTripPlanner](https://github.com/opentripplanner/OpenTripPlanner/) repository with the same name you added to [performance-test.yml](/.github/workflows/performance-test.yml).
5. The tests will run after the push.

**Note** In order to visualise before and after, it is helpful to push a commit that is the same as the current `dev-2.x` and run the speed
test on that.

## Instrumentation

Each run on CI is instrumented with Java Flight Recorder. The results are then saved as an artifact
and can be downloaded. IntelliJ for example can display a very useful flame graph from those jrf files
that shows bottlenecks in the code.

## Configure the test

- Pick a valid "testDate" for your data set and set it in the speed-test-config.json.
- Make sure build-config "transitServiceStart" and "transitServiceEnd" include the "testDate".

## Data files

All data input files are located at https://otp-performance.leonard.io/data/

### Norway

[📊 Dashboard](https://otp-performance.leonard.io/) 

Data used:
- Norwegian NeTEx data
- Norway OSM data

Contact: [T.Gran, Entur](https://github.com/t2gran)

[build-config](norway/build-config.json)

If the link above do not work you should be able to find it on the ENTUR web:

- https://www.entur.org/

### Baden-Württemberg, Germany

[📊 Dashboard](https://otp-performance.leonard.io/d/9sXJ43gVk/otp-performance?orgId=1&var-category=transit&var-branch_fixed=dev-2.x&var-location=baden-wuerttemberg&var-branch=dev-2.x&from=1658872800000&to=now)

Data used:
- Tidied GTFS data
- BW OSM data

[build-config](baden-wuerttemberg/build-config.json)
 
### Hamburg, Germany

[📊 Dashboard](https://otp-performance.leonard.io/d/9sXJ43gVk/otp-performance?orgId=1&var-category=transit&var-branch_fixed=dev-2.x&var-location=hamburg&var-branch=dev-2.x&from=1658872800000&to=now)

Data used:
- NeTEx data
- Hamburg metropolitan area OSM data

Contact: [Geofox Team](mailto:Geofox-team@hbt.de)

[build-config](hamburg/build-config.json)

### Helsinki, Finland

[📊 Dashboard](https://otp-performance.leonard.io/d/9sXJ43gVk/otp-performance?orgId=1&var-category=transit&var-branch_fixed=dev-2.x&var-location=helsinki&var-branch=dev-2.x&from=1658872800000&to=now)

Data used:
- HSL GTFS data
- Helsinki metropolitan area OSM data

[build-config](helsinki/build-config.json)

### Germany

[📊 Dashboard](https://otp-performance.leonard.io/d/9sXJ43gVk/otp-performance?orgId=1&var-category=transit&var-branch_fixed=dev-2.x&var-location=germany&var-branch=dev-2.x&from=1661292000000&to=now)

Data used:
- Tidied GTFS data

[build-config](germany/build-config.json)

### Skånetrafiken

[📊 Dashboard](https://otp-performance.leonard.io/d/9sXJ43gVk/otp-performance?orgId=1&var-category=top-5000&var-branch_fixed=dev-2.x&var-location=skanetrafiken&var-branch=dev-2.x&from=1666965240000&to=now)

Data used:
- Skånetrafiken NeTEx data
- Sweden OSM data
- Denmark GTFS data
- Denmark OSM data

[build-config](skanetrafiken/build-config.json)


### Switzerland

[📊 Dashboard](https://otp-performance.leonard.io/d/9sXJ43gVk/otp-performance?orgId=1&var-category=transit&var-branch_fixed=dev-2.x&var-location=switzerland&var-branch=dev-2.x&from=1666965240000&to=now)

Data used:
- Switzerland OSM data
- Switzerland GTFS national data

[build-config](switzerland/build-config.json)

### Washington State

This data set tests a lot of overlapping flex routes in and around Seattle in Washington State.

[📊 Dashboard](https://otp-performance.leonard.io/d/9sXJ43gVk/otp-performance?orgId=1&var-category=flex&var-location=washington-state&var-branch=dev-2.x&from=1669892798000&to=now)

[build-config](washington-state/build-config.json)
