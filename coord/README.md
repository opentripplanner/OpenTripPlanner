# Coord OTP.

## To run locally

Run a fake GBFS feed (http://localhost:10000) and the frontend (http://localhost:9000) in one terminal:

`./coord/run-helpers.sh`

Run the actual OTP server in another. OTP only needs to restarted to reload the transit or road
networks or for configuration changes. Restarts take time (minutes), so avoid them if possible!

`./coord/run-otp.sh`

## To generate fake bike stations (hubs)

```bash
./coord/util/generate_stations.py /tmp/gbfs -77.05759048 \
    -76.921291 38.85160659 38.940585 150 150
```
which generates bike stations within the given boundary and a point every 150m.
