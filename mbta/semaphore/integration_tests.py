#/usr/bin/env python
from datetime import date, time, datetime, timedelta
from random import randrange, getrandbits
import boto3
import json
import jsondiff
import re
import requests

SAVED_SEARCH = "Top 10 trip plans %2B 10 latest trip plans"
OTP_DEV = "https://dev.otp.mbtace.com"
OTP_PROD = "https://prod.otp.mbtace.com"

PREDEFINED_PLANS = [
    # Airport shuttles 1
    {"Terminal E - Arrivals Level::42.369344,-71.020238", "Wood Island::42.37964,-71.022865"},

    # Airport shuttles 2
    {"Assembly::42.392811,-71.077257", "Terminal C - Departures Level::42.366635,-71.017167"},

    # Connection between bus routes in the middle of the routes
    {"Lexington St @ Willow St::42.4732,-71.17253", "Main St, Woburn, MA, USA::42.4928705,-71.1544787"},

    # Logan express
    {"Back Bay::42.34735,-71.075727", "Logan International Airport, Boston, MA, USA::42.3658907,-71.017547"},

    # Continuous drop-off
    {"Wellington::42.40237,-71.077082", "49 Cushing Street, Medford, MA, USA::42.4142089,-71.0968295"}
]


def splunk_json_request(path):
    proxy_lambda = boto3.client("lambda")
    response = proxy_lambda.invoke(
        FunctionName='splunk-proxy',
        InvocationType='RequestResponse',
        Payload=json.dumps({
            "path": path
        }).encode('utf-8'),
    )
    return json.loads(response["Payload"].read()).get("body")


def splunk_get_search_name():
    data = splunk_json_request(f"/services/saved/searches/{SAVED_SEARCH}/history")

    if "entry" not in data or len(data.get("entry")) < 1 or "name" not in data.get("entry")[0]:
        raise RuntimeError("Unable to get results reference from splunk")

    return data.get("entry")[0].get("name")


def splunk_get_search_results(name):
    data = splunk_json_request(f"/services/search/jobs/{name}/results")

    if "results" not in data or len(data.get("results")) <= 10:
        raise RuntimeError("Unable to get trip data from splunk")

    return [{r.get("fromPlace"), r.get("toPlace")} for r in data.get("results")]


def get_testing_dates():
    # returns random times on next Wednesday and next Sunday

    tomorrow = (
        datetime.combine(date.today() + timedelta(days=1), time(randrange(6, 21), randrange(0, 59)))
    )

    return {tomorrow + timedelta(days=(2 - tomorrow.weekday()) % 7),
            tomorrow + timedelta(days=(6 - tomorrow.weekday()) % 7)}


def get_trip_plans(environment, from_place, to_place, trip_date, arrive_by):
    url = f"{(OTP_PROD if environment == 'prod' else OTP_DEV)}/otp/routers/default/plan"

    params = {
        "fromPlace": from_place,
        "toPlace": to_place,
        "date": trip_date.strftime("%Y-%m-%d"),
        "time": trip_date.strftime("%I:%M%p"),
        "showIntermediateStops": "true",
        "format": "json",
        "locale": "en",
        "mode": "TRAM,SUBWAY,FERRY,RAIL,BUS,WALK",
        "walkReluctance": "5",
        "arriveBy": "true" if arrive_by else "false",
    }

    return requests.get(url, params).json()


def process_plan(plan):
    # alerts come in random order, which sometimes causes plan comparison to fail
    def sort_alerts(json):
        plan = json.get("plan")
        for itinerary in plan.get("itineraries"):
            for leg in itinerary.get("legs"):
                alerts = leg.get("alerts")
                if alerts:
                    leg["alerts"] = sorted(alerts, key=lambda a: a["alertDescriptionText"])
        return json

    # temporary stops have random IDs
    def remove_random_stop_ids(json):
        if isinstance(json, list):
            return [remove_random_stop_ids(j) for j in json]
        elif isinstance(json, dict):
            result = {}
            for key, value in json.items():
                if key == "stopId":
                    result[key] = re.sub(r"(\w+:temp_0)\.\d+", r"\1-random", value)
                else:
                    result[key] = remove_random_stop_ids(value)
            return result
        else:
            return json

    plan = sort_alerts(plan)
    plan = remove_random_stop_ids(plan)
    return plan


def compare_plans(plan1, plan2, **kwargs):
    diff = {}

    try:
        assert ("plan" in plan1) == ("plan" in plan2)

        if "plan" in plan1:
            assert ("itineraries" in plan1.get("plan")) == ("itineraries" in plan2.get("plan"))

            if "itineraries" in plan1.get("plan"):
                plan1 = process_plan(plan1)
                plan2 = process_plan(plan2)

                diff = jsondiff.diff(
                    plan1.get("plan").get("itineraries"),
                    plan2.get("plan").get("itineraries"),
                    syntax='symmetric',
                )
                assert diff == {}

        print("[PASS] Plans are identical\n")
        return True

    except AssertionError:
        print("[FAIL] Plans are different:")
        print(json.dumps(diff, indent=2))

        if kwargs.get("local_run", False):
            dt = datetime.now().strftime("%Y%m%d-%H%M%S%f")

            def save(plan, prefix):
                filename = f"{dt}-{prefix}.json"
                with open(filename, "w") as f:
                    json.dump(plan, f, sort_keys=True, indent=2)
                    print(f"Plan saved to {filename}")

            save(plan1, "prod")
            save(plan2, "dev")

        print("\n")

        return False


if __name__ == "__main__":
    sname = splunk_get_search_name()
    trips = splunk_get_search_results(sname)

    has_errors = False

    for fromPlace, toPlace in PREDEFINED_PLANS + trips:
        for date in get_testing_dates():
            arriveBy = bool(getrandbits(1))

            print(f"Comparing plans from {fromPlace} to {toPlace} {'arriving by' if arriveBy else 'departing on'} {date}")

            prod_plans = get_trip_plans("prod", fromPlace, toPlace, date, arriveBy)
            dev_plans = get_trip_plans("dev", fromPlace, toPlace, date, arriveBy)

            if not compare_plans(prod_plans, dev_plans, local_run=False):
                has_errors = True

    if has_errors:
        print("Tests completed with errors")
        exit(1)
    else:
        print("Tests completed successfully")
        exit(0)
