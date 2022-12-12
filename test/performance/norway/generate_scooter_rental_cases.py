#! /usr/bin/python3

import csv

fieldnames = ["testCaseId", "description", "departure", "arrival", "window", "origin", "fromPlace", "fromLat", "fromLon", "destination", "toPlace", "toLat", "toLon", "category", "modes"]

locations= [
    {
        "coordinates":  "59.90947/10.78350",
        "name": "Jordal"
    },
    {
        "coordinates":  "59.9204/10.7866",
        "name": "Lille Tøyen"
    },
    {
        "coordinates":  "59.9211/10.7167",
        "name": "Briskeby"
    },
    {
        "coordinates":  "59.9109/10.7423",
        "name": "Kvadraturen"
    },
    {
        "coordinates":  "59.94770/10.74223",
        "name": "Bergskogen"
    },
    {
        "coordinates":  "59.9388/10.8012",
        "name": "Refstad"
    },
    {
        "coordinates":  "59.9463/10.6263",
        "name": "Eiksmarka"
    }
]

rows = []

def parse_coords(input):
    split = input.split("/")
    return {
        "lat": split[0],
        "lon": split[1]
    }

counter = 200
for start in locations:

    for end in locations:

        if end["coordinates"] is not start["coordinates"]:

            start_coords = parse_coords(start["coordinates"])
            end_coords = parse_coords(end["coordinates"])


            counter = counter + 1

            rows.append({
                "testCaseId": counter,
                "description": f'{start["name"]} to {end["name"]}',
                "departure": "10:00",
                "fromLat": start_coords["lat"],
                "fromLon": start_coords["lon"],
                "toLat": end_coords["lat"],
                "toLon": end_coords["lon"],
                "origin": start["name"],
                "destination": end["name"],
                "modes": "WALK|SCOOTER_RENT",
                "category": "scooter-rent"
            })

with open('travelSearch-scooter-rent.csv', 'w', encoding='UTF8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(rows)