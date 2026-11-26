#!/usr/bin/env python3
"""Generate the dataset `oslo-parking`: the vehicle parking updaters of the vehicle parking sandbox.

The updaters read from HTTP only, so the harness serves <workDir>/http (see Dataset). The files
for the initial state are inputs, and each step replaces some of them. Sources:

- park-api (cars), bicycle-park-api, liipi (facilities, hubs and utilizations), bikely (POST),
  bikeep (GeoJSON), and siri-fm (only availability, for the lots of a park-api feed that is read
  once an hour).

State 0 is the initial state. Step 1 changes availability, closes lots and adds and removes lots.
Step 2 reverts some of it and changes the hubs.
"""
import json
import shutil
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
DS = HERE / "datasets/oslo-parking"
FILES = DS / "http"
PHASES = ["initial", "parking-1", "parking-2"]


def pt(lat, lon):
    return {"type": "Point", "coordinates": [lon, lat]}


def parkapi_lot(id_, name, lat, lon, total, free, *, disabled=None, state="open", hours=None,
                notes=None, extra=None):
    lot = {"id": id_, "name": name, "coords": {"lat": lat, "lng": lon}, "state": state,
           "lot_type": "Parkhaus", "address": f"{name} gate 1", "forecast": False,
           "total": total, "free": free}
    if disabled:
        lot["total:disabled"], lot["free:disabled"] = disabled
    if hours:
        lot["opening_hours"] = hours
    if notes:
        lot["notes"] = notes
    lot.update(extra or {})
    return lot


# ---- park-api (car): feed `pa`
def pa(phase):
    lots = {
        "initial": [
            parkapi_lot("pa-1", "Oslo S", 59.9110, 10.7530, 400, 120, disabled=(10, 4), hours="Mo-Fr 06:00-23:00",
                        notes={"en": "Pay at the machine", "no": "Betal i automaten"},
                        extra={"url": "https://example.com/pa-1", "image_url": "https://example.com/pa-1.jpg"}),
            parkapi_lot("pa-2", "Vaterland", 59.9138, 10.7478, 250, 0),
            parkapi_lot("pa-3", "Operaen", 59.9075, 10.7530, 300, 150, state="closed"),
            parkapi_lot("pa-4", "Grønland", 59.9120, 10.7600, 80, 40, hours="Mo-Su 00:00-24:00"),
        ],
        "parking-1": [
            parkapi_lot("pa-1", "Oslo S", 59.9110, 10.7530, 400, 35, disabled=(10, 1), hours="Mo-Fr 06:00-23:00",
                        notes={"en": "Pay at the machine", "no": "Betal i automaten"},
                        extra={"url": "https://example.com/pa-1", "image_url": "https://example.com/pa-1.jpg"}),
            parkapi_lot("pa-2", "Vaterland", 59.9138, 10.7478, 250, 90),
            parkapi_lot("pa-3", "Operaen", 59.9075, 10.7530, 300, 150),  # reopened
            # pa-4 is gone, pa-5 is new
            parkapi_lot("pa-5", "Bjørvika", 59.9085, 10.7580, 120, 60),
        ],
        "parking-2": [
            parkapi_lot("pa-1", "Oslo S", 59.9110, 10.7530, 400, 200, disabled=(10, 5), hours="Mo-Fr 06:00-23:00",
                        notes={"en": "Pay at the machine", "no": "Betal i automaten"},
                        extra={"url": "https://example.com/pa-1", "image_url": "https://example.com/pa-1.jpg"}),
            parkapi_lot("pa-2", "Vaterland", 59.9138, 10.7478, 250, 90),
            parkapi_lot("pa-3", "Operaen", 59.9075, 10.7530, 300, 10, state="closed"),
            parkapi_lot("pa-4", "Grønland", 59.9120, 10.7600, 80, 40, hours="Mo-Su 00:00-24:00"),
            parkapi_lot("pa-5", "Bjørvika", 59.9085, 10.7580, 120, 0),
        ],
    }[phase]
    return {"last_updated": "2017-12-27T12:00:00", "lots": lots}


# ---- bicycle-park-api: feed `bpa`
def bpa(phase):
    free = {"initial": (50, 30), "parking-1": (50, 5), "parking-2": (50, 50)}[phase]
    lots = [parkapi_lot("bpa-1", "Sykkelhotell", 59.9105, 10.7525, free[0], free[1])]
    if phase != "parking-1":
        lots.append(parkapi_lot("bpa-2", "Sykkelstativ Torggata", 59.9160, 10.7490, 20, 8))
    return {"lots": lots}


