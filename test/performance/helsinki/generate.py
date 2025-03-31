#! /usr/bin/python3


# Total times(ms)     : [329 | 443 | 253 | 436 | 222 | 318 | 393 | 295 | 445 | 288 | 340 | 360 | 189 | 274 | 231 | 219 | 325 | 189 | 156 | 209 | 286 | 319 | 218 | 154 | 269 | 229 | 288 | 302 | 232 | 208]
# Successful searches : 0 / 30
# Sample              : 4 / 4
# Time total          : 8.4 seconds
# !!! UNEXPECTED RESULTS: 30 OF 30 FAILED. SEE LOG ABOVE FOR ERRORS !!!
# 
# Worker: 
#  ==> multi_criteria_destination : [   83,   75,   74,   74 ] Avg: 76.5  (σ=3.8)
# 
# Total:  
#  ==> multi_criteria_destination : [  318,  288,  286,  280 ] Avg: 293.0  (σ=14.7)
# 17:23:14.926 INFO [org.opentripplanner.transit.speed_test.SpeedTest.main()] (AbstractCsvFile.java:92) INFO - New CSV file with is saved to '/home/henrik/hsl/OpenTripPlanner/test/performance/helsinki/travelSearch-results-md.csv'.
# 
# SpeedTest done! Version: 2.7.0-SNAPSHOT, ser.ver.id: 177, commit: 5ab75af0edb3685aaf9f6e8ad6d02b14d5560885, branch: performance-tests
# [INFO] ------------------------------------------------------------------------
# [INFO] BUILD SUCCESS
# [INFO] ------------------------------------------------------------------------
# [INFO] Total time:  55.217 s
# [INFO] Finished at: 2025-02-21T17:23:14+02:00
# [INFO] ------------------------------------------------------------------------
# 17:23:14.948 INFO [server-shutdown-info] (OtpStartupInfo.java:48) OTP SHUTTING DOWN - Run Speed Test - Version: 2.7.0-SNAPSHOT, ser.ver.id: 177, commit: 5ab75af0edb3685aaf9f6e8ad6d02b14d5560885, branch: performance-tests


import csv

fieldnames = ["testCaseId", "description", "departure", "fromLat", "fromLon", "toLat", "toLon", "origin", "destination", "modes", "category"]

locations= [

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
    #Airpot
    {
            "coordinates":  "60.317508, 24.969089",
            "name": "Airport"
    },
    #Kirkkonummi
    {
                "coordinates":  "60.12640, 24.43613",
                "name": "Kirkkonummi"
    }
]



failing_cases = [27, 66, 105, 144, 159, 183, 222, 300, 339, 420, 459, 498, 537]


rows = []

modes = [{
        "mode": "TRANSIT|WALK",
        "category": "transit",
        }
        #,
        #{
         #   "mode": "WALK|SCOOTER_RENT",
         #   "category": "scooter"
        #}
        ]


departure_times = ["06:00","13:00","22:00"]

def parse_coords(input):
    split = input.split(",")
    return {
        "lat": split[0],
        "lon": split[1]
    }

counter = 0
for mode in modes:
    for start in locations:

        for end in locations:

            if end["coordinates"] is not start["coordinates"]:

                start_coords = parse_coords(start["coordinates"])
                end_coords = parse_coords(end["coordinates"])


                for departure in departure_times:
                    counter = counter + 1
                    if counter not in failing_cases:

                        rows.append({
                            "testCaseId": counter,
                            "description": f'{start["name"]} to {end["name"]} ({mode["category"]})',
                            "departure": departure,
                            "fromLat": start_coords["lat"],
                            "fromLon": start_coords["lon"],
                            "toLat": end_coords["lat"],
                            "toLon": end_coords["lon"],
                            "origin": start["name"],
                            "destination": end["name"],
                            "modes": mode["mode"],
                            "category": mode["category"]
                        })

print(counter)
with open('travelSearch.csv', 'w', encoding='UTF8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(rows)