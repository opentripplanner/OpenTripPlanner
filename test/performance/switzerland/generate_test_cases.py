# Need openaddress address input file
# to generate address input file:
# Download geojson file from https://results.openaddresses.io/sources/ch
# Convert geojson to csv using jq
#   echo 'city,street,number,lat,lon' > address.csv
#   zcat addresses.geojson | jq '[.properties.city, .properties.street, .properties.number, .geometry.coordinates[0], .geometry.coordinates[1]] | @csv' -r >> address.csv

import csv
import random
import haversine as hs

file = open("./address.csv", "r", encoding='utf-8-sig')
data = list(csv.DictReader(file, delimiter=","))
file.close()

f = open('travel.csv', 'w')

fieldnames = ["testCaseId", "description", "departure", "fromPlace", "toPlace", "fromLat", "fromLon", "toLat", "toLon", "origin", "destination", "modes", "category"]

rows = []

used_names = []

def get_id(stop):
    return ""

def get_address():
    stop = None
    while stop is None:
        stop = random.choice(data)
        print(stop)
    return stop

for i in range(0,100):
    start = get_address()
    end = get_address()

    loc1=(float(start["lat"]), float(start["lon"]))
    loc2=(float(end["lat"]), float(end["lon"]))
    distance = hs.haversine(loc1,loc2)

    print(distance)

    rows.append({
        "testCaseId": i,
        "description": f'{start["street"]}, {start["city"]} to {end["street"]}, {start["city"]}',
        "departure": "08:00",
        "fromPlace": get_id(start),
        "toPlace": get_id(end),
        "fromLat": start["lat"],
        "fromLon": start["lon"],
        "toLat": end["lat"],
        "toLon": end["lon"],
        "origin": start["street"],
        "destination": end["street"],
        "modes": "TRANSIT|WALK",
        "category": "transit"
    })

with open('travelSearch.csv', 'w', encoding='UTF8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(rows)