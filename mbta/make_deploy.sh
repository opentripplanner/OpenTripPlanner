#!/bin/bash
rm mbta_otp.zip
zip mbta_otp.zip Procfile otp-1.4.0*-shaded.jar var/graphs/mbta/Graph.obj var/graphs/mbta/*.json
