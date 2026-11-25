#!/usr/bin/env python3
"""Generate the GTFS-RT update files of the portland dataset into datasets/portland/updates/ and
the scenarios that run after each step.

The files are protobuf. The encoder is written by hand for the few messages used, so no protobuf
library is needed. Field numbers are from gtfs-realtime.proto.

Trips are real trips of the Portland feed on Tuesday 2009-11-17. Stop and trip ids have no feed
prefix; the updaters add the feed id (1). Times of day are seconds after midnight of the service
day, which is 1258444800 in epoch seconds (2009-11-17 00:00 PST).
"""
import json
import struct
from pathlib import Path

DS = Path(__file__).resolve().parent.parent / "datasets/portland"
UPDATES = DS / "updates"
DAY = 1258444800


# ---- minimal protobuf encoder -------------------------------------------------------------
def varint(n):
    if n < 0:
        n += 1 << 64
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        if n:
            out.append(b | 0x80)
        else:
            out.append(b)
            return bytes(out)


def tag(field, wire):
    return varint(field << 3 | wire)


def f_varint(field, n):
    return tag(field, 0) + varint(n)


def f_bytes(field, b):
    return tag(field, 2) + varint(len(b)) + b


def f_str(field, s):
    return f_bytes(field, s.encode())


def f_float(field, x):
    return tag(field, 5) + struct.pack("<f", x)


def msg(*parts):
    return b"".join(p for p in parts if p)


# ---- GTFS-RT messages ----------------------------------------------------------------------
def header(ts):
    return f_bytes(1, msg(f_str(1, "2.0"), f_varint(2, 0), f_varint(3, ts)))


def entity(eid, **kind):
    body = f_str(1, eid)
    for field, b in kind.items():
        body += f_bytes({"trip_update": 3, "vehicle": 4, "alert": 5}[field], b)
    return f_bytes(2, body)


def trip_desc(trip_id, relationship=None):
    b = f_str(1, trip_id) + f_str(3, "20091117")
    if relationship is not None:
        b += f_varint(4, relationship)
    return b


def stop_event(delay=None, time=None):
    return msg(f_varint(1, delay) if delay is not None else b"", f_varint(2, time) if time is not None else b"")


def stop_update(seq, stop_id, arr=None, dep=None, relationship=None):
    b = f_varint(1, seq) + f_str(4, stop_id)
    if arr:
        b += f_bytes(2, stop_event(**arr))
    if dep:
        b += f_bytes(3, stop_event(**dep))
    if relationship is not None:
        b += f_varint(5, relationship)
    return f_bytes(2, b)


def trip_update(trip_id, updates=(), relationship=None):
    return msg(f_bytes(1, trip_desc(trip_id, relationship)), *updates)


SCHEDULED, ADDED, UNSCHEDULED, CANCELED = 0, 1, 2, 3
SKIPPED = 1


def translated(text_by_lang):
    return msg(*[f_bytes(1, f_str(1, t) + f_str(2, l)) for l, t in text_by_lang])


def alert(header_text, desc, effect, cause, entities, period=(DAY + 6 * 3600, DAY + 86400), url=None):
    b = f_bytes(1, f_varint(1, period[0]) + f_varint(2, period[1]))
    for e in entities:
        b += f_bytes(5, e)
    b += f_varint(6, cause) + f_varint(7, effect)
    if url:
        b += f_bytes(8, translated([("en", url)]))
    b += f_bytes(10, translated(header_text)) + f_bytes(11, translated(desc))
    return b


def sel_route(route_id):
    return f_str(2, route_id)


def sel_stop(stop_id):
    return f_str(5, stop_id)


def sel_trip(trip_id):
    return f_bytes(4, trip_desc(trip_id))


def sel_route_type(t):
    return f_varint(3, t)


def vehicle(trip_id, vid, label, lat, lon, bearing, speed, seq, stop_id, status, ts, occupancy):
    pos = f_float(1, lat) + f_float(2, lon) + f_float(3, bearing) + f_float(5, speed)
    return msg(
        f_bytes(1, trip_desc(trip_id)), f_bytes(2, pos), f_varint(3, seq), f_varint(4, status),
        f_varint(5, ts), f_str(7, stop_id), f_bytes(8, f_str(1, vid) + f_str(2, label)),
        f_varint(9, occupancy))


def feed(ts, entities):
    return header(ts) + b"".join(entities)


