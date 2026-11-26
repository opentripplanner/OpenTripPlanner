#!/usr/bin/env python3
"""Generate the scenarios of the dataset `cobb-flex` (flexible transit, a sandbox feature).

The data is the GTFS-Flex sample of Cobb County: two fixed-route feeds (Cobb bus 30, MARTA bus 856)
and a scheduled deviated flex service with zones (feed FLEX). The date of the tests in the OTP
repository is used: 2021-12-02 (a Thursday), 12:00 -05:00.

  python3 tools/gen_flex.py            writes datasets/cobb-flex/scenarios
  python3 tools/gen_flex.py --check    runs every scenario against a server on port 8080 and reports
"""
import json
import sys
import urllib.request
from pathlib import Path

DS = Path(__file__).resolve().parent.parent / "datasets/cobb-flex"
SC = DS / "scenarios"

OUTSIDE = (33.7552, -84.4631)
INSIDE = (33.8694, -84.6233)
NEAR_ZONE = (33.85281, -84.60271)
MIDDLE_ZONE = (33.86701256815635, -84.61787939071655)
NOON = "2021-12-02T12:00:00-05:00"

LEG = ("mode transitLeg duration distance realTime start { scheduledTime } end { scheduledTime } "
       "from { name stop { gtfsId } } to { name stop { gtfsId } } route { gtfsId shortName } trip { gtfsId } "
       "pickupBookingInfo { message minimumBookingNotice contactInfo { phoneNumber } "
       "earliestBookingTime { daysPrior time } latestBookingTime { daysPrior time } } "
       "dropOffBookingInfo { message minimumBookingNotice }")


def coord(c):
    return f"{{location:{{coordinate:{{latitude:{c[0]},longitude:{c[1]}}}}}}}"


def stop(sid):
    return f'{{location:{{stopLocation:{{stopLocationId:"{sid}"}}}}}}'


FLEX_MODES = ", modes:{direct:[WALK, FLEX], transit:{access:[WALK, FLEX], egress:[WALK, FLEX]}}"


def gtfs_plan(o, d, dt, *, first=10, arrive_by=False, extra=FLEX_MODES):
    when = f'latestArrival:"{dt}"' if arrive_by else f'earliestDeparture:"{dt}"'
    return (f"{{ planConnection(origin:{o}, destination:{d}, dateTime:{{{when}}}, first:{first}{extra}) "
            f"{{ routingErrors {{ code }} edges {{ node {{ start end duration numberOfTransfers "
            f"generalizedCost legs {{ {LEG} }} }} }} }} }}")


def tm_trip(o, d, dt, *, modes="", n=8, arrive_by=False):
    return (f'{{ trip(from:{{coordinates:{{latitude:{o[0]},longitude:{o[1]}}}}}, '
            f'to:{{coordinates:{{latitude:{d[0]},longitude:{d[1]}}}}}, dateTime:"{dt}", '
            f"arriveBy:{str(arrive_by).lower()}, numTripPatterns:{n}{modes}) "
            "{ routingErrors { code } tripPatterns { expectedStartTime expectedEndTime duration "
            "generalizedCost legs { mode aimedStartTime aimedEndTime line { id publicCode flexibleLineType } "
            "serviceJourney { id } fromPlace { name } toPlace { name } "
            "fromEstimatedCall { aimedDepartureTime bookingArrangements { bookingMethods bookingNote minimumBookingPeriod "
            "latestBookingTime latestBookingDay } } "
            "toEstimatedCall { aimedArrivalTime } } } } }")


FLEX_TRIPS = ('{ trips(feeds:["FLEX"]) { gtfsId route { gtfsId shortName longName } pattern { code } '
              "stoptimes { stopPosition stopPositionInPattern pickupType dropoffType scheduledArrival "
              "scheduledDeparture timepoint stop { gtfsId name lat lon } } } }")
FLEX_ZONES = ('{ routes(feeds:["FLEX"]) { gtfsId patterns { code stops { gtfsId name lat lon '
              "geometries { googleEncoded { length points } } } } } }")
PATTERNS = ("{ patterns { code name route { gtfsId } stops { gtfsId name } } }")
ROUTES = "{ routes { gtfsId shortName longName mode type color agency { gtfsId } } }"
TM_FLEX = ('{ lines(flexibleOnly:true) { id name publicCode flexibleLineType transportMode '
           "journeyPatterns { id name } serviceJourneys { id "
           "passingTimes { arrival { time dayOffset } departure { time dayOffset } "
           "quay { id name flexibleGroup { id } } "
           "bookingArrangements { bookingMethods bookingNote minimumBookingPeriod latestBookingTime latestBookingDay "
           "bookingContact { phone contactPerson url } } } } } }")
