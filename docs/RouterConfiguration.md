# Router configuration

This section covers all options that can be set for each router using the `router-config.json` file.
These options can be applied by the OTP server without rebuilding the graph.

config key | description | value type | value default | notes
---------- | ----------- | ---------- | ------------- | -----
`routingDefaults` | Default routing parameters, which will be applied to every request | object |  | see [routing defaults](#routing-defaults)
`streetRoutingTimeout` | maximum time limit for street route queries | double | null | units: seconds; see [timeout](#timeout)
`requestLogFile` | Path to a plain-text file where requests will be logged | string | null | see [logging incoming requests](#logging-incoming-requests)
`transit` | Transit tuning parameters | `TransitRoutingConfig` |  | see [Tuning transit routing](#Tuning-transit-routing)
`updaters` | configure real-time updaters, such as GTFS-realtime feeds | object | null | see [configuring real-time updaters](#configuring-real-time-updaters)
`transmodelApi` | configure Entur Transmodel API (**Sandbox**) | object | null | See the code for parameters, no doc provided.


## Routing defaults

There are many trip planning options used in the OTP web API, and more exist
internally that are not exposed via the API. You may want to change the default value for some of these parameters,
i.e. the value which will be applied unless it is overridden in a web API request.

A full list of them can be found in the [RoutingRequest](https://github.com/opentripplanner/OpenTripPlanner/blob/v2.1.0/src/main/java/org/opentripplanner/routing/api/request/RoutingRequest.java).
Any public field or setter method in this class can be given a default value using the routingDefaults section of
`router-config.json` as follows:

```JSON
// router-config.json
{
    "routingDefaults": {
        "walkSpeed": 2.0,
        "stairsReluctance": 4.0,
        "carDropoffTime": 240
    }
}
```


### Tuning transfer optimization

The main purpose of transfer optimization is to handle cases where it is possible to transfer between two routes at more than one point (pair of stops). The transfer optimization ensures that transfers occur at the best possible location. By post-processing all paths returned by the router, OTP can apply sophisticated calculations that are too slow or not algorithmically valid within Raptor. Transfers are optimized before the paths are passed to the itinerary-filter-chain.

For a detailed description of the design and the optimization calculations see the [design documentation](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/src/main/java/org/opentripplanner/routing/algorithm/transferoptimization/package.md) (dev-2.x latest).

#### Transfer optimization configuration

To toggle transfer optimization on or off use the OTPFeature `OptimizeTransfers` (default is on).
You should leave this on unless there is a critical issue with it. The OTPFeature `GuaranteedTransfers` will toggle on and off the priority optimization (part of OptimizeTransfers).

The optimized transfer service will try to, in order:

1. Use transfer priority. This includes stay-seated and guaranteed transfers.
2. Use the transfers with the best distribution of the wait-time, and avoid very short transfers.
3. Avoid back-travel
4. Boost stop-priority to select preferred and recommended stops.

If two paths have the same transfer priority level, then we break the tie by looking at waiting
times. The goal is to maximize the wait-time for each stop, avoiding situations where there is
little time available to make the transfer. This is balanced with the generalized-cost. The cost is
adjusted with a new cost for wait-time (optimized-wait-time-cost).

The defaults should work fine, but if you have results with short wait-times dominating a better
option or "back-travel", then try to increase the `minSafeWaitTimeFactor`,
`backTravelWaitTimeFactor` and/or `extraStopBoardAlightCostsFactor`.

```JSON
// router-config.json
{
  "routingDefaults": {
    "transferOptimization": {
      "optimizeTransferWaitTime": true,
      "minSafeWaitTimeFactor": 5.0,
      "backTravelWaitTimeFactor": 1.0,
      "extraStopBoardAlightCostsFactor": 2.5
    }
  }
}
```
See the [TransferOptimizationParameters](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/src/main/java/org/opentripplanner/routing/algorithm/transferoptimization/api/TransferOptimizationParameters.java) (dev-2.x latest) for a description of these parameters.


### Tuning itinerary filtering
Nested inside `routingDefaults { itineraryFilters{...} }` in `router-config.json`.

The purpose of the itinerary filter chain is to post process the result returned by the routing
search. The filters may modify itineraries, sort them, and filter away less preferable results.

OTP2 may produce numerous _pareto-optimal_ results when using `time`, `number-of-transfers` and
`generalized-cost` as criteria. Use the parameters listed here to reduce/filter the itineraries
return by the search engine before returning the results to client. There is also a few mandatory
none configurable filters removing none optimal results. You may see these filters pop-up in the
filter debugging.

config key | description | value type | value default
---------- | ----------- | ---------- | -------------
`debug` | Enable this to attach a system notice to itineraries instead of removing them. This is very convenient when tuning the filters. | boolean | `false`
`groupSimilarityKeepOne` | Pick ONE itinerary from each group after putting itineraries that is 85% similar together. | double | `0.85` (85%)
`groupSimilarityKeepThree` | Reduce the number of itineraries to three itineraries by reducing each group of itineraries grouped by 68% similarity. | double | `0.68` (68%)
`groupedOtherThanSameLegsMaxCostMultiplier` | Filter grouped itineraries, where the non-grouped legs are more expensive than in the lowest cost one.  | double | `2.0` (2x cost)
`transitGeneralizedCostLimit` | A relative maximum limit for the generalized cost for transit itineraries. The limit is a linear function of the minimum generalized-cost. The function is used to calculate a max-limit. The max-limit is then used to to filter by generalized-cost. Transit itineraries with a cost higher than the max-limit is dropped from the result set. None transit itineraries is excluded from the filter. To set a filter to be _1 hour plus 2 times the best cost_ use: `3600 + 2.0 x`. To set an absolute value(3000s) use: `3000 + 0x`  | linear function | `3600 + 2.0 x`
`nonTransitGeneralizedCostLimit` | A relative maximum limit for the generalized cost for non-transit itineraries. The max limit is calculated using ALL itineraries, but only non-transit itineraries will be filtered out. The limit is a linear function of the minimum generalized-cost. The function is used to calculate a max-limit. The max-limit is then used to to filter by generalized-cost. Non-transit itineraries with a cost higher than the max-limit is dropped from the result set. To set a filter to be _1 hour plus 2 times the best cost_ use: `3600 + 2.0 x`. To set an absolute value(3000s) use: `3000 + 0x`  | linear function | `3600 + 2.0 x`
`bikeRentalDistanceRatio` | For routes that consist only of bike rental and walking what is the minimum fraction of _distance_ of the bike rental leg. This filters out results that consist of a long walk plus a relatively short bike rental leg. A value of `0.3` means that a minimum of 30% of the total distance must be spent on the bike in order for the result to be included. | double | `0.0`
`parkAndRideDurationRatio` | For P+R routes that consist only of driving and walking what is the minimum fraction of _time_ of the driving leg. This filters out results that consist of driving plus a very long walk leg at the end. A value of `0.3` means that a minimum of 30% of the total time must be spent in the car in order for the result to be included. However, if there is only a single result, it is never filtered. | double | `0.0`


#### Group by similarity filters

The group-by-filter is a bit complex, but should be simple to use. Set `debug=true` and experiment
with `searchWindow` and the three group-by parameters(`groupSimilarityKeepOne`,
`groupSimilarityKeepThree` and `groupedOtherThanSameLegsMaxCostMultiplier`).

The group-by-filter work by grouping itineraries together and then reducing the number of
itineraries in each group, keeping the itinerary/itineraries with the best itinerary
_generalized-cost_. The group-by function first pick all transit legs that account for more than N%
of the itinerary based on distance traveled. This become the group-key. Two keys are the same if all
legs in one of the keys also exist in the other. Note, one key may have a lager set of legs than
the other, but they can still be the same. When comparing two legs we compare the `tripId` and make
sure the legs overlap in place and time. Two legs are the same if both legs ride at least a common
subsection of the same trip. The `keepOne` filter will keep ONE itinerary in each group. The
`keepThree` keeps 3 itineraries for each group.

The grouped itineraries can be further reduced by using `groupedOtherThanSameLegsMaxCostMultiplier`.
This parameter filters out itineraries, where the legs that are not common for all the grouped
itineraries have a much higher cost, than the lowest in the group. By default, it filters out
itineraries that are at least double in cost for the non-grouped legs.


### Drive-to-transit routing defaults

When using the "park and ride" or "kiss and ride" modes (drive to transit), the initial driving time to reach a transit
stop or park and ride facility is constrained. You can set a drive time limit in seconds by adding a line like
`maxPreTransitTime = 1200` to the routingDefaults section. If the limit is too high on a very large street graph, routing
performance may suffer.


## Boarding and alighting times

Sometimes there is a need to configure a longer ride or alighting times for specific modes, such as airplanes or ferries,
where the check-in process needs to be done in good time before ride. The ride time is added to the time when going
from the stop (offboard) vertex to the onboard vertex, and the alight time is added vice versa. The times are configured as
seconds needed for the ride and alighting processes in `router-config.json` as follows:

```JSON
// router-config.json
{
  "routingDefaults": {
    "boardSlackForMode": {
      "AIRPLANE": 2700
    },
    "alightSlackForMode": {
      "AIRPLANE": 1200
    }
  }
}
```

## Timeout

In OTP1 path searches sometimes toke a long time to complete. With the new Raptor algorithm this not
the case anymore. The street part of the routing may still take a long time if searching very long
distances. You can set the street routing timeout to avoid tying up server resources on pointless
searches and ensure that your users receive a timely response. You can also limit the max distance
to search for WALK, BIKE and CAR. When a search times out, a WARN level log entry is made with
information that can help identify problematic searches and improve our routing methods. There are
no timeouts for the transit part of the routing search, instead configure a reasonable dynamic
search-window. To set the street routing timeout use the following config:

```JSON
// router-config.json
{
  "streetRoutingTimeout": 5.5
}
```

This specifies a timeout in (optionally fractional) seconds. The search abort after this many seconds and any paths found are returned to the client.

##maxAccessEgressDurationSecondsForMode

Override the settings in maxAccessEgressDurationSeconds for specific street modes. This is done because
some street modes searches are much more resource intensive than others.

```JSON
// router-config.json
"maxAccessEgressDurationSecondsForMode": {
  "BIKE_RENTAL": 1200
}
```

This will limit only the BIKE_RENTAL mode to 1200 seconds, while keeping the default limit for all
other access/egress modes.

## Logging incoming requests

You can log some characteristics of trip planning requests in a file for later analysis. Some transit agencies and
operators find this information useful for identifying existing or unmet transportation demand. Logging will be
performed only if you specify a log file name in the router config:

```JSON
// router-config.json
{
  "requestLogFile": "/var/otp/request.log"
}
```

Each line in the resulting log file will look like this:

`2016-04-19T18:23:13.486 0:0:0:0:0:0:0:1 ARRIVE 2016-04-07T00:17 WALK,BUS,CABLE_CAR,TRANSIT,BUSISH 45.559737193889966 -122.64999389648438 45.525592487765635 -122.39044189453124 6095 3 5864 3 6215 3`

The fields separated by whitespace are (in order):

1. Date and time the request was received
2. IP address of the user
3. Arrive or depart search
4. The arrival or departure time
5. A comma-separated list of all transport modes selected
6. Origin latitude and longitude
7. Destination latitude and longitude

Finally, for each itinerary returned to the user, there is a travel duration in seconds and the number of transit vehicles used in that itinerary.


## Tuning transit routing
Nested inside `transit {...}` in `router-config.json`.

Some of these parameters for tuning transit routing is only available through configuration and cannot be set in the routing request. These parameters work together with the default routing request and the actual routing request.

config key | description | value type | value default
---------- | ----------- | ---------- | -------------
`maxNumberOfTransfers` | Use this parameter to allocate enough space for Raptor. Set it to the maximum number of transfers for any given itinerary expected to be found within the entire transit network. The memory overhead of setting this higher than the maximum number of transfers is very little so it is better to set it too high then to low. | int | `12`
`scheduledTripBinarySearchThreshold` | The threshold is used to determine when to perform a binary trip schedule search to reduce the number of trips departure time lookups and comparisons. When testing with data from Entur and all of Norway as a Graph, the optimal value was around 50. Changing this may improve the performance with just a few percent. | int | `50`
`iterationDepartureStepInSeconds` | Step for departure times between each RangeRaptor iterations. A transit network usually uses minute resolution for its depature and arrival times. To match that, set this variable to 60 seconds. | int | `60`
`searchThreadPoolSize` | Split a travel search in smaller jobs and run them in parallel to improve performance. Use this parameter to set the total number of executable threads available across all searches. Multiple searches can run in parallel - this parameter have no effect with regard to that. If 0, no extra threads are started and the search is done in one thread. | int | `0`
`dynamicSearchWindow` | The dynamic search window coefficients used to calculate the EDT(earliest-departure-time), LAT(latest-arrival-time) and SW(raptor-search-window) using heuristics. | object | `null`
`stopTransferCost` | Use this to set a stop transfer cost for the given [TransferPriority](https://github.com/opentripplanner/OpenTripPlanner/blob/v2.1.0/src/main/java/org/opentripplanner/model/TransferPriority.java). The cost is applied to boarding and alighting at all stops. All stops have a transfer cost priority set, the default is `ALLOWED`. The `stopTransferCost` parameter is optional, but if listed all values must be set. | enum map | `null`
`transferCacheMaxSize` | The maximum number of distinct transfers parameters (`RoutingRequest`s) to cache pre-calculated transfers for. If too low, requests may be slower. If too high, more memory may be used then required. | int | `25`
`pagingSearchWindowAdjustments` | The provided array of durations is used to increase the search-window for the next/previous page when the current page return few options. If ZERO results is returned the first duration in the list is used, if ONE result is returned then the second duration is used and so on. The duration is added to the existing search-window and inserted into the next and previous page cursor. See JavaDoc for [TransitTuningParameters#pagingSearchWindowAdjustments](https://github.com/opentripplanner/OpenTripPlanner/blob/v2.1.0/src/main/java/org/opentripplanner/routing/algorithm/raptor/transit/TransitTuningParameters.java) for more info.  | duration[] | `["4h", "2h", "1h", "30m", "20m", "10m"]`

### Tuning transit routing - Dynamic search window
Nested inside `transit : { dynamicSearchWindow : { ... } }` in `router-config.json`.

config key | description | value type | value default
---------- | ----------- | ---------- | -------------
`minTransitTimeCoefficient` | The coefficient to multiply with minimum transit time found using a heuristic search. This scaled value is added to the `minWinTimeMinutes`. A value between `0.0` to `3.0` is expected to give ok results. | double | `0.5`
`minWaitTimeCoefficient` | The coefficient to multiply with a minimum wait time estimated based on the heuristic search. This will increase the search-window in low transit frequency areas. This value is added to the `minWinTimeMinutes`. A value between `0.0` to `1.0` is expected to give ok results. | double | `0.5`
`minWinTimeMinutes` | The constant minimum number of minutes for a raptor search window. Use a value between 20-180 minutes in a normal deployment. | int | `40`
`maxWinTimeMinutes` | Set an upper limit to the calculation of the dynamic search window to prevent exceptionable cases to cause very long search windows. Long search windows consumes a lot of resources and may take a long time. Use this parameter to tune the desired maximum search time. | int | `180` (3 hours)
`stepMinutes` | The search window is rounded of to the closest multiplication of N minutes. If N=10 minutes, the search-window can be 10, 20, 30 ... minutes. It the computed search-window is 5 minutes and 17 seconds it will be rounded up to 10 minutes. | int | `10`


### Tuning transit routing - Stop transfer cost
Nested inside `transit : { stopTransferCost : { ... } }` in `router-config.json`.

This _cost_ is in addition to other costs like `boardCost` and indirect cost from waiting (board-/alight-/transfer slack). You should account for this when you tune the routing search parameters.

If not set the `stopTransferCost` is ignored. This is only available for NeTEx imported Stops.

The cost is a scalar, but is equivalent to the felt cost of riding a transit trip for 1 second.

config key | description | value type
---------- | ----------- | ---------- 
`DISCOURAGED` | Use a very high cost like `72 000` to eliminate transfers ath the stop if not the only option. | int
`ALLOWED` | Allowed, but not recommended. Use something like `150`. | int
`RECOMMENDED` | Use a small cost penalty like `60`. | int
`PREFERRED` | The best place to do transfers. Should be set to `0`(zero). | int

Use values in a range from `0` to `100 000`. **All key/value pairs are required if the `stopTransferCost` is listed.**


### Transit example
```JSON
// router-config.json
{
    "transit": {
        "maxNumberOfTransfers": 12,
        "scheduledTripBinarySearchThreshold": 50,
        "iterationDepartureStepInSeconds": 60,
        "searchThreadPoolSize": 0,
        "dynamicSearchWindow": {
            "minTransitTimeCoefficient" : 0.5,
            "minWaitTimeCoefficient" : 0.5,
            "minTimeMinutes": 30,
            "maxLengthMinutes" : 360,
            "stepMinutes": 10
        },
        "stopTransferCost": {
            "DISCOURAGED": 72000,
            "ALLOWED":       150,
            "RECOMMENDED":    60,
            "PREFERRED":       0
        }
    }
}
```

## Real-time data

GTFS feeds contain *schedule* data that is is published by an agency or operator in advance. The feed does not account
for unexpected service changes or traffic disruptions that occur from day to day. Thus, this kind of data is also
referred to as 'static' data or 'theoretical' arrival and departure times.

### GTFS-Realtime

The [GTFS-RT spec](https://developers.google.com/transit/gtfs-realtime/) complements GTFS with three additional kinds of
feeds. In contrast to the base GTFS schedule feed, they provide *real-time* updates (*'dynamic'* data) and are are
updated from minute to minute.

- **Alerts** are text messages attached to GTFS objects, informing riders of disruptions and changes.

- **TripUpdates** report on the status of scheduled trips as they happen, providing observed and predicted arrival and
  departure times for the remainder of the trip.

- **VehiclePositions** give the location of some or all vehicles currently in service, in terms of geographic coordinates
  or position relative to their scheduled stops.

### Vehicle rental systems using GBFS

Besides GTFS-RT transit data, OTP can also fetch real-time data about vehicle rental networks including the number
of bikes and free parking spaces at each station. We support vehicle rental systems from using GBFS feed format.

### Vehicle parking (sandbox feature)

Vehicle parking options and configuration is documented in its [sandbox documentation](sandbox/VehicleParking.md).

### Configuring real-time updaters

Real-time data can be provided using either a pull or push system. In a pull configuration, the GTFS-RT consumer polls the
real-time provider over HTTP. That is to say, OTP fetches a file from a web server every few minutes. In the push
configuration, the consumer opens a persistent connection to the GTFS-RT provider, which then sends incremental updates
immediately as they become available. OTP can use both approaches. The [OneBusAway GTFS-realtime exporter project](https://github.com/OneBusAway/onebusaway-gtfs-realtime-exporter) provides this kind of streaming, incremental updates over a websocket rather than a single large file.

Real-time data sources are configured in `router-config.json`. The `updaters` section is an array of JSON objects, each
of which has a `type` field and other configuration fields specific to that type. Common to all updater entries that
connect to a network resource is the `url` field.

```JSON
// router-config.json
{
    // Routing defaults are any public field or setter in the Java class
    // org.opentripplanner.routing.api.request.RoutingRequest
    "routingDefaults": {
        "numItineraries": 6,
        "walkSpeed": 2.0,
        "stairsReluctance": 4.0,
        "carDropoffTime": 240
    },

    "updaters": [

        // GTFS-RT service alerts (frequent polling)
        {
            "type": "real-time-alerts",
            "frequencySec": 30,
            "url": "http://developer.trimet.org/ws/V1/FeedSpecAlerts/appID/0123456789ABCDEF",
            "feedId": "TriMet"
        },

        //<!--- Tampa Area GBFS bike share -->
        {
          "type": "vehicle-rental",
          "frequencySec": 300,
          "sourceType": "gbfs",
          "url": "http://coast.socialbicycles.com/opendata/gbfs.json"
        },

        // Vehicle parking availability
        {
            "type": "vehicle-parking"
        },

        // Polling for GTFS-RT TripUpdates)
        {
            "type": "stop-time-updater",
            "frequencySec": 60,
            // this is either http or file... shouldn't it default to http or guess from the presence of a URL?
            "sourceType": "gtfs-http",
            "url": "http://developer.trimet.org/ws/V1/TripUpdate/appID/0123456789ABCDEF",
            "feedId": "TriMet"
        },

        // Streaming differential GTFS-RT TripUpdates over websockets
        {
            "type": "websocket-gtfs-rt-updater"
        }
    ]
}
```

#### GBFS Configuration

[GBFS](https://github.com/NABSA/gbfs) is used for a variety of shared mobility services, with partial support for both v1 and v2.2 ([list of known GBFS feeds](https://github.com/NABSA/gbfs/blob/master/systems.csv)).

To add a GBFS feed to the router add one entry in the `updater` field of `router-config.json` in the format:

```JSON
// router-config.json
{
   "type": "vehicle-rental",
   "sourceType": "gbfs",
   // frequency in seconds in which the GBFS service will be polled
   "frequencySec": 60,
   // The URL of the GBFS feed auto-discovery file
   "url": "http://coast.socialbicycles.com/opendata/gbfs.json",
   // Optionally specify the language version of the feed to use. If no language is set, the first language in the feed is used. 
   "language": "en",
   // if it should be possible to arrive at the destination with a rented bicycle, without dropping it off
   "allowKeepingRentedBicycleAtDestination": true
}
```

##### Arriving with rental bikes at the destination

In some cases it may be useful to not drop off the rented bicycle before arriving at the
destination. This is useful if bicycles may only be rented for round trips, or the destination is an
intermediate place.

For this to be possible three things need to be configured:

1. In the updater configuration `allowKeepingRentedBicycleAtDestination` should be set to `true`.

2. `allowKeepingRentedBicycleAtDestination` should also be set for each request, either using
   [routing defaults](#routing-defaults), or per-request.

3. If keeping the bicycle at the destination should be discouraged, then
   `keepingRentedBicycleAtDestinationCost` (default: `0`) may also be set in the
   [routing defaults](#routing-defaults).

#### Vehicle Rental Service Directory configuration (sandbox feature)

To configure and url for the [VehicleRentalServiceDirectory](sandbox/VehicleRentalServiceDirectory.md).

```JSON
// router-config.json
{
  "vehicleRentalServiceDirectory": {
    "url": "https://api.dev.entur.io/mobility/v1/bikes"
  }
}
```

# Configure using command-line arguments

Certain settings can be provided on the command line, when starting OpenTripPlanner. See the `CommandLineParameters` class for [a full list of arguments](https://github.com/opentripplanner/OpenTripPlanner/blob/v2.1.0/src/main/java/org/opentripplanner/standalone/config/CommandLineParameters.java).