STOPPED_AT, IN_TRANSIT_TO = 1, 2
MANY_SEATS, STANDING = 1, 3
# causes and effects
WEATHER, CONSTRUCTION = 8, 10
NO_SERVICE, REDUCED_SERVICE, SIGNIFICANT_DELAYS, DETOUR = 1, 2, 3, 4

T41, T171, T81, T331, T441 = "41W1300", "171W1260", "81W1180", "331W1220", "441W1140"


def trip_updates(step):
    if step == 1:
        ents = [
            trip_update(T41, [stop_update(3, "1342", arr={"delay": 120}, dep={"delay": 120})]),
            trip_update(T171, [
                stop_update(2, "2708", arr={"time": DAY + 34652 + 60}, dep={"time": DAY + 34652 + 90}),
                stop_update(4, "6725", dep={"delay": 120})]),
            trip_update(T81, [], relationship=CANCELED),
            trip_update(T331, [
                stop_update(2, "2828", relationship=SKIPPED),
                stop_update(3, "9041", arr={"delay": 60}, dep={"delay": 60})]),
            trip_update(T441, [stop_update(1, "4431", dep={"delay": -30})]),
        ]
    else:
        ents = [
            trip_update(T41, [
                stop_update(3, "1342", arr={"delay": 300}, dep={"delay": 300}),
                stop_update(6, "1316", arr={"delay": 330}, dep={"delay": 330})]),
            trip_update(T171, [], relationship=CANCELED),
            trip_update(T331, [
                stop_update(2, "2828", relationship=SKIPPED),
                stop_update(3, "9041", arr={"delay": 60}, dep={"delay": 60})]),
            trip_update(T441, [stop_update(4, "952", dep={"delay": 90})]),
        ]
    return feed(DAY + 32000 + step, [entity(f"tu-{i}", trip_update=e) for i, e in enumerate(ents)])


def alerts(step):
    en = lambda s: [("en", s)]
    a = []
    if step == 1:
        a.append(alert(en("Route 4 detour") + [("es", "Desvío de la línea 4")], en("Construction on Division St. Route 4 is detoured."),
                       DETOUR, CONSTRUCTION, [sel_route("4")], url="https://example.com/alerts/route-4"))
        a.append(alert(en("Delays downtown"), en("Snow causes delays near SW 6th & W Burnside."),
                       SIGNIFICANT_DELAYS, WEATHER, [sel_stop("7751"), sel_route("44")]))
        a.append(alert(en("Trip cancelled"), en("This trip does not run today."), NO_SERVICE, 2, [sel_trip(T81)]))
    else:
        a.append(alert(en("Delays downtown"), en("Snow still causes delays. Expect 15 minutes."),
                       SIGNIFICANT_DELAYS, WEATHER, [sel_stop("7751"), sel_route("44")]))
        a.append(alert(en("Reduced bus service"), en("Fewer buses run on all bus routes."),
                       REDUCED_SERVICE, 2, [sel_route_type(3)]))
    return feed(DAY + 32000 + step, [entity(f"al-{i}", alert=e) for i, e in enumerate(a)])


def vehicle_positions(step):
    ts = DAY + 32900 + 60 * step
    if step == 1:
        v = [
            vehicle(T41, "bus-4001", "4001", 45.50498, -122.42728, 90.0, 8.5, 2, "10602", IN_TRANSIT_TO, ts, MANY_SEATS),
            vehicle(T171, "bus-1701", "1701", 45.48878, -122.52431, 45.0, 0.0, 2, "2708", STOPPED_AT, ts, STANDING),
            vehicle(T331, "bus-3301", "3301", 45.32389, -122.57977, 10.0, 6.0, 2, "2828", IN_TRANSIT_TO, ts, MANY_SEATS),
        ]
    else:
        v = [
            vehicle(T41, "bus-4001", "4001", 45.50456, -122.43262, 92.0, 9.0, 3, "1342", IN_TRANSIT_TO, ts, MANY_SEATS),
            vehicle(T171, "bus-1701", "1701", 45.49047, -122.5235, 50.0, 7.0, 3, "6728", IN_TRANSIT_TO, ts, STANDING),
            vehicle(T441, "bus-4401", "4401", 45.43987, -122.72694, 355.0, 5.5, 2, "7579", STOPPED_AT, ts, MANY_SEATS),
        ]
    return feed(ts, [entity(f"vp-{i}", vehicle=e) for i, e in enumerate(v)])


