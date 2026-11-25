#!/usr/bin/env python3
"""Generate the scenarios that run after each SIRI-ET step of the oslo dataset.

Every step gets the same set of queries, so the effect of each update (and of the updates before
it) shows in the same places: stop times with real-time data, trip searches, departures.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "datasets/oslo/scenarios"
STEPS = ["et-delay", "et-cancel", "et-extra", "et-delay-again"]
QUAYS = [10682, 7184, 7203, 8027, 8028, 7194, 10681, 7183]

gtfs_stops = " ".join(
    f'q{q}: stop(id:"EN:NSR:Quay:{q}") {{ stoptimesForServiceDate(date:"2017-12-27", omitCanceled:false) '
    "{ pattern { code } stoptimes { scheduledArrival realtimeArrival arrivalDelay scheduledDeparture "
    "realtimeDeparture departureDelay realtime realtimeState serviceDay headsign trip { gtfsId } } } }"
    for q in QUAYS
)
tm_calls = " ".join(
    f'q{q}: quay(id:"EN:NSR:Quay:{q}") {{ estimatedCalls(numberOfDepartures:10, startTime:"2017-12-27T00:00:00+01:00", timeRange:86400) '
    "{ date aimedArrivalTime expectedArrivalTime aimedDepartureTime expectedDepartureTime realtime "
    "realtimeState cancellation predictionInaccurate serviceJourney { id line { id } } } }"
    for q in QUAYS
)


def gtfs_plan(o, d, dt):
    return (
        f"{{ planConnection(origin:{{location:{{coordinate:{{latitude:{o[0]},longitude:{o[1]}}}}}}}, "
        f"destination:{{location:{{coordinate:{{latitude:{d[0]},longitude:{d[1]}}}}}}}, "
        f'dateTime:{{earliestDeparture:"{dt}"}}, first:3) {{ routingErrors {{ code }} edges {{ node {{ '
        "start end duration numberOfTransfers legs { mode transitLeg realTime realtimeState departureDelay arrivalDelay "
        "start { scheduledTime estimated { time delay } } end { scheduledTime estimated { time delay } } "
        "from { name stop { gtfsId } } to { name stop { gtfsId } } route { gtfsId } trip { gtfsId } } } } } }"
    )


def tm_trip(o, d, dt):
    return (
        f"{{ trip(from:{{coordinates:{{latitude:{o[0]},longitude:{o[1]}}}}}, "
        f"to:{{coordinates:{{latitude:{d[0]},longitude:{d[1]}}}}}, "
        f'dateTime:"{dt}", numTripPatterns:3) {{ routingErrors {{ code }} tripPatterns {{ '
        "expectedStartTime expectedEndTime duration legs { mode realtime aimedStartTime expectedStartTime "
        "aimedEndTime expectedEndTime line { id } serviceJourney { id } "
        "fromEstimatedCall { cancellation realtime } toEstimatedCall { cancellation realtime } } } } }"
    )


BERG, JERN, KJEL, MAJOR = (59.86718, 10.82105), (59.9123, 10.7513), (59.9300, 10.7147), (59.9115, 10.7498)
scenarios = {
    "gtfs-stop-times": {"api": "gtfs", "query": "{ " + gtfs_stops + " }",
                        "unordered": ["data.*.stoptimesForServiceDate"]},
    "gtfs-plan-line4": {"api": "gtfs", "query": gtfs_plan(BERG, JERN, "2017-12-27T14:00:00+01:00")},
    "gtfs-plan-line4-late": {"api": "gtfs", "query": gtfs_plan(BERG, JERN, "2017-12-27T15:50:00+01:00")},
    "gtfs-plan-line12": {"api": "gtfs", "query": gtfs_plan(MAJOR, KJEL, "2017-12-27T06:00:00+01:00")},
    "gtfs-patterns": {"api": "gtfs", "query": "{ patterns { code name route { gtfsId } stops { gtfsId } } }",
                      "sortArraysBy": "code"},
    "tm-estimated-calls": {"api": "transmodel", "query": "{ " + tm_calls + " }",
                           "unordered": ["data.*.estimatedCalls"]},
    "tm-trip-line4": {"api": "transmodel", "query": tm_trip(BERG, JERN, "2017-12-27T14:00:00+01:00")},
    "tm-trip-line12": {"api": "transmodel", "query": tm_trip(MAJOR, KJEL, "2017-12-27T06:00:00+01:00")},
}

for step in STEPS:
    d = ROOT / f"after-{step}"
    d.mkdir(parents=True, exist_ok=True)
    for name, s in scenarios.items():
        (d / f"{name}.json").write_text(json.dumps(s, indent=2) + "\n")