TM_QUAYS = '{ quays { id name flexibleArea flexibleGroup { id } latitude longitude } }'
TM_ESTIMATED = ('{ q: quay(id:"FLEX:yz85") { name estimatedCalls(numberOfDepartures:5, '
                'startTime:"2021-12-02T00:00:00-05:00", timeRange:86400) { aimedDepartureTime '
                "bookingArrangements { bookingMethods bookingNote minimumBookingPeriod } "
                "serviceJourney { id line { id } } } } "
                'c: quay(id:"FLEX:cujv") { name estimatedCalls(numberOfDepartures:5, '
                'startTime:"2021-12-02T00:00:00-05:00", timeRange:86400) { aimedDepartureTime aimedArrivalTime '
                "serviceJourney { id line { id } } } } }")

SCENARIOS = {
    "gtfs-routes": {"api": "gtfs", "query": ROUTES, "sortArraysBy": "gtfsId"},
    "gtfs-patterns": {"api": "gtfs", "query": PATTERNS, "sortArraysBy": "code"},
    "gtfs-flex-trips": {"api": "gtfs", "query": FLEX_TRIPS, "sortArraysBy": "gtfsId"},
    "gtfs-flex-zones": {"api": "gtfs", "query": FLEX_ZONES, "sortArraysBy": "gtfsId",
                        "unordered": ["data.routes[*].patterns"]},
    "gtfs-plan-bus-to-flex": {"api": "gtfs", "query": gtfs_plan(coord(OUTSIDE), coord(INSIDE), NOON)},
    "gtfs-plan-two-transfers": {"api": "gtfs", "query": gtfs_plan(stop("MARTA:97266"), coord(MIDDLE_ZONE), NOON, first=8)},
    "gtfs-plan-direct-flex": {"api": "gtfs", "query": gtfs_plan(coord(NEAR_ZONE), coord(MIDDLE_ZONE), NOON)},
    "gtfs-plan-arrive-by": {"api": "gtfs", "query": gtfs_plan(coord(OUTSIDE), coord(INSIDE), "2021-12-02T16:00:00-05:00", arrive_by=True)},
    "gtfs-plan-flex-egress-only": {"api": "gtfs", "query": gtfs_plan(coord(OUTSIDE), coord(INSIDE), NOON, extra=", modes:{transit:{access:[WALK], egress:[WALK, FLEX]}}")},
    "gtfs-plan-no-flex": {"api": "gtfs", "query": gtfs_plan(coord(OUTSIDE), coord(INSIDE), NOON, extra="")},
    "gtfs-plan-booking-time": {"api": "gtfs", "query": gtfs_plan(coord(NEAR_ZONE), coord(MIDDLE_ZONE), NOON, extra=FLEX_MODES + ', flex:{bookingTime:"2021-12-02T11:59:00-05:00"}')},
    "gtfs-plan-after-service": {"api": "gtfs", "query": gtfs_plan(coord(NEAR_ZONE), coord(MIDDLE_ZONE), "2022-03-01T12:00:00-05:00")},
    "tm-flex-lines": {"api": "transmodel", "query": TM_FLEX, "sortArraysBy": "id",
                      "unordered": ["data.lines[*].journeyPatterns"]},
    "tm-quays": {"api": "transmodel", "query": TM_QUAYS, "sortArraysBy": "id"},
    "tm-estimated-calls": {"api": "transmodel", "query": TM_ESTIMATED},
    "tm-trip-bus-to-flex": {"api": "transmodel", "query": tm_trip(OUTSIDE, INSIDE, NOON)},
    "tm-trip-direct-flex": {"api": "transmodel", "query": tm_trip(NEAR_ZONE, MIDDLE_ZONE, NOON,
                            modes=", modes:{directMode:flexible, accessMode:foot, egressMode:flexible, transportModes:[{transportMode:bus}]}")},
    "tm-trip-arrive-by": {"api": "transmodel", "query": tm_trip(OUTSIDE, INSIDE, "2021-12-02T16:00:00-05:00", arrive_by=True)},
}


def write():
    SC.mkdir(parents=True, exist_ok=True)
    for old in SC.glob("*.json"):
        old.unlink()
    for name, s in SCENARIOS.items():
        (SC / f"{name}.json").write_text(json.dumps(s, indent=2) + "\n")


def check():
    urls = {"gtfs": "http://localhost:8080/otp/gtfs/v1", "transmodel": "http://localhost:8080/otp/transmodel/v3"}
    for name, s in SCENARIOS.items():
        req = urllib.request.Request(urls[s["api"]], json.dumps({"query": s["query"]}).encode(),
                                     {"Content-Type": "application/json"})
        d = json.load(urllib.request.urlopen(req))
        errs = [e["message"][:160] for e in d.get("errors", [])]
        print(name, "ERRORS " + str(errs) if errs else "ok " + str(len(json.dumps(d))))


if __name__ == "__main__":
    write()
    if "--check" in sys.argv:
        check()
