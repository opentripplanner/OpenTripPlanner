# Coord OTP.

## To run locally

Run a fake GBFS feed (http://localhost:10000) and the frontend (http://localhost:9000) in one terminal:

`./coord/run-helpers.sh`

Run the actual OTP server in another. OTP only needs to restarted to reload the transit or road
networks or for configuration changes. Restarts take time (minutes), so avoid them if possible!

`./coord/run-otp.sh`
