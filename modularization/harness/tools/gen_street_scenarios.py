#!/usr/bin/env python3
"""Generate street routing scenarios for an OSM-based dataset.

usage: gen_street_scenarios.py <osm.pbf> <dataset-dir> [--date 2023-06-14] [--tz +02:00]

Picks routable points from the OSM file with osmium (the first node of ways that can be walked,
sorted by position, evenly spaced) and writes scenarios that route between them for walking,
bicycle and car, in both the GTFS and the Transmodel API. The selection is deterministic, so
running it twice on the same file gives the same scenarios. Needs `osmium` on the PATH.
"""
import json
import subprocess
import sys
from pathlib import Path

WAY_TYPES = {"residential", "tertiary", "secondary", "primary", "unclassified", "service",
             "footway", "cycleway", "path", "pedestrian", "living_street", "track"}
POINTS = 8


def pick_points(pbf):
    out = subprocess.run(
        ["osmium", "export", "-f", "geojsonseq", "--geometry-types=linestring", pbf],
        check=True, capture_output=True, text=True).stdout
    pts = set()
    for line in out.splitlines():
        line = line.lstrip("\x1e").strip()
        if not line:
            continue
        f = json.loads(line)
        if f.get("properties", {}).get("highway") in WAY_TYPES:
            coords = f["geometry"]["coordinates"]
            lon, lat = coords[len(coords) // 2]
            pts.add((round(lat, 5), round(lon, 5)))
    pts = sorted(pts)
    step = max(1, len(pts) // POINTS)
    return pts[::step][:POINTS]


def write(dataset_dir, name, scenario):
    path = Path(dataset_dir) / "scenarios" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(scenario, indent=2) + "\n")


def gtfs_query(pairs, mode, date, tz):
    parts = []
    for i, (a, b) in enumerate(pairs):
        parts.append(
            f'r{i}: planConnection(origin:{{location:{{coordinate:{{latitude:{a[0]},longitude:{a[1]}}}}}}}, '
            f'destination:{{location:{{coordinate:{{latitude:{b[0]},longitude:{b[1]}}}}}}}, '
            f'dateTime:{{earliestDeparture:"{date}T10:00:00{tz}"}}, first:2, '
            f'modes:{{direct:[{mode}], directOnly:true}}) '
            '{ routingErrors { code inputField } edges { node { start end duration generalizedCost '
            'walkDistance elevationGained elevationLost legs { mode distance duration '
            'legGeometry { length } steps { streetName relativeDirection absoluteDirection distance '
            'lat lon elevationProfile { distance elevation } } } } } }')
    return "{ " + " ".join(parts) + " }"


def transmodel_query(pairs, mode, date, tz):
    parts = []
    for i, (a, b) in enumerate(pairs):
        parts.append(
            f'r{i}: trip(from:{{coordinates:{{latitude:{a[0]},longitude:{a[1]}}}}}, '
            f'to:{{coordinates:{{latitude:{b[0]},longitude:{b[1]}}}}}, dateTime:"{date}T10:00:00{tz}", '
            f'numTripPatterns:2, modes:{{directMode:{mode}, transportModes:[]}}) '
            '{ routingErrors { code } tripPatterns { startTime endTime duration generalizedCost '
            'streetDistance legs { mode distance duration '
            'pointsOnLink { length } } } }')
    return "{ " + " ".join(parts) + " }"


def main():
    pbf, dataset_dir = sys.argv[1], sys.argv[2]
    date = "2023-06-14"
    tz = "+02:00"
    args = sys.argv[3:]
    if "--date" in args:
        date = args[args.index("--date") + 1]
    if "--tz" in args:
        tz = args[args.index("--tz") + 1]
    pts = pick_points(pbf)
    half = len(pts) // 2
    pairs = list(zip(pts[:half], pts[half:]))
    for gtfs_mode, tm_mode in [("WALK", "foot"), ("BICYCLE", "bicycle"), ("CAR", "car")]:
        write(dataset_dir, f"street-{gtfs_mode.lower()}",
              {"api": "gtfs", "query": gtfs_query(pairs, gtfs_mode, date, tz)})
        write(dataset_dir, f"tm-street-{gtfs_mode.lower()}",
              {"api": "transmodel", "query": transmodel_query(pairs, tm_mode, date, tz)})
    write(dataset_dir, "gtfs-vehicle-parkings",
          {"api": "gtfs", "sortArraysBy": "vehicleParkingId",
           "unordered": ["data.vehicleParkings[*].tags"],
           "query": "{ vehicleParkings { vehicleParkingId name lat lon bicyclePlaces carPlaces "
                    "tags capacity { bicycleSpaces carSpaces wheelchairAccessibleCarSpaces } } }"})
    print(f"{len(pairs)} routes per mode, from {len(pts)} candidate points")


main()
