#!/usr/bin/env python

"""
This script generates fake dropoffs within the coverage area of bike systems.

usage: generate_stations.py [-h]
                            lng_min lng_max lat_min lat_max lat_meters
                            lng_meters

positional arguments:
  lng_min     The minimum longitude.
  lng_max     The maximum longitude.
  lat_min     The minimum latitude.
  lat_max     The maximum latitude.
  lat_meters  The latitudinal distance between stations in meters.
  lng_meters  The longitudinal distance between stations in meters.

optional arguments:
  -h, --help  show this help message and exit
"""

import argparse
import geojson
import geopy.distance
import json
import numpy as np
import os
import sys
import urllib.request

from geojson import Feature
from geojson import FeatureCollection
from json import encoder
from shapely.geometry import shape
from shapely.geometry import Point

# depending on your version, use: from shapely.geometry import shape, Point


# To avoid long float numbers for lat/lng
encoder.FLOAT_REPR = lambda o: format(o, '.6f')

parser = argparse.ArgumentParser()
parser.add_argument('lng_min', type=float, help='The minimum longitude.')
parser.add_argument('lng_max', type=float, help='The maximum longitude.')
parser.add_argument('lat_min', type=float, help='The minimum latitude.')
parser.add_argument('lat_max', type=float, help='The maximum latitude.')
parser.add_argument('lat_meters', type=int,
                    help='The latitudinal distance between stations in meters.',
                    default=200)
parser.add_argument('lng_meters', type=int,
                    help='The longitudinal distance between stations in meters.',
                    default=200)
args = parser.parse_args()

# Map of bike system ID to its boundary polygon (a shapely geometry obj which is a MultiPolygon)
BikeToPolygon = {}


def create_station(lng, lat, station_id, system_ids):
    return {
        'lon': round(lng, 6),
        'lat': round(lat, 6),
        'station_id': "fake_dropoff_%d" % + station_id,
        'system_ids': system_ids
    }


def load_polygons_by_system(system_id):
    """Calls Coord's Bike API to get the polygons of the system.

    :param system_id: string, the ID of a bike system
    :return: a shapely geometry obj
    """

    response = urllib.request.urlopen("https://api.coord.co/v1/bike/system/%s?access_key=%s" % (
        system_id, os.environ['COORD_API_KEY'])).read()
    json_resp = json.loads(response)
    if json_resp['geometry']:
        return shape(json_resp['geometry'])
    return None


def get_dockless_bike_system_ids():
    """ Returns Coord's dockless bike system IDs in the given area.

        We currently don't have an endpoint that returns `systems`,
        so we have to do an area search and find them.
    :return: a list of string, system_ids
    """
    lat = args.lat_min + (args.lat_max - args.lat_min) / 2
    lng = args.lng_min + (args.lng_max - args.lng_min) / 2
    url = "https://api.coord.co/v1/bike/location?access_key=%s" \
          "&latitude=%3.4f&longitude=%3.4f&radius_km=3" % (os.environ['COORD_API_KEY'], lat, lng)

    response = urllib.request.urlopen(url).read()
    json_resp = json.loads(response)
    system_ids = set()
    if json_resp['features'] is None:
        print("Cannot fetch bike system IDs at lng/lat: %3.5f,%3.5f from Coord API: %s" % (
            lng, lat, json_resp))
        sys.exit(0)

    for feature in json_resp['features']:
        sid = feature['properties']["system_id"]
        # We only generate fake dropoffs for dockless bikes, so skip the docked ones.
        if sid == 'CapitalBikeShare':
            # TODO(mahmood): remove this after the API can return docked/dockless status
            continue
        system_ids.add(sid)

    return list(system_ids)


def load_polygons():
    """Loads the polygons of all bikes systems available in an area."""

    global BikeToPolygon
    default_system = None
    systems_without_geom = []
    for s in get_dockless_bike_system_ids():
        geom = load_polygons_by_system(s)
        print("Loading geom for %s " % s, end='')
        if geom:
            print("\u2713")  # print a check mark
            BikeToPolygon[s] = geom
            default_system = s
        else:
            print("\u274c")  # print a cross
            systems_without_geom.append(s)

    if default_system is None or BikeToPolygon[default_system] is None:
        raise Exception("No geometry found for any bike system in the area.")

    # Use the default geom for systems without geometry.
    for sid in systems_without_geom:
        BikeToPolygon[sid] = BikeToPolygon[default_system]
        print("Using %s's geometry for %s." % (default_system, sid))


def get_system_ids_by_location(lng, lat):
    point = Point(lng, lat)
    system_ids = []
    for sid, polygon in BikeToPolygon.items():
        if polygon.contains(point):
            system_ids.append(sid)
    return system_ids


def generate_fake_stations():
    """Generate fake stations as a GeoJSON file."""
    features = []
    i = 0
    for lng in get_lng_linspace():
        for lat in get_lat_linspace():
            system_ids = get_system_ids_by_location(lng, lat)
            if len(system_ids) > 0:
                features.append(Feature(
                    geometry=Point((lng, lat)),
                    properties=create_station(lng, lat, i, system_ids)))
                i += 1

    with open('fake_stations.json', 'w') as outfile:
        geojson.dump(FeatureCollection(features), outfile, indent=2, sort_keys=True)

    print('Total # of stations:', len(features))


def get_lat_linspace():
    """Return evenly spaced latitudes."""
    coords_1 = (args.lat_min, args.lng_min)
    coords_2 = (args.lat_max, args.lng_min)
    dist = geopy.distance.vincenty(coords_1, coords_2).km * 1000
    if dist < args.lat_meters:
        raise Exception("lat_meters is too large compared with given lat_min and lat_max")
    if args.lat_meters == 0:
        raise Exception("lat_meters cannot be 0")

    num_of_steps = dist / args.lat_meters
    return np.linspace(args.lat_min, args.lat_max, num_of_steps)


def get_lng_linspace():
    """Return evenly spaced longitudes."""
    coords_1 = (args.lat_min, args.lng_min)
    coords_2 = (args.lat_min, args.lng_max)
    dist = geopy.distance.vincenty(coords_1, coords_2).km * 1000
    if dist < args.lng_meters:
        raise Exception("lng_meters is too large compared with given lng_min and lng_max")
    if args.lat_meters == 0:
        raise Exception("lng_meters cannot be 0")

    num_of_steps = dist / args.lng_meters
    return np.linspace(args.lng_min, args.lng_max, num_of_steps)


def main():
    load_polygons()
    generate_fake_stations()


if __name__ == '__main__':
    main()
