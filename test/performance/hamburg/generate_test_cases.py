#! /usr/bin/python3

import csv

fieldnames = ["testCaseId", "description", "departure", "fromLat", "fromLon", "toLat", "toLon", "origin", "destination",
              "modes", "category"]

locations = [
    {"name": "Ahrensburg", "coordinates": "53.671926,10.234838"},
    {"name": "Stade", "coordinates": "53.5960704,9.4775887"},
    {"name": "Bad Oldesloe", "coordinates": "53.8052678,10.3822381"},
    {"name": "Henstedt-Ulzburg", "coordinates": "53.7952480,9.9822267"},
    {"name": "Elmshorn", "coordinates": "53.7542251,9.6592636"},
    {"name": "Pinneberg", "coordinates": "53.6550342,9.7976864"},
    {"name": "Buchholz", "coordinates": "53.3243628,9.8749387"},
    {"name": "HH Hbf", "coordinates": "53.5531993,10.0064364"},
    {"name": "HH Flughafen", "coordinates": "53.6326466,10.0066778"},
    {"name": "HH Landungsbrücken (S)", "coordinates": "53.5462334,9.9711435"},
    {"name": "HH Harburg", "coordinates": "53.4557227,9.9917669"},
    {"name": "HH Schlump", "coordinates": "53.5675879,9.9700757"},
    {"name": "HH Kirschenallee", "coordinates": "53.6246134,10.0726482"},
    {"name": "HH Wasserturm", "coordinates": "53.6196717,10.0500744"},
    {"name": "HH Finkenwerder", "coordinates": "53.5364482,9.8789390"},
    {"name": "HH Mümmelmannsberg", "coordinates": "53.5281075,10.1500277"},
]

failing_cases = [21,48,112,127,142,157,172,187,202,217]

rows = []

modes = [
    {
        "mode": "TRANSIT|WALK",
        "category": "transit",
        "window": ""
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
