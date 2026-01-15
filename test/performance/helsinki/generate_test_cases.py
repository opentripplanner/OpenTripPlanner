import csv

fieldnames = ["testCaseId", "description", "departure", "arrival", "fromLat",
              "fromLon", "toLat", "toLon", "origin", "destination", "modes", "category"]

locations = [
    # Helsinki
    {
        "coordinates": "60.169665, 24.934652",
        "name": "Kamppi"
    },
    {
        "coordinates":  "60.179022, 24.924151",
        "name": "Töölöntori"
    },
    {
        "coordinates":  "60.22070, 24.86094",
        "name": "Pitäjänmäki"
    },
    {
        "coordinates":  "60.20863, 25.07946",
        "name": "Itäkeskus"
    },
    {
        "coordinates":  "60.148237,24.985142",
        "name": "Suomenlinna"
    },
    # Espoo
    {
        "coordinates":  "60.18234, 24.82531",
        "name": "Otaniemi"
    },
    {
        "coordinates":  "60.205940, 24.656711",
        "name": "Espoon Keskusta"
    },
    {
        "coordinates":  "60.15509, 24.74546",
        "name": "Matinkylä"
    },
    {
        "coordinates":  "60.19586, 24.58912",
        "name": "Kauklahti"
    },
    {
        "coordinates":  "60.29030, 24.56324",
        "name": "Nuuksio"
    },
    {
        "coordinates":  "60.17892, 24.65813",
        "name": "Latokaski"
    },
    # Vantaa
    {
        "coordinates":  "60.29246, 25.03861",
        "name": "Tikkurila"
    },
    # Airport
    {
        "coordinates":  "60.317508, 24.969089",
        "name": "Airport"
    },
    # Kirkkonummi
    {
        "coordinates":  "60.12640, 24.43613",
        "name": "Kirkkonummi"
    }
]

arrival = "13:00"
departure = "13:00"

rows = []

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

            counter = counter + 1

            rows.append({
                "testCaseId": counter,
                "description": f'{start["name"]} to {end["name"]} (transit)',
                "departure": departure,
                "fromLat": start_coords["lat"],
                "fromLon": start_coords["lon"],
                "toLat": end_coords["lat"],
                "toLon": end_coords["lon"],
                "origin": start["name"],
                "destination": end["name"],
                "modes": "TRANSIT|WALK",
                "category": "transit"
            })

for start in locations:
    for end in locations:
        if end["coordinates"] is not start["coordinates"]:

            start_coords = parse_coords(start["coordinates"])
            end_coords = parse_coords(end["coordinates"])

            counter = counter + 1

            rows.append({
                "testCaseId": counter,
                "description": f'{start["name"]} to {end["name"]} (transit)',
                "arrival": arrival,
                "fromLat": start_coords["lat"],
                "fromLon": start_coords["lon"],
                "toLat": end_coords["lat"],
                "toLon": end_coords["lon"],
                "origin": start["name"],
                "destination": end["name"],
                "modes": "TRANSIT|WALK",
                "category": "transit"
            })

with open('travelSearch.csv', 'w', encoding='UTF8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(rows)
