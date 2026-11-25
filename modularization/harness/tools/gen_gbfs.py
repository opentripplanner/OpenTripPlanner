#!/usr/bin/env python3
"""Generate the oslo-rental dataset: GBFS feeds in Oslo, the dataset.json and the scenarios.

Four synthetic networks, located so they link to the Oslo OSM extract:
  bysykkel      GBFS 2.3, stations
  scooters      GBFS 2.3, free-floating vehicles with geofencing
  citybikes-v3  GBFS 3.0, stations
  floaters-v3   GBFS 3.0, free-floating vehicles with geofencing

Two update steps change the feeds after the first capture:
  rental-stations  station counts and flags change
  rental-vehicles  vehicles are moved, disabled, removed and added, a geofencing zone is added

Feed discovery files contain ${workDir}, which the harness replaces with the work directory.
"""
import json
import shutil
from pathlib import Path

DS = Path(__file__).resolve().parent.parent / "datasets/oslo-rental"
FEEDS = DS / "gbfs"
OSM = "application/src/test/resources/org/opentripplanner/oslo-east-filtered.osm.pbf"
REL = "modularization/harness/datasets/oslo-rental/gbfs"

# Points on streets near the city centre (taken from the NeTEx stops of the oslo dataset)
JERN = (59.91225, 10.75014)
CENTER = [
    (59.91225, 10.75014), (59.91233, 10.74964), (59.90980, 10.74806),
    (59.91146, 10.74976), (59.91340, 10.75400), (59.93057, 10.71432),
    (59.91700, 10.74900), (59.91600, 10.75800),
]


