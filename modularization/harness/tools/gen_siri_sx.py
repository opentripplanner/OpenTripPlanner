#!/usr/bin/env python3
"""Generate the SIRI-SX (situation exchange) part of the oslo dataset.

Writes datasets/oslo/updates/sx-*.xml, the updater and the steps in datasets/oslo/dataset.json, and
the scenarios that run before the first step and after each SX step. The situations affect lines,
stop points, stop places, an operator and a vehicle journey of the NeTEx sample, and one affects
nothing OTP knows. The second step changes one situation, closes one and adds one.

The updater drops a delivery whose ResponseTimestamp is older than the previous one, so the
timestamps increase per file.
"""
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
DS = HERE / "datasets/oslo"
UPDATES = DS / "updates"
DAY = "2017-12-27"
FAR = "2099-01-01T00:00:00+01:00"


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def text(tag, lang, value):
    return f'        <{tag} xml:lang="{lang}">{esc(value)}</{tag}>\n'


def situation(number, version, summary, description, affects, *, start="2017-12-01T00:00:00+01:00",
              end=FAR, progress="open", report="incident", severity="severe", priority=1,
              advice=None, links=(), created="2017-12-26T10:00:00+01:00", extra_validity=()):
    s = "      <PtSituationElement>\n"
    s += f"        <CreationTime>{created}</CreationTime>\n"
    s += "        <ParticipantRef>HARNESS</ParticipantRef>\n"
    s += f"        <SituationNumber>HARNESS:SituationNumber:{number}</SituationNumber>\n"
    s += f"        <Version>{version}</Version>\n"
    s += "        <Source><SourceType>directReport</SourceType></Source>\n"
    s += f"        <VersionedAtTime>2017-12-26T1{version}:00:00+01:00</VersionedAtTime>\n"
    s += f"        <Progress>{progress}</Progress>\n"
    for a, b in [(start, end), *extra_validity]:
        s += f"        <ValidityPeriod>\n          <StartTime>{a}</StartTime>\n          <EndTime>{b}</EndTime>\n        </ValidityPeriod>\n"
    s += f"        <ReportType>{report}</ReportType>\n"
    s += f"        <Priority>{priority}</Priority>\n"
    s += f"        <Severity>{severity}</Severity>\n"
    for lang, value in summary:
        s += text("Summary", lang, value)
    for lang, value in description:
        s += text("Description", lang, value)
    if advice:
        s += text("Advice", advice[0], advice[1])
    s += "        <Affects>\n" + affects + "        </Affects>\n"
    if links:
        s += "        <InfoLinks>\n"
        for uri, label in links:
            s += f"          <InfoLink>\n            <Uri>{uri}</Uri>\n            <Label>{label}</Label>\n          </InfoLink>\n"
        s += "        </InfoLinks>\n"
    return s + "      </PtSituationElement>\n"


def line(ref):
    return ("          <Networks>\n            <AffectedNetwork>\n              <AffectedLine>\n"
            f"                <LineRef>{ref}</LineRef>\n              </AffectedLine>\n"
            "            </AffectedNetwork>\n          </Networks>\n")


def line_stops(ref, stops):
    pts = "".join(f"                      <AffectedStopPoint><StopPointRef>{q}</StopPointRef></AffectedStopPoint>\n" for q in stops)
    return ("          <Networks>\n            <AffectedNetwork>\n              <AffectedLine>\n"
            f"                <LineRef>{ref}</LineRef>\n                <Routes>\n                  <AffectedRoute>\n"
            f"                    <StopPoints>\n{pts}                    </StopPoints>\n"
            "                  </AffectedRoute>\n                </Routes>\n              </AffectedLine>\n"
            "            </AffectedNetwork>\n          </Networks>\n")


def stops(*quays):
    return ("          <StopPoints>\n" + "".join(
        f"            <AffectedStopPoint><StopPointRef>{q}</StopPointRef></AffectedStopPoint>\n" for q in quays)
        + "          </StopPoints>\n")


