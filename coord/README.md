# Coord OTP.

## Installing maven.

`brew install maven`

## Setup, must happen at least once (and potentially for any changes in graph building algorithm).

`./coord/setup.sh`

## To build

`mvn package -Dmaven.test.skip=true`

## To run locally

First: `export COORD_API_KEY=<valid_api_key>`

Next: `./coord/run-otp.sh`

Note that OTP only needs to restarted if the graph has been rebuilt (previous setup) or for
configuration changes. Restarts take time (minutes), so avoid them if possible!


Also note that if you are running from within an IDE, you must set the following environment variables:

```bash
export COORD_API_KEY=<>
export API_BIKE_SERVICE_HOST="api.coord.co"
export API_BIKE_SERVICE_PORT="443"
```

## To generate fake bike drop-off stations

```bash
./coord/util/generate_stations.py -77.05759048 -76.921291 38.85160659 38.940585 100 100
```
which generates bike stations within the given boundary and a point every 100m.
```bash
./coord/util/generate_stations.py -h
```
to see the parameter description.


