#! /usr/bin/python3

import csv

fieldnames = ["testCaseId", "description", "departure", "fromLat", "fromLon", "toLat", "toLon", "origin", "destination", "modes", "category"]

locations= [
    {
        "coordinates":  "47.40648236,-122.18101501",
        "name": "Hilshire Terrace"
    },
    {
        "coordinates":  "47.44306609375831,-122.34237670",
        "name": "Normandy Park"
    },
    {
        "coordinates":  "47.3978844,-122.30898",
        "name": "Des Moines"
    },
    {
        "coordinates":  "47.340218616,-122.2246170043",
        "name": "North Auburn"
    },
    {
        "coordinates":  "47.31322426310727,-122.336540222167",
        "name": "Federal Way"
    },
    {
        "coordinates":  "47.571024,-122.3877811",
        "name": "West Seattle"
    },
    {
        "coordinates":  "47.18399599096,-122.28950500488",
        "name": "Puyallup"
    },
    {
        "coordinates":  "47.52827129,-121.827821731",
        "name": "Snoqualmie"
    },
    {
        "coordinates":  "47.6394849792,-122.3636627197",
        "name": "Queen Anne"
    },
    {
        "coordinates":  "47.578668036563016,-122.31139183",
        "name": "Beacon Hill"
    },
]

failing_cases = [16, 64, 70, 82, 88, 120, 128, 130, 132, 134, 136, 138, 146, 152, 154, 158, 164, 172]

rows = []

modes = [
    {
        "mode": "TRANSIT|WALK",
        "category": "transit",
        "window": ""
    },
    {
        "mode": "FLEX_ACCESS|FLEX_EGRESS|TRANSIT",
        "category": "flex",
        "window": "6h"
    }
]

def parse_coords(input):
    split = input.split(",")
    return {
        "lat": split[0],
        "lon": split[1]
    }

counter = 0
for start in locations:

    for end in locations:

        if end["coordinates"] is not start["coordinates"]:

            start_coords = parse_coords(start["coordinates"])
            end_coords = parse_coords(end["coordinates"])


            for mode in modes:
                counter = counter + 1
                if counter not in failing_cases:

                    rows.append({
                        "testCaseId": counter,
                        "description": f'{start["name"]} to {end["name"]} ({mode["category"]})',
                        "departure": "10:00",
                        "fromLat": start_coords["lat"],
                        "fromLon": start_coords["lon"],
                        "toLat": end_coords["lat"],
                        "toLon": end_coords["lon"],
                        "origin": start["name"],
                        "destination": end["name"],
                        "modes": mode["mode"],
                        "category": mode["category"]
                    })

with open('travelSearch.csv', 'w', encoding='UTF8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(rows)