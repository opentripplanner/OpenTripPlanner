## Speed test

This folder contains configuration and expectations to run the OTP speed test. The test runs 
automatically after each merged PR and the results are visualised on a [Grafana instance](https://otp-performance.leonard.io).

Each run is listed under the [GitHub Actions](https://github.com/opentripplanner/OpenTripPlanner/actions/workflows/performance-test.yml). 

If you need to run the test locally you
need to download the fixed datasets (OSM, transit) listed below and build a graph from them first.

After the graph is built, copy it into `./test/performance/${location}` and execute the following
command to run the speed test:

```
mvn exec:java -Dexec.mainClass="org.opentripplanner.transit.raptor.speed_test.SpeedTest" -Dexec.classpathScope=test -Dexec.args="--dir=test/performance/${location} -p md -n 4 -i 3 -0"
```

The results will be displayed on the console.

### Instrumentation

Each run on CI is instrumented with Java Flight Recorder. The results are then saved as an artifact
and can be downloaded. IntelliJ for example can display a very useful flame graph from those jrf files
that shows bottlenecks in the code.

### Configure the test

- Pick a valid "testDate" for your data set and set it in the speed-test-config.json.
- Make sure build-config "transitServiceStart" and "transitServiceEnd" include the "testDate".

### Data files

All data input files are located at https://otp-performance.leonard.io/data/

#### Norway
Data used:
- Norwegian NeTEx data
- Norway OSM data

[build-config](norway/build-config.json)

If the link above do not work you should be able to find it on the ENTUR web:

- https://www.entur.org/

#### Baden-Württemberg, Germany
Data used:
- Tidied GTFS data
- BW OSM data

[build-config](baden-wuerttemberg/build-config.json)
 
#### Germany
Data used:
- Tidied GTFS data

[build-config](germany/build-config.json)

#### Skånetrafiken
Data used:
- Skånetrafiken NeTEx data
- Sweden OSM data
- Denmark GTFS data
- Denmark OSM data

[build-config](skanetrafiken/build-config.json)