def write_updates():
    UPDATES.mkdir(parents=True, exist_ok=True)
    for step in (1, 2):
        d = UPDATES / f"step-{step}"
        d.mkdir(exist_ok=True)
        (d / "trip-updates.pb").write_bytes(trip_updates(step))
        (d / "alerts.pb").write_bytes(alerts(step))
        (d / "vehicle-positions.pb").write_bytes(vehicle_positions(step))


# ---- dataset and scenarios -----------------------------------------------------------------
def update_dataset():
    p = DS / "dataset.json"
    d = json.loads(p.read_text())
    base = "modularization/harness/datasets/portland/updates"
    d["routerConfig"] = {
        "timetableUpdates": {"maxSnapshotFrequency": "PT0S"},
        "updaters": [
            {"type": "stop-time-updater", "feedId": "1", "frequency": "PT1S",
             "url": "file://${workDir}/rt-feeds/trip-updates.pb"},
            {"type": "real-time-alerts", "feedId": "1", "frequency": "PT1S",
             "url": "file://${workDir}/rt-feeds/alerts.pb"},
            {"type": "vehicle-positions", "feedId": "1", "frequency": "PT1S",
             "url": "file://${workDir}/rt-feeds/vehicle-positions.pb",
             "features": ["position", "stop-position", "occupancy"]},
        ],
    }
    d["steps"] = [
        {"name": f"rt-{n}", "awaitDir": "", "settleMillis": 4000,
         "drops": [{"from": f"{base}/step-{i}/{f}", "toDir": "rt-feeds"}
                   for f in ("trip-updates.pb", "alerts.pb", "vehicle-positions.pb")]}
        for i, n in ((1, "delays"), (2, "changes"))
    ]
    # The initial state needs the files to exist: they start empty feeds (step 0).
    d["inputs"] = [i for i in d["inputs"] if "/updates/" not in i["from"]]
    for f in ("trip-updates.pb", "alerts.pb", "vehicle-positions.pb"):
        d["inputs"].append({"from": f"{base}/step-0/{f}", "as": f"rt-feeds/{f}"})
    p.write_text(json.dumps(d, indent=2) + "\n")


TRIPS = [T41, T171, T81, T331, T441]
trip_q = " ".join(
    f't{i}: trip(id:"1:{t}") {{ stoptimesForDate(serviceDate:"20091117") {{ stop {{ gtfsId }} stopPosition scheduledArrival realtimeArrival arrivalDelay '
    f"scheduledDeparture realtimeDeparture departureDelay realtime realtimeState }} }}"
    for i, t in enumerate(TRIPS))
vp_q = " ".join(
    f't{i}: trip(id:"1:{t}") {{ pattern {{ code vehiclePositions {{ vehicleId label lat lon heading speed lastUpdated '
    "stopRelationship { status stop { gtfsId } } trip { gtfsId } } } }"
    for i, t in enumerate(TRIPS))
alert_fields = ("id alertHeaderText alertDescriptionText alertEffect alertCause alertSeverityLevel alertUrl "
                "effectiveStartDate effectiveEndDate alertHeaderTextTranslations { text language } "
                "entities { __typename ... on Route { gtfsId } ... on Stop { gtfsId } ... on Trip { gtfsId } "
                "... on RouteType { routeType } }")
entity_alerts_q = ('{ r4: route(id:"1:4") { alerts { alertHeaderText } } r44: route(id:"1:44") { alerts { alertHeaderText } } '
                   's7751: stop(id:"1:7751") { alerts { alertHeaderText } } '
                   f't81: trip(id:"1:{T81}") {{ alerts {{ alertHeaderText }} }} }}')
sit_q = ("{ situations { situationNumber summary { value language } description { value language } reportType severity "
         "validityPeriod { startTime endTime } lines { id } quays { id } serviceJourneys { id } } }")
calls_q = "{ " + " ".join(
    f'q{q}: quay(id:"1:{q}") {{ estimatedCalls(numberOfDepartures:6, startTime:"2009-11-17T09:00:00-08:00", timeRange:7200) '
    "{ aimedDepartureTime expectedDepartureTime realtime realtimeState cancellation serviceJourney { id } } }"
    for q in ("2253", "1342", "2708", "2828", "9041", "4431")) + " }"