# ---- static park-api lots (read once an hour), updated by siri-fm: feed `st`
def static():
    return {"lots": [
        parkapi_lot("st-1", "Aker brygge", 59.9110, 10.7270, 500, 500),
        parkapi_lot("st-2", "Tjuvholmen", 59.9060, 10.7190, 200, 200),
        parkapi_lot("st-3", "Skøyen", 59.9220, 10.6800, 100, 100),
    ]}


def sirifm(phase):
    counts = {"initial": {"st-1": 100, "st-2": 20}, "parking-1": {"st-1": 3, "st-2": 20, "st-3": 99},
              "parking-2": {"st-1": 450, "st-2": 0, "st-9": 5}}[phase]
    conds = ""
    for fid, n in counts.items():
        conds += ("      <FacilityCondition>\n"
                  f"        <FacilityRef>{fid}</FacilityRef>\n"
                  "        <FacilityStatus><Status>available</Status></FacilityStatus>\n"
                  "        <MonitoredCounting>\n          <CountingType>presentCount</CountingType>\n"
                  f"          <Count>{n}</Count>\n        </MonitoredCounting>\n"
                  "      </FacilityCondition>\n")
    # One that does not follow the profile (two counts): it is ignored
    conds += ("      <FacilityCondition>\n        <FacilityRef>st-3</FacilityRef>\n"
              "        <MonitoredCounting><CountingType>presentCount</CountingType><Count>1</Count></MonitoredCounting>\n"
              "        <MonitoredCounting><CountingType>presentCount</CountingType><Count>2</Count></MonitoredCounting>\n"
              "      </FacilityCondition>\n") if phase == "parking-2" else ""
    return ('<?xml version="1.0" encoding="UTF-8"?>\n'
            '<Siri xmlns="http://www.siri.org.uk/siri" version="2.1">\n  <ServiceDelivery>\n'
            "    <ResponseTimestamp>2017-12-27T12:00:00+01:00</ResponseTimestamp>\n"
            "    <ProducerRef>HARNESS</ProducerRef>\n"
            '    <FacilityMonitoringDelivery version="2.1">\n'
            "      <ResponseTimestamp>2017-12-27T12:00:00+01:00</ResponseTimestamp>\n"
            + conds.replace("      <FacilityCondition>", "      <FacilityCondition>") +
            "    </FacilityMonitoringDelivery>\n  </ServiceDelivery>\n</Siri>\n")


# ---- liipi: feed `li`
def liipi_facility(id_, names, lat, lon, cap, *, status="IN_OPERATION", services=(), auth=(), pricing=None,
                   hours=None):
    f = {"id": id_, "name": names, "location": pt(lat, lon), "status": status,
         "builtCapacity": cap, "services": list(services), "authenticationMethods": list(auth)}
    if pricing:
        f["pricingMethod"] = pricing
    if hours:
        f["openingHours"] = {"byDayType": hours}
    return f


def li_facilities(phase):
    fs = [
        liipi_facility("f1", {"fi": "Kamppi", "sv": "Kampen", "en": "Kamppi"}, 59.9170, 10.7400,
                       {"CAR": 200, "DISABLED": 8, "BICYCLE": 40}, services=["LIGHTING", "ELEVATOR"],
                       auth=["HSL_TRAVEL_CARD"], pricing="PAID_24H",
                       hours={"BUSINESS_DAY": {"from": "05", "until": "24"}, "SATURDAY": {"from": "07:30", "until": "22"}}),
        liipi_facility("f2", {"fi": "Rautatientori", "en": "Central station"}, 59.9130, 10.7520,
                       {"BICYCLE": 100}),
        liipi_facility("f3", {}, 59.9100, 10.7450, {"CAR": 30}, status="TEMPORARILY_CLOSED"),
    ]
    if phase != "initial":
        fs[1]["status"] = "INACTIVE"
        fs.append(liipi_facility("f4", {"en": "Pasila"}, 59.9200, 10.7700, {"CAR": 60, "BICYCLE": 20},
                                 status="EXCEPTIONAL_SITUATION", pricing="FREE"))
    return {"results": fs}


def li_hubs(phase):
    hubs = [{"id": "h1", "name": {"fi": "Keskusta", "en": "Centre"}, "location": pt(59.9150, 10.7460),
             "facilityIds": ["f1", "f2"]}]
    if phase == "parking-2":
        hubs = [{"id": "h2", "name": {"en": "Other"}, "location": pt(59.9120, 10.7480),
                 "facilityIds": ["f2", "f3", "f4"]}]
    return {"results": hubs}


