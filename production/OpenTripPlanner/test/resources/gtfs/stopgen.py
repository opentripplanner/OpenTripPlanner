#!/usr/bin/python

# This little script generates lists of nicely-spaced stops in the Gulf of Guinea.
# Useful in making test GTFS.

EARTH_CIRCUMFERENCE = 40075160 
METERS_PER_DEGREE = EARTH_CIRCUMFERENCE / 360.0
DEGREES_PER_METER = 360.0 / EARTH_CIRCUMFERENCE

step_m = 400 # meters
step_deg = step_m * DEGREES_PER_METER
lat0, lon0 = (0.2, 0.2) # near equator
lat, lon = lat0, lon0
for row in range(2) :
    for col in range(10) :
        print lat, lon, "stop" + str(col)
        lon += step_deg
    lat += step_deg
    lon = lon0
    print
