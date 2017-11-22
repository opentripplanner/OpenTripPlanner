#!/usr/bin/python

# This little script generates fake pointsets.

import random

EARTH_CIRCUMFERENCE = 40075160 
METERS_PER_DEGREE = EARTH_CIRCUMFERENCE / 360.0
DEGREES_PER_METER = 360.0 / EARTH_CIRCUMFERENCE

# Portland
maxLat = 45.896979
maxLon = -122.1131338
minLat = 45.096058
minLon = -123.2109336

lo = 0
hi = 20

n = 20000

print "lat,lon,child,adult,senior"

for i in range(n) :
    lat = random.triangular(minLat, maxLat)
    lon = random.triangular(minLon, maxLon)
    adult  = int(random.triangular(lo, hi))
    child  = int(adult * random.triangular(0, 2))
    senior = int(adult * random.triangular(0, 1))
    fields = [lat,lon,child,adult,senior]
    print ','.join(map(str, fields))