def li_utilizations(phase):
    n = {"initial": (120, 4, 10, 60), "parking-1": (0, 0, 35, 12), "parking-2": (199, 8, 40, 59)}[phase]
    u = [{"facilityId": "f1", "capacityType": "CAR", "spacesAvailable": n[0]},
         {"facilityId": "f1", "capacityType": "DISABLED", "spacesAvailable": n[1]},
         {"facilityId": "f1", "capacityType": "BICYCLE", "spacesAvailable": n[2]},
         {"facilityId": "f2", "capacityType": "BICYCLE", "spacesAvailable": n[3]},
         {"facilityId": "f3", "capacityType": "CAR", "spacesAvailable": 5},
         {"facilityId": "f3", "capacityType": "UNKNOWN", "spacesAvailable": 1}]
    if phase != "initial":
        u.append({"facilityId": "f4", "capacityType": "CAR", "spacesAvailable": 7})
    return u  # a plain array, other than facilities and hubs


# ---- bikely: feed `bk`
def bikely_loc(id_, name, lat, lon, doors, *, maintenance=False, start=(0, 1), main=(0, 1)):
    return {"id": id_, "name": name, "latitude": lat, "longitude": lon, "isInMaintenance": maintenance,
            "startPriceAmount": start[0], "startPriceDuration": start[1],
            "mainPriceAmount": main[0], "mainPriceDuration": main[1],
            "doorAvailibilities": [{"doorTypeName": t, "availableSpots": a, "totalSpots": tot} for t, a, tot in doors]}


def bikely(phase):
    locs = [
        bikely_loc("b1", "Bikely Storgata", 59.9140, 10.7530, [("Standard", 3, 6), ("Rack", 1, 2), ("Other", 9, 9)]),
        bikely_loc("b2", "Bikely Youngstorget", 59.9150, 10.7500, [("Standard", 0, 4)], start=(10, 4), main=(5, 24)),
        bikely_loc("b3", "Bikely no doors", 59.9160, 10.7510, []),
    ]
    if phase == "parking-1":
        locs[0] = bikely_loc("b1", "Bikely Storgata", 59.9140, 10.7530, [("Standard", 0, 6), ("Rack", 0, 2)],
                             maintenance=True)
        locs.append(bikely_loc("b4", "Bikely Nytorget", 59.9230, 10.7620, [("Standard", 5, 5)]))
    if phase == "parking-2":
        locs[1] = bikely_loc("b2", "Bikely Youngstorget", 59.9150, 10.7500, [("Standard", 4, 4)])
    return {"result": locs}


# ---- bikeep: feed `bp`
def bikeep(phase):
    def f(code, label, lat, lon, avail, total, tags):
        return {"type": "Feature", "geometry": {"type": "Point", "coordinates": [lon, lat]},
                "properties": {"code": code, "label": label, "tags": tags,
                               "parking": {"available": avail, "total": total}}}
    feats = [f("k1", "Bikeep Sentrum", 59.9125, 10.7405, 10, 20, ["covered", "lockers"]),
             f("k2", "Bikeep Grünerløkka", 59.9230, 10.7580, 0, 12, [])]
    if phase == "parking-1":
        feats[0] = f("k1", "Bikeep Sentrum", 59.9125, 10.7405, 2, 20, ["covered", "lockers"])
        feats.append(f("k3", "Bikeep Majorstuen", 59.9280, 10.7150, 8, 8, ["covered"]))
    if phase == "parking-2":
        feats = feats[:1] + [f("k2", "Bikeep Grünerløkka", 59.9230, 10.7580, 12, 12, ["open"])]
    return {"type": "FeatureCollection", "features": feats}


SOURCES = {
    "parkapi/lots.json": pa,
    "bparkapi/lots.json": bpa,
    "sirifm/parking.xml": sirifm,
    "liipi/facilities.json": li_facilities,
    "liipi/hubs.json": li_hubs,
    "liipi/utilizations.json": li_utilizations,
    "bikely/locations.json": bikely,
    "bikeep/locations.json": bikeep,
}


def write_files():
    if FILES.exists():
        shutil.rmtree(FILES)
    for phase in PHASES:
        for rel, fn in SOURCES.items():
            p = FILES / phase / rel
            p.parent.mkdir(parents=True, exist_ok=True)
            v = fn(phase)
            p.write_text(v if isinstance(v, str) else json.dumps(v, indent=2, ensure_ascii=False) + "\n")
    p = FILES / "static/lots.json"
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(static(), indent=2, ensure_ascii=False) + "\n")


def updater(source, feed, url, **kw):
    u = {"type": "vehicle-parking", "sourceType": source, "feedId": feed, "url": "${httpBase}/" + url}
    if source in ("park-api", "bicycle-park-api"):
        u["timeZone"] = "Europe/Oslo"
    u.update(kw)
    return u