def places(*ids):
    return ("          <StopPlaces>\n" + "".join(
        f"            <AffectedStopPlace><StopPlaceRef>{i}</StopPlaceRef></AffectedStopPlace>\n" for i in ids)
        + "          </StopPlaces>\n")


def operator(ref):
    return f"          <Operators>\n            <AffectedOperator><OperatorRef>{ref}</OperatorRef></AffectedOperator>\n          </Operators>\n"


def journey(trip):
    return ("          <VehicleJourneys>\n            <AffectedVehicleJourney>\n"
            f"              <FramedVehicleJourneyRef>\n                <DataFrameRef>{DAY}</DataFrameRef>\n"
            f"                <DatedVehicleJourneyRef>{trip}</DatedVehicleJourneyRef>\n"
            "              </FramedVehicleJourneyRef>\n            </AffectedVehicleJourney>\n          </VehicleJourneys>\n")


def siri(situations, seq):
    response = f"2099-01-01T00:01:{seq:02d}+01:00"
    return (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<Siri xmlns="http://www.siri.org.uk/siri" version="2.0">\n'
        "  <ServiceDelivery>\n"
        f"    <ResponseTimestamp>{response}</ResponseTimestamp>\n"
        "    <ProducerRef>HARNESS</ProducerRef>\n"
        '    <SituationExchangeDelivery version="2.0">\n'
        f"      <ResponseTimestamp>{response}</ResponseTimestamp>\n"
        "      <Situations>\n" + "".join(situations) + "      </Situations>\n"
        "    </SituationExchangeDelivery>\n"
        "  </ServiceDelivery>\n"
        "</Siri>\n"
    )


L4, L12 = "RUT:Line:4", "RUT:Line:12"
T4_A = "RUT:ServiceJourney:4-101493-1098"

STEP1 = [
    situation(1, 1, [("NO", "Forsinkelser på linje 4"), ("EN", "Delays on line 4")],
              [("NO", "Redusert fremkommelighet."), ("EN", "Reduced accessibility.")],
              line(L4), advice=("EN", "Take line 12 instead"),
              links=[("https://example.com/line4", "More about line 4")]),
    situation(2, 1, [("EN", "Quay closed")], [("EN", "The quay is closed for maintenance.")],
              stops("NSR:Quay:10682"), severity="normal", priority=2),
    situation(3, 1, [("EN", "Stop place works")], [("EN", "Works at the stop place.")],
              places("NSR:StopPlace:4000"), severity="slight", priority=3, report="general"),
    situation(4, 1, [("EN", "Operator strike")], [("EN", "Operator 130c is on strike.")],
              operator("RUT:Operator:130c"), severity="verySevere", priority=1),
    situation(5, 1, [("EN", "Trip cancelled")], [("EN", "This trip is cancelled due to a vehicle fault.")],
              journey(T4_A), severity="severe", report="incident"),
    situation(6, 1, [("EN", "Line 12 stops affected")], [("EN", "Two stops on line 12 are not served.")],
              line_stops(L12, ["NSR:Quay:7203", "NSR:Quay:8027"]), severity="normal"),
    situation(7, 1, [("EN", "Unknown target")], [("EN", "Affects something OTP does not know.")],
              stops("NSR:Quay:DOES-NOT-EXIST"), severity="unknown"),
    situation(8, 1, [("EN", "Ended last week")], [("EN", "A past situation.")],
              line(L12), start="2017-12-01T00:00:00+01:00", end="2017-12-10T00:00:00+01:00", severity="slight"),
    situation(9, 1, [("EN", "Far in the future")], [("EN", "A future situation.")],
              line(L12), start="2098-01-01T00:00:00+01:00", end="2098-02-01T00:00:00+01:00"),
    situation(10, 1, [("EN", "Two periods")], [("EN", "A situation with two validity periods.")],
              stops("NSR:Quay:7184"), start="2017-12-20T00:00:00+01:00", end="2017-12-24T00:00:00+01:00",
              extra_validity=[("2017-12-26T00:00:00+01:00", "2017-12-29T00:00:00+01:00")]),
    # no texts at all: the updater ignores it
    "      <PtSituationElement>\n        <CreationTime>2017-12-26T10:00:00+01:00</CreationTime>\n"
    "        <ParticipantRef>HARNESS</ParticipantRef>\n"
    "        <SituationNumber>HARNESS:SituationNumber:11</SituationNumber>\n"
    "        <Version>1</Version>\n        <Progress>open</Progress>\n"
    "        <Affects>\n" + line(L4) + "        </Affects>\n      </PtSituationElement>\n",
]

