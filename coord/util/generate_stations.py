#!/usr/bin/env python

"""
This script reads GBFS files and generates fake bike stations (hubs).

usage: generate_stations.py [-h]
                        gbfs_dir lng_min lng_max lat_min lat_max lat_meters
                        lng_meters

positional arguments:
  gbfs_dir    The path to the GBFS directory.
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
import time

from geojson import Feature
from geojson import FeatureCollection
from geojson import Point
from json import encoder

# To avoid long float numbers for lat/lng
encoder.FLOAT_REPR = lambda o: format(o, '3.6f')

parser = argparse.ArgumentParser()
parser.add_argument('gbfs_dir', type=str, help='The path to the GBFS directory.')
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


def get_system_id(gbfs_dir):
    """Reads the GBFS system ID from system_information.json

    :param gbfs_dir: path to a GBFS folder
    :return: string, system_id
    """
    system = json.load(open(gbfs_dir + '/system_information.json'))
    return system['data']['system_id']


def create_status(system_id):
    return {
        "station_id": system_id,
        "num_bikes_available": 0,
        "num_bikes_disabled": 0,
        "num_docks_available": 10,
        "is_installed": 1,
        "is_renting": 0,
        "is_returning": 1,
        "last_reported": int(time.time())

    }


def create_station(lon, lat, system_id):
    return {
        'station_id': system_id,
        'name': system_id,
        'region_id': "region_391",
        'lon': lon,
        'lat': lat,
        'address': system_id
    }


def generate_fake_stations():
    """Generate fake stations.

        It keeps the original stations and adds fake ones to it.
        It also keeps a copy of the original files with `.old` appended.
        It generates a geojson file of all stations.
    """
    gbfs_dir = args.gbfs_dir
    station_info_filename = gbfs_dir + '/station_information.json'
    station_status_filename = gbfs_dir + '/station_status.json'

    station_data = json.load(open(station_info_filename))
    stations = station_data['data']['stations']
    statuses_data = json.load(open(station_status_filename))
    statuses = statuses_data['data']['stations']

    system_id = get_system_id(gbfs_dir)
    features = []
    i = 0
    for lng in get_lng_linspace():
        for lat in get_lat_linspace():
            system_id = system_id + "%d" % i
            features.append(Feature(geometry=Point((lng, lat))))
            stations.append(create_station(lng, lat, system_id))
            statuses.append(create_status(system_id))
            i += 1

    time_str = time.strftime("%Y%m%d-%H%M%S")
    with open(str.replace(station_info_filename, '.json', '_%s.json' % time_str), 'w') as outfile:
        json.dump(stations, outfile, indent=2, sort_keys=True)

    with open(str.replace(station_status_filename, '.json', '_%s.json' % time_str), 'w') as outfile:
        json.dump(statuses_data, outfile, indent=2, sort_keys=True)

    with open(str.replace(gbfs_dir + '/stations-geojson.json', '.json', '_%s.json' % time_str),
              'w') as outfile:
        geojson.dump(FeatureCollection(features), outfile, indent=2, sort_keys=True)

    print('Total # of stations:', len(stations))


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
    generate_fake_stations()


if __name__ == '__main__':
    main()
