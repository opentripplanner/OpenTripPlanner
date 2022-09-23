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

#### Norway

- [Norwegian NeTEx data](https://leonard.io/otp/rb_norway-aggregated-netex-2021-12-11.zip)
- [Norway OSM data](https://download.geofabrik.de/europe/norway-210101.osm.pbf)

If the link above do not work you should be able to find it on the ENTUR web:

- https://www.entur.org/

#### Baden-Württemberg, Germany

- [Tidied GTFS data](https://leonard.io/otp/baden-wuerttemberg-2022-07-25.gtfs.tidy.zip)
- [BW OSM data](https://download.geofabrik.de/europe/germany/baden-wuerttemberg-220101.osm.pbf)
 
#### Germany

- [Tidied GTFS data](https://leonard.io/otp/germany-2022-08-23.tidy.gtfs.zip)
