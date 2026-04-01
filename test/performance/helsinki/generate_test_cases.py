import csv

fieldnames = ["testCaseId", "description", "departure", "arrival", "fromLat",
              "fromLon", "toLat", "toLon", "origin", "destination", "modes", "category",
              "viaLabel", "viaMinimumWaitTime", "viaLat", "viaLon"]

locations = [
    # Helsinki
    {
        "coordinates": "60.169665,24.934652",
        "name": "Kamppi"
    },
    {
        "coordinates":  "60.179022,24.924151",
        "name": "Töölöntori"
    },
    {
        "coordinates":  "60.22070,24.86094",
        "name": "Pitäjänmäki"
    },
    {
        "coordinates":  "60.20863,25.07946",
        "name": "Itäkeskus"
    },
    {
        "coordinates":  "60.148237,24.985142",
        "name": "Suomenlinna"
    },
    # Espoo
    {
        "coordinates":  "60.18234,24.82531",
        "name": "Otaniemi"
    },
    {
        "coordinates":  "60.205940,24.656711",
        "name": "Espoon Keskusta"
    },
    {
        "coordinates":  "60.15509,24.74546",
        "name": "Matinkylä"
    },
    {
        "coordinates":  "60.19586,24.58912",
        "name": "Kauklahti"
    },
    {
        "coordinates":  "60.29030,24.56324",
        "name": "Nuuksio"
    },
    {
        "coordinates":  "60.17892,24.65813",
        "name": "Latokaski"
    },
    # Vantaa
    {
        "coordinates":  "60.29246,25.03861",
        "name": "Tikkurila"
    },
    # Airport
    {
        "coordinates":  "60.317508,24.969089",
        "name": "Airport"
    },
    # Kirkkonummi
    {
        "coordinates":  "60.12640,24.43613",
        "name": "Kirkkonummi"
    }
]

complicated_area_locations = [
    # Helsinki
    {
        "coordinates": "60.17188,24.93951",
        "name": "Elielinaukio"
    },
    {
        "coordinates": "60.16758,24.95410",
        "name": "Kauppatori"
    },
    {
        "coordinates": "60.16968,24.93486",
        "name": "Narinkkatori"
    },
    # Espoo
    {
        "coordinates": "60.17685,24.80548",
        "name": "Tapionraitti"
    },
    # Kauniainen
    {
        "coordinates": "60.21073,24.72707",
        "name": "Thurmaninaukio"
    },
    # Kirkkonummi
    {
        "coordinates": "60.11974,24.43999",
        "name": "Asema-aukio"
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

# depart-after
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
                "category": "depart-after"
            })

# arrive-by
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
                "category": "arrive-by"
            })

# complicated-area
for start in complicated_area_locations:
    for end in complicated_area_locations:
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
                "category": "complicated-area"
            })

# viapoint
via_start = complicated_area_locations[0]
via_end = complicated_area_locations[1]
via_start_coords = parse_coords(via_start["coordinates"])
via_end_coords = parse_coords(via_end["coordinates"])
via_locations = locations
for via_location in via_locations:
    via_coords = parse_coords(via_location["coordinates"])

    counter = counter + 1
    rows.append({
        "testCaseId": counter,
        "description": f'{via_start["name"]} to {via_end["name"]} (transit)',
        "departure": departure,
        "fromLat": via_start_coords["lat"],
        "fromLon": via_start_coords["lon"],
        "toLat": via_end_coords["lat"],
        "toLon": via_end_coords["lon"],
        "origin": via_start["name"],
        "destination": via_end["name"],
        "modes": "TRANSIT|WALK",
        "category": "viapoint",
        "viaLabel": via_location["name"],
        "viaMinimumWaitTime": "60s",
        "viaLat": via_coords["lat"],
        "viaLon": via_coords["lon"]
    })

with open('travelSearch.csv', 'w', encoding='UTF8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(rows)