def write_dataset():
    root = "modularization/harness/datasets/oslo-parking/http"
    inputs = [{"from": "application/src/test/resources/org/opentripplanner/oslo-east-filtered.osm.pbf"}]
    for rel in SOURCES:
        inputs.append({"from": f"{root}/initial/{rel}", "as": f"http/{rel}"})
    inputs.append({"from": f"{root}/static/lots.json", "as": "http/static/lots.json"})
    steps = [{"name": "parking-initial", "settleMillis": 3000, "drops": []}]
    for phase in PHASES[1:]:
        steps.append({"name": phase, "settleMillis": 3000, "drops": [
            {"from": f"{root}/{phase}/{rel}", "toDir": f"http/{Path(rel).parent}"} for rel in SOURCES]})
    ds = {
        "inputs": inputs,
        "buildConfig": {},
        "routerConfig": {"updaters": [
            updater("park-api", "pa", "parkapi/lots.json", frequency="PT1S", tags=["source:parkapi"]),
            updater("bicycle-park-api", "bpa", "bparkapi/lots.json", frequency="PT1S"),
            updater("park-api", "st", "static/lots.json", frequency="PT1H"),
            updater("siri-fm", "st", "sirifm/parking.xml", frequency="PT1S"),
            {"type": "vehicle-parking", "sourceType": "liipi", "feedId": "li", "timeZone": "Europe/Oslo",
             "facilitiesUrl": "${httpBase}/liipi/facilities.json", "hubsUrl": "${httpBase}/liipi/hubs.json",
             "utilizationsUrl": "${httpBase}/liipi/utilizations.json",
             "facilitiesFrequencySec": 1, "utilizationsFrequencySec": 1},
            updater("bikely", "bk", "bikely/locations.json", frequency="PT1S"),
            updater("bikeep", "bp", "bikeep/locations.json", frequency="PT1S"),
        ]},
        "steps": steps,
    }
    (DS / "dataset.json").write_text(json.dumps(ds, indent=2) + "\n")


PARKINGS = ("{ vehicleParkings { vehicleParkingId name lat lon state realtime bicyclePlaces carPlaces anyCarPlaces "
            "wheelchairAccessibleCarPlaces tags detailsUrl imageUrl "
            "capacity { bicycleSpaces carSpaces wheelchairAccessibleCarSpaces } "
            "availability { bicycleSpaces carSpaces wheelchairAccessibleCarSpaces } "
            "note openingHours { osm } } }")
BY_ID = ('{ a: vehicleParking(id:"pa:pa-1") { name availability { carSpaces } } '
         'b: vehicleParking(id:"li:f1") { name availability { carSpaces bicycleSpaces wheelchairAccessibleCarSpaces } } '
         'c: vehicleParking(id:"st:st-1") { name availability { carSpaces } } '
         'd: vehicleParking(id:"bk:b1") { name state availability { bicycleSpaces } } '
         'e: vehicleParking(id:"pa:pa-5") { name } f: vehicleParking(id:"pa:nope") { name } '
         'm: vehicleParkings(ids:["bp:k1","bpa:bpa-1","li:h1"]) { vehicleParkingId name } }')
def plan(dest):
    """Drive from the east of Oslo, park, walk the rest. Only operational lots are used."""
    return ('{ planConnection(origin:{location:{coordinate:{latitude:59.9230,longitude:10.7700}}}, '
            f'destination:{{location:{{coordinate:{{latitude:{dest[0]},longitude:{dest[1]}}}}}}}, '
            'dateTime:{earliestDeparture:"2017-12-27T08:00:00+01:00"}, first:3, '
            'modes:{directOnly:true, direct:[CAR_PARKING]}) '
            "{ routingErrors { code } edges { node { duration legs { mode distance duration "
            "from { name vehicleParking { vehicleParkingId } } to { name vehicleParking { vehicleParkingId } } } } } } }")


PLANS = {"parking-plan-oslo-s": plan((59.9125, 10.7500)), "parking-plan-vaterland": plan((59.9138, 10.7478)),
         "parking-plan-opera": plan((59.9075, 10.7530)), "parking-plan-groenland": plan((59.9118, 10.7605))}


def write_scenarios():
    sc = {
        "parking-all": {"api": "gtfs", "query": PARKINGS, "sortArraysBy": "vehicleParkingId",
                        "unordered": ["data.vehicleParkings[*].tags"]},
        "parking-by-id": {"api": "gtfs", "query": BY_ID},
    }
    sc.update({n: {"api": "gtfs", "query": q} for n, q in PLANS.items()})
    steps = [s["name"] for s in json.loads((DS / "dataset.json").read_text())["steps"]]
    if (DS / "scenarios").exists():
        shutil.rmtree(DS / "scenarios")
    for step in steps:
        d = DS / "scenarios" / f"after-{step}"
        d.mkdir(parents=True, exist_ok=True)
        for n, s in sc.items():
            (d / f"{n}.json").write_text(json.dumps(s, indent=2) + "\n")


if __name__ == "__main__":
    DS.mkdir(parents=True, exist_ok=True)
    write_files()
    write_dataset()
    write_scenarios()
