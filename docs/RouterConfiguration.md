<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc-templates
   - Generated directory is: /docs 
-->

# Router configuration

This section covers all options that can be set for each router using the `router-config.json` file.
These options can be applied by the OTP server without rebuilding the graph.

## Configure using command-line arguments

Certain settings can be provided on the command line, when starting OpenTripPlanner. See
the `CommandLineParameters` class
for [a full list of arguments](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/src/main/java/org/opentripplanner/standalone/config/CommandLineParameters.java)
.

## Routing defaults

There are many trip planning options used in the OTP web API, and more exist internally that are not
exposed via the API. You may want to change the default value for some of these parameters, i.e. the
value which will be applied unless it is overridden in a web API request.

A full list of them can be found in the [RouteRequest](RouteRequest.md).


## Parameter Summary

<!-- PARAMETERS-TABLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                                                          |          Type         | Summary                                                                                           |  Req./Opt. | Default Value | Since |
|-------------------------------------------------------------------------------------------|:---------------------:|---------------------------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| [configVersion](#configVersion)                                                           |        `string`       | Deployment version of the *router-config.json*.                                                   | *Optional* |               |  2.1  |
| [requestLogFile](#requestLogFile)                                                         |        `string`       | The path of the log file for the requests.                                                        | *Optional* |               |  2.0  |
| [streetRoutingTimeout](#streetRoutingTimeout)                                             |       `duration`      | The maximum time a street routing request is allowed to take before returning a timeout.          | *Optional* | `"PT5S"`      |  2.2  |
| [flex](sandbox/Flex.md)                                                                   |        `object`       | Configuration for flex routing.                                                                   | *Optional* |               |  2.1  |
| [rideHailingServices](sandbox/RideHailing.md)                                             |       `object[]`      | Configuration for interfaces to external ride hailing services like Uber.                         | *Optional* |               |  2.3  |
| [routingDefaults](RouteRequest.md)                                                        |        `object`       | The default parameters for the routing query.                                                     | *Optional* |               |  2.0  |
| timetableUpdates                                                                          |        `object`       | Global configuration for timetable updaters.                                                      | *Optional* |               |  2.2  |
| [transit](#transit)                                                                       |        `object`       | Configuration for transit searches with RAPTOR.                                                   | *Optional* |               |   na  |
|    [iterationDepartureStepInSeconds](#transit_iterationDepartureStepInSeconds)            |       `integer`       | Step for departure times between each RangeRaptor iterations.                                     | *Optional* | `60`          |   na  |
|    [maxNumberOfTransfers](#transit_maxNumberOfTransfers)                                  |       `integer`       | This parameter is used to allocate enough memory space for Raptor.                                | *Optional* | `12`          |   na  |
|    [scheduledTripBinarySearchThreshold](#transit_scheduledTripBinarySearchThreshold)      |       `integer`       | This threshold is used to determine when to perform a binary trip schedule search.                | *Optional* | `50`          |   na  |
|    [searchThreadPoolSize](#transit_searchThreadPoolSize)                                  |       `integer`       | Split a travel search in smaller jobs and run them in parallel to improve performance.            | *Optional* | `0`           |   na  |
|    [transferCacheMaxSize](#transit_transferCacheMaxSize)                                  |       `integer`       | The maximum number of distinct transfers parameters to cache pre-calculated transfers for.        | *Optional* | `25`          |   na  |
|    [dynamicSearchWindow](#transit_dynamicSearchWindow)                                    |        `object`       | The dynamic search window coefficients used to calculate the EDT, LAT and SW.                     | *Optional* |               |  2.1  |
|       [maxWindow](#transit_dynamicSearchWindow_maxWindow)                                 |       `duration`      | Upper limit for the search-window calculation.                                                    | *Optional* | `"PT3H"`      |  2.2  |
|       [minTransitTimeCoefficient](#transit_dynamicSearchWindow_minTransitTimeCoefficient) |        `double`       | The coefficient to multiply with `minTransitTime`.                                                | *Optional* | `0.5`         |  2.1  |
|       [minWaitTimeCoefficient](#transit_dynamicSearchWindow_minWaitTimeCoefficient)       |        `double`       | The coefficient to multiply with `minWaitTime`.                                                   | *Optional* | `0.5`         |  2.1  |
|       [minWindow](#transit_dynamicSearchWindow_minWindow)                                 |       `duration`      | The constant minimum duration for a raptor-search-window.                                         | *Optional* | `"PT40M"`     |  2.2  |
|       [stepMinutes](#transit_dynamicSearchWindow_stepMinutes)                             |       `integer`       | Used to set the steps the search-window is rounded to.                                            | *Optional* | `10`          |  2.1  |
|    [pagingSearchWindowAdjustments](#transit_pagingSearchWindowAdjustments)                |      `duration[]`     | The provided array of durations is used to increase the search-window for the next/previous page. | *Optional* |               |   na  |
|    [stopTransferCost](#transit_stopTransferCost)                                          | `enum map of integer` | Use this to set a stop transfer cost for the given transfer priority                              | *Optional* |               |  2.0  |
|    [transferCacheRequests](#transit_transferCacheRequests)                                |       `object[]`      | Routing requests to use for pre-filling the stop-to-stop transfer cache.                          | *Optional* |               |  2.3  |
| transmodelApi                                                                             |        `object`       | Configuration for the Transmodel GraphQL API.                                                     | *Optional* |               |  2.1  |
|    [hideFeedId](#transmodelApi_hideFeedId)                                                |       `boolean`       | Hide the FeedId in all API output, and add it to input.                                           | *Optional* | `false`       |   na  |
|    [tracingHeaderTags](#transmodelApi_tracingHeaderTags)                                  |       `string[]`      | Used to group requests when monitoring OTP.                                                       | *Optional* |               |   na  |
| [updaters](UpdaterConfig.md)                                                              |       `object[]`      | Configuration for the updaters that import various types of data into OTP.                        | *Optional* |               |  1.5  |
| [vectorTileLayers](sandbox/MapboxVectorTilesApi.md)                                       |       `object[]`      | Configuration of the individual layers for the Mapbox vector tiles.                               | *Optional* |               |  2.0  |
| vehicleRentalServiceDirectory                                                             |        `object`       | Configuration for the vehicle rental service directory.                                           | *Optional* |               |  2.0  |
|    language                                                                               |        `string`       | Language code.                                                                                    | *Optional* |               |   na  |
|    sourcesName                                                                            |        `string`       | Json tag name for updater sources.                                                                | *Optional* | `"systems"`   |   na  |
|    updaterNetworkName                                                                     |        `string`       | Json tag name for the network name for each source.                                               | *Optional* | `"id"`        |   na  |
|    updaterUrlName                                                                         |        `string`       | Json tag name for endpoint urls for each source.                                                  | *Optional* | `"url"`       |   na  |
|    url                                                                                    |         `uri`         | Endpoint for the VehicleRentalServiceDirectory                                                    | *Required* |               |   na  |
|    [headers](#vehicleRentalServiceDirectory_headers)                                      |    `map of string`    | Http headers.                                                                                     | *Optional* |               |   na  |

<!-- PARAMETERS-TABLE END -->


## Parameter Details

<!-- PARAMETERS-DETAILS BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

<h3 id="configVersion">configVersion</h3>

**Since version:** `2.1` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** / 

Deployment version of the *router-config.json*.

The config-version is a parameter which each OTP deployment may set to be able to query the
OTP server and verify that it uses the correct version of the config. The version should be
injected into the config in the (continuous) deployment pipeline. How this is done, is up to
the deployment.

The config-version has no effect on OTP, and is provided as is on the API. There is no syntax
or format check on the version and it can be any string.

Be aware that OTP uses the config embedded in the loaded graph if no new config is provided.


<h3 id="requestLogFile">requestLogFile</h3>

**Since version:** `2.0` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** / 

The path of the log file for the requests.

You can log some characteristics of trip planning requests in a file for later analysis. Some
transit agencies and operators find this information useful for identifying existing or unmet
transportation demand. Logging will be performed only if you specify a log file name in the router
config.

Each line in the resulting log file will look like this:

```
2016-04-19T18:23:13.486 0:0:0:0:0:0:0:1 ARRIVE 2016-04-07T00:17 WALK,BUS,CABLE_CAR,TRANSIT,BUSISH 45.559737193889966 -122.64999389648438 45.525592487765635 -122.39044189453124 6095 3 5864 3 6215 3
```

The fields separated by whitespace are (in order):

1. Date and time the request was received
2. IP address of the user
3. Arrive or depart search
4. The arrival or departure time
5. A comma-separated list of all transport modes selected
6. Origin latitude and longitude
7. Destination latitude and longitude

Finally, for each itinerary returned to the user, there is a travel duration in seconds and the
number of transit vehicles used in that itinerary.


<h3 id="streetRoutingTimeout">streetRoutingTimeout</h3>

**Since version:** `2.2` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT5S"`   
**Path:** / 

The maximum time a street routing request is allowed to take before returning a timeout.

In OTP1 path searches sometimes took a long time to complete. With the new Raptor algorithm this is not
the case anymore. The street part of the routing may still take a long time if searching very long
distances. You can set the street routing timeout to avoid tying up server resources on pointless
searches and ensure that your users receive a timely response. You can also limit the max distance
to search for WALK, BIKE and CAR. When a search times out, a WARN level log entry is made with
information that can help identify problematic searches and improve our routing methods. There are
no timeouts for the transit part of the routing search, instead configure a reasonable dynamic
search-window.

The search aborts after this duration and any paths found are returned to the client.


<h3 id="transit">transit</h3>

**Since version:** `na` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** / 

Configuration for transit searches with RAPTOR.

Some of these parameters for tuning transit routing are only available through configuration and
cannot be set in the routing request. These parameters work together with the default routing
request and the actual routing request.


<h3 id="transit_iterationDepartureStepInSeconds">iterationDepartureStepInSeconds</h3>

**Since version:** `na` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `60`   
**Path:** /transit 

Step for departure times between each RangeRaptor iterations.

This is a performance optimization parameter. A transit network usually uses minute resolution for
the timetables, so to match that, set this variable to 60 seconds. Setting it to less than 60 will
not give better result, but degrade performance. Setting it to 120 seconds will improve performance,
but you might get a slack of 60 seconds somewhere in the result.


<h3 id="transit_maxNumberOfTransfers">maxNumberOfTransfers</h3>

**Since version:** `na` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `12`   
**Path:** /transit 

This parameter is used to allocate enough memory space for Raptor.

Set it to the maximum number of transfers for any given itinerary expected to be found within the
entire transit network. The memory overhead of setting this higher than the maximum number of
transfers is very little so it is better to set it too high than to low.


<h3 id="transit_scheduledTripBinarySearchThreshold">scheduledTripBinarySearchThreshold</h3>

**Since version:** `na` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `50`   
**Path:** /transit 

This threshold is used to determine when to perform a binary trip schedule search.

This reduce the number of trips departure time lookups and comparisons. When testing with data from
Entur and all of Norway as a Graph, the optimal value was about 50. If you calculate the departure
time every time or want to fine tune the performance, changing this may improve the performance a
few percents.


<h3 id="transit_searchThreadPoolSize">searchThreadPoolSize</h3>

**Since version:** `na` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0`   
**Path:** /transit 

Split a travel search in smaller jobs and run them in parallel to improve performance.

Use this parameter to set the total number of executable threads available across all searches.
Multiple searches can run in parallel - this parameter have no effect with regard to that. If 0,
no extra threads are started and the search is done in one thread.


<h3 id="transit_transferCacheMaxSize">transferCacheMaxSize</h3>

**Since version:** `na` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `25`   
**Path:** /transit 

The maximum number of distinct transfers parameters to cache pre-calculated transfers for.

 If too low, requests may be slower. If too high, more memory may be used then required.

<h3 id="transit_dynamicSearchWindow">dynamicSearchWindow</h3>

**Since version:** `2.1` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transit 

The dynamic search window coefficients used to calculate the EDT, LAT and SW.

The dynamic search window coefficients is used to calculate EDT(*earliest-departure-time*),
LAT(*latest-arrival-time*) and SW(*raptor-search-window*) request parameters using heuristics. The
heuristics perform a Raptor search (one-iteration) to find a trip which we use to find a lower
bound for the travel duration time - the "minTransitTime". The heuristic search is used for other
purposes too, and is very fast.

At least the EDT or the LAT must be passed into Raptor to perform a Range Raptor search. If
unknown/missing the parameters(EDT, LAT, DW) are dynamically calculated. The dynamic coefficients
affect the performance and should be tuned to match the deployment.

The request parameters are calculated like this:

```
    DW  = round_N(C + T * minTransitTime + W * minWaitTime)
    LAT = EDT + DW + minTransitTime
    EDT = LAT - (DW + minTransitTime)
```

The `round_N(...)` method rounds the input to the closest multiplication of N.

The 3 coefficients above are:

 - `C` is parameter: `minWindow`
 - `T` is parameter: `minTransitTimeCoefficient`
 - `W` is parameter: `minWaitTimeCoefficient`
 - `N` is parameter: `stepMinutes`

In addition there is an upper bound on the calculation of the search window:
`maxWindow`.


<h3 id="transit_dynamicSearchWindow_maxWindow">maxWindow</h3>

**Since version:** `2.2` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT3H"`   
**Path:** /transit/dynamicSearchWindow 

Upper limit for the search-window calculation.

Long search windows consumes a lot of resources and may take a long time. Use this parameter to
tune the desired maximum search time.

This is the parameter that affects the response time most, the downside is that a search is only
guaranteed to be pareto-optimal within a search-window.


<h3 id="transit_dynamicSearchWindow_minTransitTimeCoefficient">minTransitTimeCoefficient</h3>

**Since version:** `2.1` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.5`   
**Path:** /transit/dynamicSearchWindow 

The coefficient to multiply with `minTransitTime`.

Use a value between `0.0` and `3.0`. Using `0.0` will eliminate the `minTransitTime` from the dynamic raptor-search-window calculation.

<h3 id="transit_dynamicSearchWindow_minWaitTimeCoefficient">minWaitTimeCoefficient</h3>

**Since version:** `2.1` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.5`   
**Path:** /transit/dynamicSearchWindow 

The coefficient to multiply with `minWaitTime`.

Use a value between `0.0` and `1.0`. Using `0.0` will eliminate the `minWaitTime` from the dynamic raptor-search-window calculation.

<h3 id="transit_dynamicSearchWindow_minWindow">minWindow</h3>

**Since version:** `2.2` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT40M"`   
**Path:** /transit/dynamicSearchWindow 

The constant minimum duration for a raptor-search-window. 

Use a value between 20 and 180 minutes in a normal deployment.

<h3 id="transit_dynamicSearchWindow_stepMinutes">stepMinutes</h3>

**Since version:** `2.1` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `10`   
**Path:** /transit/dynamicSearchWindow 

Used to set the steps the search-window is rounded to.

The search window is rounded off to the closest multiplication of `stepMinutes`. If `stepMinutes` =
10 minutes, the search-window can be 10, 20, 30 ... minutes. It the computed search-window is 5
minutes and 17 seconds it will be rounded up to 10 minutes.


Use a value between `1` and `60`. This should be less than the `min-raptor-search-window`
coefficient.


<h3 id="transit_pagingSearchWindowAdjustments">pagingSearchWindowAdjustments</h3>

**Since version:** `na` ∙ **Type:** `duration[]` ∙ **Cardinality:** `Optional`   
**Path:** /transit 

The provided array of durations is used to increase the search-window for the next/previous page.

The search window is expanded when the current page return few options. If ZERO result is returned
the first duration in the list is used, if ONE result is returned then the second duration is used
and so on. The duration is added to the existing search-window and inserted into the next and
previous page cursor. See JavaDoc for [TransitTuningParameters#pagingSearchWindowAdjustments](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/src/main/java/org/opentripplanner/routing/algorithm/raptor/transit/TransitTuningParameters.java)" +
for more info."


<h3 id="transit_stopTransferCost">stopTransferCost</h3>

**Since version:** `2.0` ∙ **Type:** `enum map of integer` ∙ **Cardinality:** `Optional`   
**Path:** /transit   
**Enum keys:** `discouraged` | `allowed` | `recommended` | `preferred`

Use this to set a stop transfer cost for the given transfer priority

The cost is applied to boarding and alighting at all stops. All stops have a transfer cost priority
set, the default is `allowed`. The `stopTransferCost` parameter is optional, but if listed all
values must be set.

If not set the `stopTransferCost` is ignored. This is only available for NeTEx imported Stops.

The cost is a scalar, but is equivalent to the felt cost of riding a transit trip for 1 second.

| Config key    | Description                                                                                   | Type |
|---------------|-----------------------------------------------------------------------------------------------|:----:|
| `discouraged` | Use a very high cost like `72 000` to eliminate transfers at the stop if not the only option. | int  |
| `allowed`     | Allowed, but not recommended. Use something like `150`.                                       | int  |
| `recommended` | Use a small cost penalty like `60`.                                                           | int  |
| `preferred`   | The best place to do transfers. Should be set to `0`(zero).                                   | int  |

Use values in a range from `0` to `100 000`. **All key/value pairs are required if the
`stopTransferCost` is listed.**


<h3 id="transit_transferCacheRequests">transferCacheRequests</h3>

**Since version:** `2.3` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** /transit ∙ **See:** [RouteRequest.md](RouteRequest.md) 

Routing requests to use for pre-filling the stop-to-stop transfer cache.

If not set, the default behavior is to cache stop-to-stop transfers using the default route request
(`routingDefaults`). Use this to change the default or specify more than one `RouteRequest`.

**Example**

```JSON
// router-config.json
{
  "transit": {
    "transferCacheRequests": [
      { "modes": "WALK"                                                     },
      { "modes": "WALK",    "wheelchairAccessibility": { "enabled": true  } }
    ]
  }
}
```


<h3 id="transmodelApi_hideFeedId">hideFeedId</h3>

**Since version:** `na` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /transmodelApi 

Hide the FeedId in all API output, and add it to input.

Only turn this feature on if you have unique ids across all feeds, without the feedId prefix.

<h3 id="transmodelApi_tracingHeaderTags">tracingHeaderTags</h3>

**Since version:** `na` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /transmodelApi 

Used to group requests when monitoring OTP.

<h3 id="vehicleRentalServiceDirectory_headers">headers</h3>

**Since version:** `na` ∙ **Type:** `map of string` ∙ **Cardinality:** `Optional`   
**Path:** /vehicleRentalServiceDirectory 

Http headers.


<!-- PARAMETERS-DETAILS END -->


## Router Config Example

<!-- JSON-EXAMPLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

```JSON
// router-config.json
{
  "configVersion" : "v2.2.0-EN000121",
  "streetRoutingTimeout" : "5s",
  "routingDefaults" : {
    "walkSpeed" : 1.3,
    "bikeSpeed" : 5,
    "carSpeed" : 40,
    "numItineraries" : 12,
    "transferPenalty" : 0,
    "walkReluctance" : 4.0,
    "bikeReluctance" : 5.0,
    "bikeWalkingReluctance" : 10.0,
    "bikeStairsReluctance" : 150.0,
    "carReluctance" : 10.0,
    "stairsReluctance" : 1.65,
    "turnReluctance" : 1.0,
    "elevatorBoardTime" : 90,
    "elevatorBoardCost" : 90,
    "elevatorHopTime" : 20,
    "elevatorHopCost" : 20,
    "bikeRentalPickupCost" : 120,
    "bikeRentalDropoffTime" : 30,
    "bikeRentalDropoffCost" : 30,
    "bikeParkTime" : 60,
    "bikeParkCost" : 120,
    "carDropoffTime" : 120,
    "waitReluctance" : 1.0,
    "walkBoardCost" : 600,
    "bikeBoardCost" : 600,
    "otherThanPreferredRoutesPenalty" : 300,
    "transferSlack" : 120,
    "boardSlackForMode" : {
      "AIRPLANE" : "35m"
    },
    "alightSlackForMode" : {
      "AIRPLANE" : "15m"
    },
    "transitReluctanceForMode" : {
      "RAIL" : 0.85
    },
    "maxAccessEgressDurationForMode" : {
      "BIKE_RENTAL" : "20m"
    },
    "itineraryFilters" : {
      "transitGeneralizedCostLimit" : {
        "costLimitFunction" : "900 + 1.5 x",
        "intervalRelaxFactor" : 0.4
      },
      "bikeRentalDistanceRatio" : 0.3,
      "accessibilityScore" : true,
      "minBikeParkingDistance" : 300
    },
    "carDecelerationSpeed" : 2.9,
    "carAccelerationSpeed" : 2.9,
    "ignoreRealtimeUpdates" : false,
    "geoidElevation" : false,
    "maxJourneyDuration" : "36h",
    "unpreferred" : {
      "agencies" : [
        "HSL:123"
      ],
      "routes" : [
        "HSL:456"
      ]
    },
    "unpreferredCost" : "600 + 2.0 x",
    "transferOptimization" : {
      "optimizeTransferWaitTime" : true,
      "minSafeWaitTimeFactor" : 5.0,
      "backTravelWaitTimeFactor" : 1.0,
      "extraStopBoardAlightCostsFactor" : 8.0
    },
    "wheelchairAccessibility" : {
      "trip" : {
        "onlyConsiderAccessible" : false,
        "unknownCost" : 600,
        "inaccessibleCost" : 3600
      },
      "stop" : {
        "onlyConsiderAccessible" : false,
        "unknownCost" : 600,
        "inaccessibleCost" : 3600
      },
      "elevator" : {
        "onlyConsiderAccessible" : false,
        "unknownCost" : 20,
        "inaccessibleCost" : 3600
      },
      "inaccessibleStreetReluctance" : 25,
      "maxSlope" : 0.083,
      "slopeExceededReluctance" : 1,
      "stairsReluctance" : 100
    }
  },
  "flex" : {
    "maxTransferDuration" : "5m",
    "maxFlexTripDuration" : "45m",
    "maxAccessWalkDuration" : "15m",
    "maxEgressWalkDuration" : "15m"
  },
  "transit" : {
    "maxNumberOfTransfers" : 12,
    "dynamicSearchWindow" : {
      "minTransitTimeCoefficient" : 0.5,
      "minWaitTimeCoefficient" : 0.5,
      "minWindow" : "1h",
      "maxWindow" : "5h"
    },
    "stopTransferCost" : {
      "DISCOURAGED" : 1500,
      "ALLOWED" : 75,
      "RECOMMENDED" : 30,
      "PREFERRED" : 0
    },
    "transferCacheRequests" : [
      {
        "modes" : "WALK"
      },
      {
        "modes" : "WALK",
        "wheelchairAccessibility" : {
          "enabled" : true
        }
      }
    ]
  },
  "vehicleRentalServiceDirectory" : {
    "url" : "https://entur.no/bikeRentalServiceDirectory",
    "sourcesName" : "systems",
    "updaterUrlName" : "url",
    "updaterNetworkName" : "id",
    "headers" : {
      "ET-Client-Name" : "MY_ORG_CLIENT_NAME"
    }
  },
  "transmodelApi" : {
    "hideFeedId" : true
  },
  "vectorTileLayers" : [
    {
      "name" : "stops",
      "type" : "Stop",
      "mapper" : "Digitransit",
      "maxZoom" : 20,
      "minZoom" : 14,
      "cacheMaxSeconds" : 600
    },
    {
      "name" : "stations",
      "type" : "Station",
      "mapper" : "Digitransit",
      "maxZoom" : 20,
      "minZoom" : 12,
      "cacheMaxSeconds" : 600
    },
    {
      "name" : "rentalPlaces",
      "type" : "VehicleRental",
      "mapper" : "Digitransit",
      "maxZoom" : 20,
      "minZoom" : 14,
      "cacheMaxSeconds" : 60,
      "expansionFactor" : 0.25
    },
    {
      "name" : "rentalVehicle",
      "type" : "VehicleRentalVehicle",
      "mapper" : "Digitransit",
      "maxZoom" : 20,
      "minZoom" : 14,
      "cacheMaxSeconds" : 60
    },
    {
      "name" : "rentalStation",
      "type" : "VehicleRentalStation",
      "mapper" : "Digitransit",
      "maxZoom" : 20,
      "minZoom" : 14,
      "cacheMaxSeconds" : 600
    },
    {
      "name" : "vehicleParking",
      "type" : "VehicleParking",
      "mapper" : "Digitransit",
      "maxZoom" : 20,
      "minZoom" : 14,
      "cacheMaxSeconds" : 60,
      "expansionFactor" : 0.25
    }
  ],
  "updaters" : [
    {
      "type" : "real-time-alerts",
      "frequencySec" : 30,
      "url" : "http://developer.trimet.org/ws/V1/FeedSpecAlerts/appID/0123456789ABCDEF",
      "feedId" : "TriMet",
      "headers" : {
        "Some-Header" : "A-Value"
      }
    },
    {
      "type" : "vehicle-rental",
      "network" : "socialbicycles_coast",
      "sourceType" : "gbfs",
      "language" : "en",
      "frequencySec" : 60,
      "allowKeepingRentedBicycleAtDestination" : false,
      "geofencingZones" : false,
      "url" : "http://coast.socialbicycles.com/opendata/gbfs.json",
      "headers" : {
        "Auth" : "<any-token>",
        "<key>" : "<value>"
      }
    },
    {
      "type" : "vehicle-parking",
      "sourceType" : "hsl-park",
      "feedId" : "hslpark",
      "timeZone" : "Europe/Helsinki",
      "facilitiesFrequencySec" : 3600,
      "facilitiesUrl" : "https://p.hsl.fi/api/v1/facilities.json?limit=-1",
      "utilizationsFrequencySec" : 600,
      "utilizationsUrl" : "https://p.hsl.fi/api/v1/utilizations.json?limit=-1",
      "hubsUrl" : "https://p.hsl.fi/api/v1/hubs.json?limit=-1"
    },
    {
      "type" : "vehicle-parking",
      "sourceType" : "park-api",
      "feedId" : "parkapi",
      "timeZone" : "Europe/Berlin",
      "frequencySec" : 600,
      "url" : "https://foo.bar",
      "headers" : {
        "Cache-Control" : "max-age=604800"
      },
      "tags" : [
        "source:parkapi"
      ]
    },
    {
      "type" : "vehicle-parking",
      "feedId" : "bikely",
      "sourceType" : "bikely",
      "url" : "https://api.safebikely.com/api/v1/s/locations",
      "headers" : {
        "X-Bikely-Token" : "${BIKELY_TOKEN}",
        "Authorization" : "${BIKELY_AUTHORIZATION}"
      }
    },
    {
      "type" : "stop-time-updater",
      "frequencySec" : 60,
      "backwardsDelayPropagationType" : "REQUIRED_NO_DATA",
      "url" : "http://developer.trimet.org/ws/V1/TripUpdate/appID/0123456789ABCDEF",
      "feedId" : "TriMet",
      "headers" : {
        "Authorization" : "A-Token"
      }
    },
    {
      "type" : "vehicle-positions",
      "url" : "https://s3.amazonaws.com/kcm-alerts-realtime-prod/vehiclepositions.pb",
      "feedId" : "1",
      "frequencySec" : 60,
      "headers" : {
        "Header-Name" : "Header-Value"
      }
    },
    {
      "type" : "websocket-gtfs-rt-updater"
    },
    {
      "type" : "siri-et-updater",
      "url" : "https://example.com/some/path",
      "feedId" : "feed_id",
      "timeoutSec" : 30,
      "headers" : {
        "Authorization" : "Some-Token"
      }
    },
    {
      "type" : "siri-azure-sx-updater",
      "topic" : "some_topic",
      "servicebus-url" : "service_bus_url",
      "feedId" : "feed_id",
      "customMidnight" : 4,
      "history" : {
        "url" : "endpoint_url",
        "fromDateTime" : "-P1D",
        "toDateTime" : "P1D",
        "timeout" : 300000
      }
    }
  ],
  "rideHailingServices" : [
    {
      "type" : "uber-car-hailing",
      "clientId" : "secret-id",
      "clientSecret" : "very-secret",
      "wheelchairAccessibleRideType" : "car"
    }
  ]
}
```

<!-- JSON-EXAMPLE END -->
