import csv

fieldnames = ["testCaseId", "description", "departure", "fromLat",
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
    # Airpot
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

scooter_locations = [
    {
        "coordinates": "60.169665, 24.934652",
        "name": "Kamppi"
    },
    {
        "coordinates":  "60.179022, 24.924151",
        "name": "Töölöntori"
    },
    {
        "coordinates":  "60.18234, 24.82531",
        "name": "Otaniemi"
    },
    {
        "coordinates":  "60.198349, 24.875654",
        "name": "Munkkiniemi"
    },
    {
        "coordinates":  "60.199712, 24.939437",
        "name": "Pasila"
    },
    {
        "coordinates":  "60.155454, 24.945134",
        "name": "Eira"
    },
    {
        "coordinates":  "60.184945,24.982309",
        "name": "Kalasatama"
    },
    {
        "coordinates":  "60.15654,24.86665",
        "name": "Lauttasaari"
    },
    {
        "coordinates":  "60.18534,25.00162",
        "name": "Kulosaari"
    },
]
departure_times = ["06:00", "13:00", "22:00"]


failing_cases = [27, 66, 105, 144, 159, 183, 222,
                 300, 339, 420, 459, 498, 537, 566, 568, 570]


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
            for departure in departure_times:
                counter = counter + 1
                if counter in failing_cases:
                    print("FAILED: ", start["name"],
                          " ", end["name"],  " ", departure)
                else:
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
# Generate scooter test
for start in scooter_locations:
    for end in scooter_locations:
        if end["coordinates"] != start["coordinates"]:
            start_coords = parse_coords(start["coordinates"])
            end_coords = parse_coords(end["coordinates"])
            departure = departure_times[0]  # Use just the first time
            counter += 1
            if counter in failing_cases:
                print("FAILED:", start["name"], "→",
                      end["name"], "@", departure, "(scooter)")
            else:
                rows.append({
                    "testCaseId": counter,
                    "description": f'{start["name"]} to {end["name"]} (scooter)',
                    "departure": departure,
                    "fromLat": start_coords["lat"],
                    "fromLon": start_coords["lon"],
                    "toLat": end_coords["lat"],
                    "toLon": end_coords["lon"],
                    "origin": start["name"],
                    "destination": end["name"],
                    "modes": "SCOOTER_RENT|WALK",
                    "category": "scooter"
                })

print(counter)
with open('travelSearch.csv', 'w', encoding='UTF8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(rows)