def w(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def gbfs_index(version, net, feeds):
    urls = [{"name": f, "url": f"file://${{workDir}}/gbfs/{net}/{f}.json"} for f in feeds]
    if version == "2.3":
        return {"last_updated": 1700000000, "ttl": 0, "version": "2.3", "data": {"en": {"feeds": urls}}}
    return {"last_updated": "2025-11-05T12:00:00+00:00", "ttl": 0, "version": "3.0", "data": {"feeds": urls}}


def envelope(version, data, ttl=0):
    if version == "2.3":
        return {"last_updated": 1700000000, "ttl": ttl, "version": "2.3", "data": data}
    return {"last_updated": "2025-11-05T12:00:00+00:00", "ttl": ttl, "version": "3.0", "data": data}


def name(version, text):
    return text if version == "2.3" else [{"text": text, "language": "en"}]


def system_information(version, net, title):
    d = {"system_id": net, "name": name(version, title), "timezone": "Europe/Oslo"}
    if version == "2.3":
        d["language"] = "en"
    return envelope(version, d)


def vehicle_types(version, types):
    return envelope(version, {"vehicle_types": types})


def station_information(version, stations):
    return envelope(version, {"stations": [
        {"station_id": sid, "name": name(version, n), "lat": lat, "lon": lon, "capacity": cap}
        for sid, n, (lat, lon), cap in stations]})


def station_status(version, rows):
    """rows: (station_id, bikes, docks, renting, returning)"""
    out = []
    for sid, bikes, docks, renting, returning in rows:
        r = {"station_id": sid, "num_docks_available": docks,
             "is_installed": True, "is_renting": renting, "is_returning": returning}
        if version == "2.3":
            r.update({"num_bikes_available": bikes, "num_bikes_disabled": 0,
                      "is_installed": 1, "is_renting": int(renting), "is_returning": int(returning),
                      "last_reported": 1700000000})
        else:
            r.update({"num_vehicles_available": bikes, "last_reported": "2025-11-05T12:00:00+00:00"})
        out.append(r)
    return envelope(version, {"stations": out})


def vehicles(version, rows):
    """rows: (id, (lat, lon), type_id, range, disabled)"""
    items = []
    for vid, (lat, lon), tid, rng, disabled in rows:
        v = {"lat": lat, "lon": lon, "is_reserved": False, "is_disabled": disabled,
             "vehicle_type_id": tid, "current_range_meters": rng}
        v["bike_id" if version == "2.3" else "vehicle_id"] = vid
        items.append(v)
    return envelope(version, {"bikes" if version == "2.3" else "vehicles": items})


def square(lat, lon, d=0.0015):
    ring = [[lon - d, lat - d], [lon + d, lat - d], [lon + d, lat + d], [lon - d, lat + d], [lon - d, lat - d]]
    return {"type": "MultiPolygon", "coordinates": [[ring]]}


def geofencing(version, zones, type_ids):
    """zones: (name, (lat, lon), allowed_to_end)"""
    feats = []
    for n, (lat, lon), end_ok in zones:
        if version == "2.3":
            rule = {"vehicle_type_id": type_ids, "ride_allowed": end_ok, "ride_through_allowed": True}
            props = {"name": n, "rules": [rule]}
        else:
            rule = {"vehicle_type_ids": type_ids, "ride_start_allowed": True, "ride_end_allowed": end_ok,
                    "ride_through_allowed": True}
            props = {"name": name(version, n), "rules": [rule]}
        feats.append({"type": "Feature", "geometry": square(lat, lon), "properties": props})
    if version == "2.3":
        glob = [{"ride_allowed": True, "ride_through_allowed": True}]
    else:
        glob = [{"ride_start_allowed": True, "ride_end_allowed": True, "ride_through_allowed": True}]
    return envelope(version, {"geofencing_zones": {"type": "FeatureCollection", "features": feats},
                              "global_rules": glob})


BIKE_V2 = [{"vehicle_type_id": "bike", "form_factor": "bicycle", "propulsion_type": "human", "name": "City bike"}]
SCOOTER_V2 = [{"vehicle_type_id": "scooter", "form_factor": "scooter_standing", "propulsion_type": "electric",
               "max_range_meters": 30000, "name": "E-scooter"}]
BIKE_V3 = [{"vehicle_type_id": "bike3", "form_factor": "bicycle", "propulsion_type": "human",
            "name": [{"text": "City bike", "language": "en"}]}]
FLOAT_V3 = [{"vehicle_type_id": "float3", "form_factor": "scooter_standing", "propulsion_type": "electric",
             "max_range_meters": 40000, "name": [{"text": "E-scooter", "language": "en"}]}]


def networks(step):
    """All feed files of every network in the given state: 0 = initial, 1 = after stations, 2 = after vehicles."""
    c = CENTER
    nets = {}

    # bysykkel: GBFS 2.3 stations
    st = [("b1", "Jernbanetorget", c[0], 20), ("b2", "Brugata", c[1], 15), ("b3", "Majorstuen", c[2], 10),
          ("b4", "Torggata", c[3], 12), ("b5", "Vaterland", c[4], 18), ("b6", "Kjelsås", c[5], 16)]
    counts = {0: [8, 5, 0, 12, 3, 16], 1: [0, 5, 2, 12, 9, 0]}[min(step, 1)]
    status = [(sid, n, cap - n, True, True) for (sid, _, _, cap), n in zip(st, counts)]
    if step >= 1:
        status[3] = ("b4", 12, 0, False, False)  # closed
    nets["bysykkel"] = {
        "version": "2.3",
        "feeds": {
            "system_information": system_information("2.3", "bysykkel", "Oslo city bikes"),
            "vehicle_types": vehicle_types("2.3", BIKE_V2),
            "station_information": station_information("2.3", [(s, n, p, cap) for s, n, p, cap in st]),
            "station_status": station_status("2.3", status),
        },
    }

    # citybikes-v3: GBFS 3.0 stations
    st3 = [("c1", "Frogner plass", (59.9215, 10.7120), 14), ("c2", "Solli", (59.9145, 10.7260), 10),
           ("c3", "Nationaltheatret", (59.9142, 10.7350), 22)]
    cnt3 = {0: [6, 2, 11], 1: [1, 2, 20]}[min(step, 1)]
    nets["citybikes-v3"] = {
        "version": "3.0",
        "feeds": {
            "system_information": system_information("3.0", "citybikes-v3", "Oslo city bikes v3"),
            "vehicle_types": vehicle_types("3.0", BIKE_V3),
            "station_information": station_information("3.0", [(s, n, p, cap) for s, n, p, cap in st3]),
            "station_status": station_status("3.0", [(s, n, cap - n, True, True) for (s, _, _, cap), n in zip(st3, cnt3)]),
        },
    }

    # scooters: GBFS 2.3 free floating + geofencing
    rows = [("s1", c[0], "scooter", 12000, False), ("s2", c[1], "scooter", 20000, False),
            ("s3", c[2], "scooter", 8000, False), ("s4", c[4], "scooter", 25000, False),
            ("s5", c[6], "scooter", 15000, False)]
    zones = [("No parking Kjelsås", (59.9300, 10.7147), False)]
    if step >= 2:
        rows[0] = ("s1", (59.9131, 10.7466), "scooter", 11000, False)   # moved
        rows[1] = ("s2", c[1], "scooter", 20000, True)                  # disabled
        rows.pop(2)                                                     # removed
        rows.append(("s6", c[7], "scooter", 30000, False))              # added
        zones.append(("No parking Majorstuen", c[2], False))
    nets["scooters"] = {
        "version": "2.3",
        "feeds": {
            "system_information": system_information("2.3", "scooters", "Oslo scooters"),
            "vehicle_types": vehicle_types("2.3", SCOOTER_V2),
            "free_bike_status": vehicles("2.3", rows),
            "geofencing_zones": geofencing("2.3", zones, ["scooter"]),
        },
    }

    # floaters-v3: GBFS 3.0 free floating + geofencing
    rows3 = [("f1", c[3], "float3", 18000, False), ("f2", c[5], "float3", 22000, False),
             ("f3", c[6], "float3", 5000, False)]
    zones3 = [("Drop-off forbidden Torggata", c[3], False)]
    if step >= 2:
        rows3[0] = ("f1", (59.9120, 10.7600), "float3", 17000, False)
        rows3.pop(2)
        rows3.append(("f4", c[0], "float3", 40000, False))
        zones3.append(("Drop-off forbidden Grønland", c[5], False))
    nets["floaters-v3"] = {
        "version": "3.0",
        "feeds": {
            "system_information": system_information("3.0", "floaters-v3", "Oslo floating scooters v3"),
            "vehicle_types": vehicle_types("3.0", FLOAT_V3),
            "vehicle_status": vehicles("3.0", rows3),
            "geofencing_zones": geofencing("3.0", zones3, ["float3"]),
        },
    }
    return nets


def write_feeds():
    shutil.rmtree(FEEDS, ignore_errors=True)
    state = {s: networks(s) for s in (0, 1, 2)}
    files = {}
    for step, nets in state.items():
        for net, spec in nets.items():
            for feed, content in spec["feeds"].items():
                target = FEEDS / ("initial" if step == 0 else f"step-{step}") / net / f"{feed}.json"
                prev = state[step - 1][net]["feeds"][feed] if step else None
                if step == 0 or content != prev:
                    w(target, content)
                    files.setdefault(step, []).append((net, feed))
        if step == 0:
            for net, spec in nets.items():
                w(FEEDS / "initial" / net / "gbfs.json", gbfs_index(spec["version"], net, list(spec["feeds"])))
    return state, files


def write_dataset(state, files):
    inputs = [{"from": OSM}]
    for net, spec in state[0].items():
        inputs.append({"from": f"{REL}/initial/{net}/gbfs.json", "as": f"gbfs/{net}/gbfs.json", "substitute": True})
        for feed in spec["feeds"]:
            inputs.append({"from": f"{REL}/initial/{net}/{feed}.json", "as": f"gbfs/{net}/{feed}.json"})
    steps = []
    for step, name_ in ((1, "rental-stations"), (2, "rental-vehicles")):
        drops = [{"from": f"{REL}/step-{step}/{net}/{feed}.json", "toDir": f"gbfs/{net}"} for net, feed in files[step]]
        steps.append({"name": name_, "awaitDir": "", "settleMillis": 4000, "drops": drops})
    updaters = []
    for net, spec in state[0].items():
        u = {"type": "vehicle-rental", "sourceType": "gbfs", "feedId": net, "frequency": "PT1S",
             "url": f"file://${{workDir}}/gbfs/{net}/gbfs.json"}
        if "geofencing_zones" in spec["feeds"]:
            u["geofencing"] = {"enabled": True}
        updaters.append(u)
    dataset = {"inputs": inputs,
               "buildConfig": {"osmDefaults": {"timeZone": "Europe/Oslo"}},
               "routerConfig": {"updaters": updaters},
               "steps": steps}
    w(DS / "dataset.json", dataset)


STATIONS_Q = ("{ vehicleRentalStations { stationId name lat lon allowDropoff allowPickup allowDropoffNow "
              "allowPickupNow operative capacity availableVehicles { total } availableSpaces { total } "
              "rentalNetwork { networkId } } }")
VEHICLES_Q = ("{ rentalVehicles { vehicleId name lat lon allowPickupNow operative rentalNetwork { networkId } "
              "vehicleType { formFactor propulsionType } fuel { percent range } } }")
TM_STATIONS_Q = "{ bikeRentalStations { id name latitude longitude bikesAvailable spacesAvailable realtimeOccupancyAvailable } }"


def plan(mode, o, d):
    return (f"{{ planConnection(origin:{{location:{{coordinate:{{latitude:{o[0]},longitude:{o[1]}}}}}}}, "
            f"destination:{{location:{{coordinate:{{latitude:{d[0]},longitude:{d[1]}}}}}}}, "
            f'dateTime:{{earliestDeparture:"2023-06-14T10:00:00+02:00"}}, first:3, '
            f"modes:{{direct:[WALK, {mode}], directOnly:true}}) {{ routingErrors {{ code inputField }} edges {{ node {{ "
            "start end duration generalizedCost legs { mode rentedBike distance duration "
            "from { name vehicleRentalStation { stationId } rentalVehicle { vehicleId } } "
            "to { name vehicleRentalStation { stationId } rentalVehicle { vehicleId } } } } } } }")


def tm_trip(mode, o, d):
    return (f"{{ trip(from:{{coordinates:{{latitude:{o[0]},longitude:{o[1]}}}}}, "
            f"to:{{coordinates:{{latitude:{d[0]},longitude:{d[1]}}}}}, dateTime:\"2023-06-14T10:00:00+02:00\", "
            f"numTripPatterns:3, modes:{{directMode:{mode}, transportModes:[]}}) {{ routingErrors {{ code }} "
            "tripPatterns { startTime endTime duration legs { mode distance duration "
            "fromPlace { name } toPlace { name } } } } }")


def write_scenarios():
    shutil.rmtree(DS / "scenarios", ignore_errors=True)
    A, B = (59.9124, 10.7503), (59.9303, 10.7148)   # near b1 and b6, 3 km apart
    C_, D_ = (59.9216, 10.7125), (59.9143, 10.7348)  # near c1 and c3
    S1, S2 = (59.9124, 10.7500), (59.9301, 10.7147)  # near scooters, 3 km apart
    scen = {
        "gtfs-rental-stations": {"api": "gtfs", "query": STATIONS_Q, "sortArraysBy": "stationId"},
        "gtfs-rental-vehicles": {"api": "gtfs", "query": VEHICLES_Q, "sortArraysBy": "vehicleId"},
        "gtfs-plan-bicycle-rental": {"api": "gtfs", "query": plan("BICYCLE_RENTAL", A, B)},
        "gtfs-plan-bicycle-rental-v3": {"api": "gtfs", "query": plan("BICYCLE_RENTAL", C_, D_)},
        "gtfs-plan-scooter-rental": {"api": "gtfs", "query": plan("SCOOTER_RENTAL", S1, S2)},
        "tm-rental-stations": {"api": "transmodel", "query": TM_STATIONS_Q, "sortArraysBy": "id"},
        "tm-trip-bicycle-rental": {"api": "transmodel", "query": tm_trip("bike_rental", A, B)},
        "tm-trip-scooter-rental": {"api": "transmodel", "query": tm_trip("scooter_rental", S1, S2)},
    }
    for phase in ("", "after-rental-stations", "after-rental-vehicles"):
        for n, s in scen.items():
            w(DS / "scenarios" / phase / f"{n}.json", s)


state, files = write_feeds()
write_dataset(state, files)
write_scenarios()
print("steps:", {k: len(v) for k, v in files.items()})
