#! /usr/bin/python3

import csv

fieldnames = ["testCaseId", "description", "departure", "fromPlace", "toPlace", "fromLat", "fromLon", "toLat", "toLon", "origin", "destination", "modes", "category"]

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
        "coordinates":  "47.187262622,-122.166767120361",
        "name": "Bonney Lake"
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

rows = []

modes = [
    {
        "mode": "TRANSIT,WALK",
        "category": "transit"
    },
    {
        "mode": "FLEX_ACCESS,FLEX_EGRESS,TRANSIT",
        "category": "flex"
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