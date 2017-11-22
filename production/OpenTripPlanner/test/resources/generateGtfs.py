#!/usr/bin/python

readme = """A fake GTFS feed for use in testing departure time searches.
It is used to test pickup/dropoff combinations, wheelchair boarding and headsigns at the per-trip 
and per-stop levels, bicycle-allowing trips, banned routes and trips, and multiple boarding search 
return values all with one feed.

The feed describes two transit routes named A and B.
Both routes have service every 15 minutes from 06:00 until 23:00.
Trip IDs are the route name concatenated with the zero-padded first departure time, e.g. A0730.
Both routes have seven stops, with each stop serving only one route. 
Stops are numbered 0 through 7, and their IDs are the route name concatenated with the stop number. 
Stops A3 and B3 are located in a single station complex with ID 'xfer'.
Each stop is 10 minutes away from the previous one on its line.
Arrival times are identical to departure times at stops on route A (i.e. there is no dwell time),
but arrivals are 1 minute before departures on route B (there is a dwell of 1 minute at each stop). 

All trips allow bicycles except those during rush hour (defined as 7-9 AM and 5-7 PM).
Trips departing the first stop at x:00 and x:30 can accommodate wheelchairs, while those departing 
at x:15 and x:45 cannot. Wheelchair accessibility also varies at the stop level, with odd-numbered 
stops marked inaccessible.

Starting at 10 PM stops 0 and 1 are pick-up-only, 2 through 4 allow pick-up and drop-off,
and 5 and 6 only allow drop-off. Before 10 PM, all stops allow both pick-up and drop-off.

Trip-level headsign definitions read EAST on route A and SOUTH on route B. 
Stop-level headsign definitions are provided at stops 0, 1, and 2 which append the text 'VIA XFER'.

"""

# 1. The wheelchair_boarding field in stops.txt is standard but optional.
# 2. The wheelchair_accessible field in trips.txt is an (optional) extension.
#    http://support.google.com/transitpartners/bin/answer.py?hl=en&answer=2450962
# 3. The trip_bikes_allowed field in trips.txt is an (optional) extension.
#    https://groups.google.com/d/msg/gtfs-changes/QqaGOuNmG7o/uKpD70szrbkJ

WHEELCHAIR_YES = 1
WHEELCHAIR_NO = 2
WHEELCHAIR_UNDEF = 0
BIKE_YES = 2
BIKE_NO = 1
BIKE_UNDEF = 0
LOCATION_STOP = 0
LOCATION_STATION = 1
PICKDROP_SCHEDULED = 0
PICKDROP_NO = 1
PICKDROP_PHONE = 2
PICKDROP_COORDINATE = 3

# zipfile has no way to add file-like buffers (SpooledTemporaryFile is out), so we have to add the 
# intermediate files from the filesystem, subclass ZipFile, or write from strings. We use the latter.
from zipfile import ZipFile
gtfs = ZipFile('generated.gtfs.zip', mode='w')

# The agency and calendar tables are hard coded (not generated). There should be 
# one agency called TEST and one service_id called ALL that runs every day of the week from 
# 01-JAN-2012 through 31-DEC-2012.
agency_rows = """agency_id,agency_name,agency_url,agency_timezone
TEST,Gulf of Guinea Transit,http://www.test.com,Africa/Accra
"""
gtfs.writestr('agency.txt', agency_rows)
calendar_rows = """service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date
ALL,1,1,1,1,1,1,1,20120101,20121231
"""
gtfs.writestr('calendar.txt', calendar_rows)

from cStringIO import StringIO
routes_file = StringIO()
stops_file = StringIO()
import csv
routes = csv.writer(routes_file)
routes.writerow(['route_id', 'route_short_name', 'route_long_name', 'route_type'])
stops = csv.writer(stops_file)
stops.writerow(['stop_id', 'stop_name', 'stop_lat', 'stop_lon', 'location_type', 'parent_station', 'wheelchair_boarding'])
stops.writerow(['xfer', 'xfer', '1.00', '1.03', LOCATION_STATION, None, WHEELCHAIR_YES]) # station allows wheelchair access
for route in ['A', 'B'] :
    routes.writerow((route, route, route, 1)) # make B a different mode for mode param testing?
    if route == 'A' :
        stop_lon = stop_lat = 1.00
    else :
        stop_lon = stop_lat = 1.03
    for stop in range (7) :
        stop_id = route + str(stop)
        wheelchair = WHEELCHAIR_YES if (stop % 2 == 0) else WHEELCHAIR_NO
        parent_station = 'xfer' if stop == 3 else None
        row = (stop_id, stop_id, stop_lat, stop_lon, LOCATION_STOP, parent_station, wheelchair)
        stops.writerow(row)
        if route == 'A' :
            stop_lon += 0.01000
        else :
            stop_lat -= 0.01000

gtfs.writestr('routes.txt', routes_file.getvalue())
gtfs.writestr('stops.txt', stops_file.getvalue())

def sec(h, m, s = 0) :
    return (((h * 60) + m) * 60) + s
    
def tstr(h, m, s = 0):
    # handle (possibly negative) minute overage
    s = sec(h, m, s)
    m = s / 60
    s = s % 60
    h = m / 60
    m = m % 60
    return '%02d:%02d:%02d' % (h, m, s)
    
trips_file = StringIO()
trips = csv.writer(trips_file)
header = ('route_id', 'service_id', 'trip_id', 'trip_headsign')
trips.writerow(header)

stop_times_file = StringIO()
stop_times = csv.writer(stop_times_file)
header = ('trip_id', 'arrival_time', 'departure_time', 'stop_id', 'stop_sequence', 'stop_headsign', 'pickup_type', 'drop_off_type')
stop_times.writerow(header)

for route in ['A', 'B'] :
    headsign = 'EAST' if route == 'A' else 'SOUTH'
    for hour in range (6, 23):
        bicycle = not (hour in [7, 8, 17, 18])
        for minute in [0, 15, 30, 45] :
            # print 'route %s %02d:%02d' % (route, hour, minute)
            trip_id = '%s%02d%02d' % (route, hour, minute)
            wheelchair_trip = minute in [0, 30]
            arv = dep = minute
            if route == 'B' :
                arv -= 1
            row = (route, 'ALL', trip_id, headsign)
            trips.writerow(row)
            for stop in range (7) :
                stop_id = route + str(stop)
                if hour < 22 :
                    pickup = drop_off = PICKDROP_SCHEDULED
                else :
                    pickup   = PICKDROP_SCHEDULED if stop <= 4 else PICKDROP_NO
                    drop_off = PICKDROP_SCHEDULED if stop >= 2 else PICKDROP_NO
                stop_headsign = headsign + ' VIA XFER' if stop < 3 else None
                row = (trip_id, tstr(hour, arv), tstr(hour, dep), stop_id, stop + 100, stop_headsign, pickup, drop_off)
                stop_times.writerow(row)
                arv += 10
                dep += 10

gtfs.writestr('trips.txt', trips_file.getvalue())
gtfs.writestr('stop_times.txt', stop_times_file.getvalue())
gtfs.writestr('README', readme)
#interline trips back in the other direction?