def plan(o, d, dt, extra=""):
    return (f"{{ planConnection(origin:{{location:{{coordinate:{{latitude:{o[0]},longitude:{o[1]}}}}}}}, "
            f"destination:{{location:{{coordinate:{{latitude:{d[0]},longitude:{d[1]}}}}}}}, "
            f'dateTime:{{earliestDeparture:"{dt}"}}, first:3 {extra}) {{ routingErrors {{ code }} edges {{ node {{ start end duration '
            "legs { mode transitLeg realTime realtimeState departureDelay arrivalDelay "
            "start { scheduledTime estimated { time delay } } end { scheduledTime estimated { time delay } } "
            "from { name stop { gtfsId } } to { name stop { gtfsId } } route { gtfsId } trip { gtfsId } "
            "alerts { alertHeaderText } } } } } }")


def plan_stops(o, d, dt):
    """Stop to stop, transit only. The trips are outside the street network of the OSM extract."""
    return (f'{{ planConnection(origin:{{location:{{stopLocation:{{stopLocationId:"1:{o}"}}}}}}, '
            f'destination:{{location:{{stopLocation:{{stopLocationId:"1:{d}"}}}}}}, '
            f'dateTime:{{earliestDeparture:"{dt}"}}, first:3, modes:{{transitOnly:true}}) {{ routingErrors {{ code }} '
            "edges { node { start end duration legs { mode transitLeg realTime realtimeState departureDelay arrivalDelay "
            "start { scheduledTime estimated { time delay } } end { scheduledTime estimated { time delay } } "
            "from { name stop { gtfsId } } to { name stop { gtfsId } } route { gtfsId } trip { gtfsId } "
            "alerts { alertHeaderText } } } } } }")


def tm_trip(o, d, dt):
    return (f"{{ trip(from:{{coordinates:{{latitude:{o[0]},longitude:{o[1]}}}}}, "
            f"to:{{coordinates:{{latitude:{d[0]},longitude:{d[1]}}}}}, dateTime:\"{dt}\", numTripPatterns:3) "
            "{ routingErrors { code } tripPatterns { expectedStartTime expectedEndTime legs { mode realtime "
            "aimedStartTime expectedStartTime aimedEndTime expectedEndTime line { id } serviceJourney { id } "
            "situations { situationNumber } } } } }")


def write_scenarios():
    scen = {
        "rt-gtfs-trips": {"api": "gtfs", "query": "{ " + trip_q + " }"},
        "rt-gtfs-vehicle-positions": {"api": "gtfs", "query": "{ " + vp_q + " }"},
        "rt-gtfs-alerts": {"api": "gtfs", "query": "{ alerts { " + alert_fields + " } }",
                           "sortArraysBy": "id", "unordered": ["data.alerts[*].entities"]},
        "rt-gtfs-entity-alerts": {"api": "gtfs", "query": entity_alerts_q},
        "rt-gtfs-plan-downtown": {"api": "gtfs", "query": plan((45.5231, -122.6765), (45.5155, -122.6789), "2009-11-17T10:00:00-08:00")},
        "rt-gtfs-plan-route4": {"api": "gtfs", "query": plan_stops("1342", "1316", "2009-11-17T09:00:00-08:00")},
        "rt-gtfs-plan-route17": {"api": "gtfs", "query": plan_stops("2708", "4583", "2009-11-17T09:30:00-08:00")},
        "rt-tm-situations": {"api": "transmodel", "query": sit_q, "sortArraysBy": "situationNumber",
                             "unordered": ["data.situations[*].lines", "data.situations[*].quays", "data.situations[*].serviceJourneys"]},
        "rt-tm-estimated-calls": {"api": "transmodel", "query": calls_q, "unordered": ["data.*.estimatedCalls"]},
    }
    for phase in ("after-rt-delays", "after-rt-changes"):
        d = DS / "scenarios" / phase
        d.mkdir(parents=True, exist_ok=True)
        for n, s in scen.items():
            (d / f"{n}.json").write_text(json.dumps(s, indent=2) + "\n")
    # also run the same queries before any update, to see the baseline
    for n, s in scen.items():
        (DS / "scenarios" / f"{n}.json").write_text(json.dumps(s, indent=2) + "\n")


def write_empty_step_0():
    d = UPDATES / "step-0"
    d.mkdir(parents=True, exist_ok=True)
    for f in ("trip-updates.pb", "alerts.pb", "vehicle-positions.pb"):
        (d / f).write_bytes(feed(DAY + 32000, []))


write_updates()
write_empty_step_0()
update_dataset()
write_scenarios()
print("ok")