STEP2 = [
    # new version of 1: other text, other severity, one more link
    situation(1, 2, [("NO", "Store forsinkelser på linje 4"), ("EN", "Major delays on line 4")],
              [("EN", "Replacement buses are running.")], line(L4), severity="verySevere", priority=1,
              links=[("https://example.com/line4", "More about line 4"), ("https://example.com/bus", "Replacement buses")]),
    # 2 is closed
    situation(2, 2, [("EN", "Quay closed")], [("EN", "Open again.")], stops("NSR:Quay:10682"), progress="closed"),
    # 3 moves to another stop place and a quay
    situation(3, 2, [("EN", "Stop place works")], [("EN", "Works moved.")],
              places("NSR:StopPlace:3990") + stops("NSR:Quay:7194"), severity="slight", priority=3, report="general"),
    # a new one on the cancelled trip's line and stop
    situation(12, 1, [("EN", "New: line 4 and quay")], [("EN", "Affects the line and one of its quays.")],
              line(L4) + stops("NSR:Quay:7184"), severity="normal", priority=2),
]

SCENARIO_STEPS = ["sx-add", "sx-update"]
FILES = {"sx-add": "sx-1-add.xml", "sx-update": "sx-2-update.xml"}


def write_updates():
    UPDATES.mkdir(parents=True, exist_ok=True)
    (UPDATES / "sx-1-add.xml").write_text(siri(STEP1, 1))
    (UPDATES / "sx-2-update.xml").write_text(siri(STEP2, 2))


def write_dataset():
    p = DS / "dataset.json"
    d = json.loads(p.read_text())
    ups = d["routerConfig"]["updaters"]
    if not any(u["type"] == "siri-sx-updater" for u in ups):
        ups.append({"type": "siri-sx-updater", "feedId": "EN", "url": "file://${workDir}/siri-sx",
                    "frequency": "PT1S", "earlyStart": "PT30M"})
    names = {s["name"] for s in d["steps"]}
    for step in SCENARIO_STEPS:
        if step not in names:
            d["steps"].append({"name": step, "awaitDir": "siri-sx", "settleMillis": 2000,
                               "drops": [{"from": f"modularization/harness/datasets/oslo/updates/{FILES[step]}",
                                          "toDir": "siri-sx"}]})
    p.write_text(json.dumps(d, indent=2) + "\n")


ALERT_FIELDS = ("id alertHeaderText alertId alertDescriptionText alertEffect alertCause alertSeverityLevel alertUrl "
                "effectiveStartDate effectiveEndDate activityPeriods { start end } alertHeaderTextTranslations { text language } "
                "alertDescriptionTextTranslations { text language } "
                "entities { __typename ... on Route { gtfsId } ... on Stop { gtfsId } ... on Trip { gtfsId } "
                "... on Agency { gtfsId } ... on RouteType { routeType } }")
ENTITY = ('{ l4: route(id:"EN:RUT:Line:4") { alerts { id alertHeaderText } } '
          'l12: route(id:"EN:RUT:Line:12") { alerts { id alertHeaderText } } '
          'q10682: stop(id:"EN:NSR:Quay:10682") { alerts { id alertHeaderText } } '
          'q7184: stop(id:"EN:NSR:Quay:7184") { alerts { id alertHeaderText } } '
          'q7194: stop(id:"EN:NSR:Quay:7194") { alerts { id alertHeaderText } } '
          'q7203: stop(id:"EN:NSR:Quay:7203") { alerts { id alertHeaderText } } '
          f't4: trip(id:"EN:{T4_A}") {{ alerts {{ id alertHeaderText }} }} '
          'agency: agencies { gtfsId alerts { id alertHeaderText } } }')
