#!/usr/bin/env python3
"""Generate the SIRI-ET update files of the oslo dataset into datasets/oslo/updates/.

The trips are the four trips of the NeTEx sample on 2017-12-27 (see datasets/oslo). Stop and trip
ids have no feed prefix; the updater adds the feed id (EN).
"""
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "datasets/oslo/updates"
DATE = "2017-12-27"


def t(hhmm):
    return f"{DATE}T{hhmm[:2]}:{hhmm[2:]}:00+01:00"


def call(stop, order, arr=None, dep=None, cancelled=False):
    """arr and dep are (aimed, expected) in HHMM, or None."""
    s = f"          <EstimatedCall>\n            <StopPointRef>{stop}</StopPointRef>\n            <Order>{order}</Order>\n"
    if cancelled:
        s += "            <Cancellation>true</Cancellation>\n"
    if arr:
        s += f"            <AimedArrivalTime>{t(arr[0])}</AimedArrivalTime>\n            <ExpectedArrivalTime>{t(arr[1])}</ExpectedArrivalTime>\n"
    if dep:
        s += f"            <AimedDepartureTime>{t(dep[0])}</AimedDepartureTime>\n            <ExpectedDepartureTime>{t(dep[1])}</ExpectedDepartureTime>\n"
    return s + "          </EstimatedCall>\n"


def journey(line, trip, calls, cancelled=False, extra_code=None, recorded="1200"):
    s = "      <EstimatedVehicleJourney>\n"
    s += f"        <RecordedAtTime>{t(recorded)}</RecordedAtTime>\n"
    s += f"        <LineRef>{line}</LineRef>\n"
    if extra_code:
        s += f"        <EstimatedVehicleJourneyCode>{extra_code}</EstimatedVehicleJourneyCode>\n"
        s += "        <ExtraJourney>true</ExtraJourney>\n"
        s += "        <OperatorRef>RUT:Operator:130c</OperatorRef>\n"
    else:
        s += (
            "        <FramedVehicleJourneyRef>\n"
            f"          <DataFrameRef>{DATE}</DataFrameRef>\n"
            f"          <DatedVehicleJourneyRef>{trip}</DatedVehicleJourneyRef>\n"
            "        </FramedVehicleJourneyRef>\n"
        )
    if cancelled:
        s += "        <Cancellation>true</Cancellation>\n"
    s += "        <Monitored>true</Monitored>\n        <DataSource>HARNESS</DataSource>\n"
    s += "        <EstimatedCalls>\n" + "".join(calls) + "        </EstimatedCalls>\n"
    return s + "      </EstimatedVehicleJourney>\n"


def siri(journeys, seq):
    # The updater drops a delivery with a ResponseTimestamp older than the one before it, or older
    # than the start of OTP. So the response timestamps are in the future and increase per file.
    response = f"2099-01-01T00:00:{seq:02d}+01:00"
    return (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<Siri xmlns="http://www.siri.org.uk/siri" version="2.0">\n'
        "  <ServiceDelivery>\n"
        f"    <ResponseTimestamp>{response}</ResponseTimestamp>\n"
        "    <ProducerRef>HARNESS</ProducerRef>\n"
        "    <EstimatedTimetableDelivery version=\"2.0\">\n"
        f"      <ResponseTimestamp>{response}</ResponseTimestamp>\n"
        "      <EstimatedJourneyVersionFrame>\n"
        f"      <RecordedAtTime>{t('1200')}</RecordedAtTime>\n"
        + "".join(journeys)
        + "      </EstimatedJourneyVersionFrame>\n"
        "    </EstimatedTimetableDelivery>\n"
        "  </ServiceDelivery>\n"
        "</Siri>\n"
    )


def write(name, journeys):
    OUT.mkdir(parents=True, exist_ok=True)
    seq = int(name.split("-")[1])
    (OUT / name).write_text(siri(journeys, seq))


L4, L12 = "RUT:Line:4", "RUT:Line:12"
T4_A, T4_B = "RUT:ServiceJourney:4-101493-1098", "RUT:ServiceJourney:4-101468-583"
T12_A, T12_B = "RUT:ServiceJourney:12-101375-1000", "RUT:ServiceJourney:12-101375-1001"

# Step 1: a late trip on line 4, an early trip on line 12
write("et-1-delay.xml", [
    journey(L4, T4_A, [
        call("NSR:Quay:10682", 1, dep=("1430", "1433")),
        call("NSR:Quay:7184", 2, arr=("1450", "1455")),
    ]),
    journey(L12, T12_A, [
        call("NSR:Quay:7203", 1, dep=("0634", "0632")),
        call("NSR:Quay:8027", 2, arr=("0649", "0647")),
    ]),
])

# Step 2: a cancelled trip, and a trip with a cancelled stop
write("et-2-cancel.xml", [
    journey(L4, T4_B, [
        call("NSR:Quay:10681", 1, dep=("1110", "1110")),
        call("NSR:Quay:7183", 2, arr=("1129", "1129")),
    ], cancelled=True),
    journey(L12, T12_B, [
        call("NSR:Quay:8028", 1, dep=("0704", "0704")),
        call("NSR:Quay:7194", 2, arr=("0719", "0719"), cancelled=True),
    ]),
])

# Step 3: an extra journey on line 4
write("et-3-extra.xml", [
    journey(L4, None, [
        call("NSR:Quay:10682", 1, dep=("1600", "1600")),
        call("NSR:Quay:7184", 2, arr=("1620", "1620")),
    ], extra_code="HARNESS-EXTRA-1"),
])

# Step 4: a new update of the trip from step 1 replaces the old one
write("et-4-delay-again.xml", [
    journey(L4, T4_A, [
        call("NSR:Quay:10682", 1, dep=("1430", "1440")),
        call("NSR:Quay:7184", 2, arr=("1450", "1502")),
    ], recorded="1210"),
])