SITUATIONS = ("{ situations { situationNumber participant reportType severity priority version "
              "creationTime versionedAtTime "
              "summary { value language } description { value language } advice { value language } "
              "infoLinks { uri label } validityPeriods { startTime endTime } "
              "authority { id } lines { id } quays { id } stopPlaces { id } serviceJourneys { id } "
              "affects { __typename ... on AffectedLine { line { id } } "
              "... on AffectedServiceJourney { serviceJourney { id } operatingDay } "
              "... on AffectedStopPlace { quay { id } stopPlace { id } stopConditions } "
              "... on AffectedStopPlaceOnLine { line { id } quay { id } stopPlace { id } stopConditions } "
              "... on AffectedStopPlaceOnServiceJourney { serviceJourney { id } quay { id } stopPlace { id } } "
              "... on AffectedUnknown { description } } } }")
PLAN = ('{ planConnection(origin:{location:{coordinate:{latitude:59.86718,longitude:10.82105}}}, '
        'destination:{location:{coordinate:{latitude:59.9123,longitude:10.7513}}}, '
        'dateTime:{earliestDeparture:"2017-12-27T14:00:00+01:00"}, first:3) { routingErrors { code } edges { node { '
        "start end legs { mode transitLeg route { gtfsId } trip { gtfsId } from { name stop { gtfsId } } "
        "to { name stop { gtfsId } } alerts { id alertHeaderText } } } } } }")
TM_TRIP = ('{ trip(from:{coordinates:{latitude:59.86718,longitude:10.82105}}, '
           'to:{coordinates:{latitude:59.9123,longitude:10.7513}}, dateTime:"2017-12-27T14:00:00+01:00", numTripPatterns:3) '
           "{ routingErrors { code } tripPatterns { expectedStartTime legs { mode line { id } serviceJourney { id } "
           "situations { situationNumber } fromEstimatedCall { quay { id } } toEstimatedCall { quay { id } } } } } }")
TM_CALLS = ('{ q: quay(id:"EN:NSR:Quay:10682") { situations { situationNumber } estimatedCalls(numberOfDepartures:5, '
            'startTime:"2017-12-27T14:00:00+01:00", timeRange:7200) { aimedDepartureTime realtime cancellation '
            "serviceJourney { id } situations { situationNumber } } } }")


def scenarios():
    return {
        "sx-gtfs-alerts": {"api": "gtfs", "query": "{ alerts { " + ALERT_FIELDS + " } }",
                           "sortArraysBy": "id", "unordered": ["data.alerts[*].entities"]},
        "sx-gtfs-entity-alerts": {"api": "gtfs", "query": ENTITY, "sortArraysBy": "id"},
        "sx-gtfs-plan-line4": {"api": "gtfs", "query": PLAN, "unordered": ["data.planConnection.edges[*].node.legs[*].alerts"]},
        "sx-tm-situations": {"api": "transmodel", "query": SITUATIONS, "sortArraysBy": "situationNumber",
                             "unordered": ["data.situations[*].lines", "data.situations[*].quays", "data.situations[*].stopPlaces", "data.situations[*].affects", "data.situations[*].validityPeriods",
                                           "data.situations[*].serviceJourneys", "data.situations[*].summary",
                                           "data.situations[*].description"]},
        "sx-tm-trip-line4": {"api": "transmodel", "query": TM_TRIP, "unordered": ["data.trip.tripPatterns[*].legs[*].situations"]},
        "sx-tm-quay-situations": {"api": "transmodel", "query": TM_CALLS,
                                  "unordered": ["data.q.situations", "data.q.estimatedCalls[*].situations"]},
    }


def write_scenarios():
    sc = scenarios()
    for phase in [None, *("after-" + s for s in SCENARIO_STEPS)]:
        d = DS / "scenarios" / phase if phase else DS / "scenarios"
        d.mkdir(parents=True, exist_ok=True)
        for n, s in sc.items():
            (d / f"{n}.json").write_text(json.dumps(s, indent=2) + "\n")


if __name__ == "__main__":
    write_updates()
    write_dataset()
    write_scenarios()
