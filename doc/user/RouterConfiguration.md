<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc/templates
   - Generated directory is: /doc/user 
-->

# Router configuration

This section covers all options that can be set for each router using the `router-config.json` file.
These options can be applied by the OTP server without rebuilding the graph.

## Configure using command-line arguments

Certain settings can be provided on the command line, when starting OpenTripPlanner. See
the `CommandLineParameters` class
for [a full list of arguments](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/application/src/main/java/org/opentripplanner/standalone/config/CommandLineParameters.java)
.

## Routing defaults

There are many trip planning options used in the OTP web API, and more exist internally that are not
exposed via the API. You may want to change the default value for some of these parameters, i.e. the
value which will be applied unless it is overridden in a web API request.

A full list of them can be found in the [RouteRequest](RouteRequest.md).


## Parameter Summary

<!-- PARAMETERS-TABLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                                                          |          Type         | Summary                                                                                                                                                                                                              |  Req./Opt. | Default Value | Since |
|-------------------------------------------------------------------------------------------|:---------------------:|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| [configVersion](#configVersion)                                                           |        `string`       | Deployment version of the *router-config.json*.                                                                                                                                                                      | *Optional* |               |  2.1  |
| [flex](sandbox/Flex.md)                                                                   |        `object`       | Configuration for flex routing.                                                                                                                                                                                      | *Optional* |               |  2.1  |
| gtfsApi                                                                                   |        `object`       | Configuration for the GTFS GraphQL API.                                                                                                                                                                              | *Optional* |               |  2.8  |
|    [tracingTags](#gtfsApi_tracingTags)                                                    |       `string[]`      | Used to group requests based on headers or query parameters when monitoring OTP.                                                                                                                                     | *Optional* |               |   na  |
| [rideHailingServices](sandbox/RideHailing.md)                                             |       `object[]`      | Configuration for interfaces to external ride hailing services like Uber.                                                                                                                                            | *Optional* |               |  2.3  |
| [routingDefaults](RouteRequest.md)                                                        |        `object`       | The default parameters for the routing query.                                                                                                                                                                        | *Optional* |               |  2.0  |
| [server](#server)                                                                         |        `object`       | Configuration for router server.                                                                                                                                                                                     | *Optional* |               |  2.4  |
|    [apiDocumentationProfile](#server_apiDocumentationProfile)                             |         `enum`        | List of available custom documentation profiles. A profile is used to inject custom documentation like type and field description or a deprecated reason.  Currently, ONLY the Transmodel API supports this feature. | *Optional* | `"default"`   |  2.7  |
|    [apiProcessingTimeout](#server_apiProcessingTimeout)                                   |       `duration`      | Maximum processing time for an API request                                                                                                                                                                           | *Optional* | `"PT-1S"`     |  2.4  |
|    [traceParameters](#server_traceParameters)                                             |       `object[]`      | Trace OTP request using HTTP request/response parameter(s) combined with logging.                                                                                                                                    | *Optional* |               |  2.4  |
|          generateIdIfMissing                                                              |       `boolean`       | If `true` a unique value is generated if no http request header is provided, or the value is missing.                                                                                                                | *Optional* | `false`       |  2.4  |
|          httpRequestHeader                                                                |        `string`       | The header-key to use when fetching the trace parameter value                                                                                                                                                        | *Optional* |               |  2.4  |
|          httpResponseHeader                                                               |        `string`       | The header-key to use when saving the value back into the http response                                                                                                                                              | *Optional* |               |  2.4  |
|          [logKey](#server_traceParameters_0_logKey)                                       |        `string`       | The log event key used.                                                                                                                                                                                              | *Optional* |               |  2.4  |
| timetableUpdates                                                                          |        `object`       | Global configuration for timetable updaters.                                                                                                                                                                         | *Optional* |               |  2.2  |
|    [maxSnapshotFrequency](#timetableUpdates_maxSnapshotFrequency)                         |       `duration`      | How long a snapshot should be cached.                                                                                                                                                                                | *Optional* | `"PT1S"`      |  2.2  |
|    purgeExpiredData                                                                       |       `boolean`       | Should expired real-time data be purged from the graph. Apply to GTFS-RT and Siri updates.                                                                                                                           | *Optional* | `true`        |  2.2  |
| [transit](#transit)                                                                       |        `object`       | Configuration for transit searches with RAPTOR.                                                                                                                                                                      | *Optional* |               |   na  |
|    [iterationDepartureStepInSeconds](#transit_iterationDepartureStepInSeconds)            |       `integer`       | Step for departure times between each RangeRaptor iterations.                                                                                                                                                        | *Optional* | `60`          |   na  |
|    [maxNumberOfTransfers](#transit_maxNumberOfTransfers)                                  |       `integer`       | This parameter is used to allocate enough memory space for Raptor.                                                                                                                                                   | *Optional* | `12`          |   na  |
|    [maxSearchWindow](#transit_maxSearchWindow)                                            |       `duration`      | Upper limit of the request parameter searchWindow.                                                                                                                                                                   | *Optional* | `"PT24H"`     |  2.4  |
|    [scheduledTripBinarySearchThreshold](#transit_scheduledTripBinarySearchThreshold)      |       `integer`       | This threshold is used to determine when to perform a binary trip schedule search.                                                                                                                                   | *Optional* | `50`          |   na  |
|    [searchThreadPoolSize](#transit_searchThreadPoolSize)                                  |       `integer`       | Split a travel search in smaller jobs and run them in parallel to improve performance.                                                                                                                               | *Optional* | `0`           |   na  |
|    [transferCacheMaxSize](#transit_transferCacheMaxSize)                                  |       `integer`       | The maximum number of distinct transfers parameters to cache pre-calculated transfers for.                                                                                                                           | *Optional* | `25`          |   na  |
|    [dynamicSearchWindow](#transit_dynamicSearchWindow)                                    |        `object`       | The dynamic search window coefficients used to calculate the EDT, LAT and SW.                                                                                                                                        | *Optional* |               |  2.1  |
|       [maxWindow](#transit_dynamicSearchWindow_maxWindow)                                 |       `duration`      | Upper limit for the search-window calculation.                                                                                                                                                                       | *Optional* | `"PT3H"`      |  2.2  |
|       [minTransitTimeCoefficient](#transit_dynamicSearchWindow_minTransitTimeCoefficient) |        `double`       | The coefficient to multiply with `minTransitTime`.                                                                                                                                                                   | *Optional* | `0.5`         |  2.1  |
|       [minWaitTimeCoefficient](#transit_dynamicSearchWindow_minWaitTimeCoefficient)       |        `double`       | The coefficient to multiply with `minWaitTime`.                                                                                                                                                                      | *Optional* | `0.5`         |  2.1  |
|       [minWindow](#transit_dynamicSearchWindow_minWindow)                                 |       `duration`      | The constant minimum duration for a raptor-search-window.                                                                                                                                                            | *Optional* | `"PT40M"`     |  2.2  |
|       [stepMinutes](#transit_dynamicSearchWindow_stepMinutes)                             |       `integer`       | Used to set the steps the search-window is rounded to.                                                                                                                                                               | *Optional* | `10`          |  2.1  |
|    [pagingSearchWindowAdjustments](#transit_pagingSearchWindowAdjustments)                |      `duration[]`     | The provided array of durations is used to increase the search-window for the next/previous page.                                                                                                                    | *Optional* |               |   na  |
|    [stopBoardAlightDuringTransferCost](#transit_stopBoardAlightDuringTransferCost)        | `enum map of integer` | Costs for boarding and alighting during transfers at stops with a given transfer priority.                                                                                                                           | *Optional* |               |  2.0  |
|    [transferCacheRequests](#transit_transferCacheRequests)                                |       `object[]`      | Routing requests to use for pre-filling the stop-to-stop transfer cache.                                                                                                                                             | *Optional* |               |  2.3  |
| transmodelApi                                                                             |        `object`       | Configuration for the Transmodel GraphQL API.                                                                                                                                                                        | *Optional* |               |  2.1  |
|    [hideFeedId](#transmodelApi_hideFeedId)                                                |       `boolean`       | Hide the FeedId in all API output, and add it to input.                                                                                                                                                              | *Optional* | `false`       |   na  |
|    [maxNumberOfResultFields](#transmodelApi_maxNumberOfResultFields)                      |       `integer`       | The maximum number of fields in a GraphQL result                                                                                                                                                                     | *Optional* | `1000000`     |  2.6  |
|    [tracingHeaderTags](#transmodelApi_tracingHeaderTags)                                  |       `string[]`      | Used to group requests when monitoring OTP.                                                                                                                                                                          | *Optional* |               |   na  |
| [triasApi](sandbox/TriasApi.md)                                                           |        `object`       | Configuration for the TRIAS API.                                                                                                                                                                                     | *Optional* |               |  2.8  |
| [updaters](Realtime-Updaters.md)                                                          |       `object[]`      | Configuration for the updaters that import various types of data into OTP.                                                                                                                                           | *Optional* |               |  1.5  |
| [vectorTiles](sandbox/MapboxVectorTilesApi.md)                                            |        `object`       | Vector tile configuration                                                                                                                                                                                            | *Optional* |               |   na  |
| [vehicleRentalServiceDirectory](sandbox/VehicleRentalServiceDirectory.md)                 |        `object`       | Configuration for the vehicle rental service directory.                                                                                                                                                              | *Optional* |               |  2.0  |

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


<h3 id="gtfsApi_tracingTags">tracingTags</h3>

**Since version:** `na` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /gtfsApi 

Used to group requests based on headers or query parameters when monitoring OTP.

<h3 id="server">server</h3>

**Since version:** `2.4` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** / 

Configuration for router server.

These parameters are used to configure the router server. Many parameters are specific to a
domain, these are set in the routing request.


<h3 id="server_apiDocumentationProfile">apiDocumentationProfile</h3>

**Since version:** `2.7` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"default"`   
**Path:** /server   
**Enum values:** `default` | `entur`

List of available custom documentation profiles. A profile is used to inject custom
documentation like type and field description or a deprecated reason.

Currently, ONLY the Transmodel API supports this feature.


 - `default` Default documentation is used.
 - `entur` Entur specific documentation. This deprecate features not supported at Entur, Norway.


<h3 id="server_apiProcessingTimeout">apiProcessingTimeout</h3>

**Since version:** `2.4` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT-1S"`   
**Path:** /server 

Maximum processing time for an API request

This timeout limits the server-side processing time for a given API request. This does not include
network latency nor waiting time in the HTTP server thread pool. The default value is
`-1s`(no timeout). The timeout is applied to all APIs (Transmodel & GTFS GraphQL).
The timeout is not enforced when the parallel routing OTP feature is in use.


<h3 id="server_traceParameters">traceParameters</h3>

**Since version:** `2.4` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** /server 

Trace OTP request using HTTP request/response parameter(s) combined with logging.

OTP supports tracing user requests across log events and "outside" services. OTP can insert
http-request-header parameters into all associated log events and into the http response. If the
value is not present in the request, a unique value can be generated. The OTP generated value is
a 6 characters long base 36[0-9a-z] character string.

**Use-case Correlation-ID**

A common use-case in a service oriented environment is to use a _correlation-id_ to identify all log
messages across multiple (micro-)services from the same user. This is done by setting the
"X-Correlation-ID" http header in the http facade/gateway. Use the "traceParameters" to configure
OTP to pick up the correlation id, insert it into the logs and return it. See the example below
on how-to configure the "server.traceParameters" instance.


<h3 id="server_traceParameters_0_logKey">logKey</h3>

**Since version:** `2.4` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /server/traceParameters/[0] 

The log event key used.

OTP stores the key/value pair in the log MDC (Mapped Diagnostic Context). To use it
you normally include the key in the log pattern like this: `%X{LOG-KEY}`. See your
log framework for details. Only log4j and logback support this.


<h3 id="timetableUpdates_maxSnapshotFrequency">maxSnapshotFrequency</h3>

**Since version:** `2.2` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT1S"`   
**Path:** /timetableUpdates 

How long a snapshot should be cached.

If a timetable snapshot is requested less than this number of milliseconds after the previous snapshot, then return the same instance. Throttles the potentially resource-consuming task of duplicating a TripPattern → Timetable map and indexing the new Timetables. Applies to GTFS-RT and Siri updates.

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


<h3 id="transit_maxSearchWindow">maxSearchWindow</h3>

**Since version:** `2.4` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT24H"`   
**Path:** /transit 

Upper limit of the request parameter searchWindow.

Maximum search window that can be set through the searchWindow API parameter.
Due to the way timetable data are collected before a Raptor trip search,
using a search window larger than 24 hours may lead to inconsistent search results.
Limiting the search window prevents also potential performance issues.
The recommended maximum value is 24 hours.
This parameter does not restrict the maximum duration of a dynamic search window (use
the parameter `transit.dynamicSearchWindow.maxWindow` to specify such a restriction).


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
Multiple searches can run in parallel - this parameter has no effect with regard to that. If 0,
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

The dynamic search window coefficients is used to calculate EDT (*earliest-departure-time*),
LAT (*latest-arrival-time*) and SW (*raptor-search-window*) request parameters using heuristics. The
heuristics perform a Raptor search (one-iteration) to find a trip which we use to find a lower
bound for the travel duration time - the "minTransitTime". The heuristic search is used for other
purposes too, and is very fast.

At least the EDT or the LAT must be passed into Raptor to perform a Range Raptor search. If
unknown/missing the parameters (EDT, LAT, DW) are dynamically calculated. The dynamic coefficients
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

Long search windows consume a lot of resources and may take a long time. Use this parameter to
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


<h3 id="transit_stopBoardAlightDuringTransferCost">stopBoardAlightDuringTransferCost</h3>

**Since version:** `2.0` ∙ **Type:** `enum map of integer` ∙ **Cardinality:** `Optional`   
**Path:** /transit   
**Enum keys:** `preferred` | `recommended` | `allowed` | `discouraged`

Costs for boarding and alighting during transfers at stops with a given transfer priority.

This cost is applied **both to boarding and alighting** at stops during transfers. All stops have a
transfer cost priority set, the default is `allowed`. The `stopBoardAlightDuringTransferCost`
parameter is optional, but if listed all values must be set.

When a transfer occurs at the same stop, the cost will be applied twice since the cost is both for
boarding and alighting,

If not set the `stopBoardAlightDuringTransferCost` is ignored. This is only available for NeTEx
imported Stops.

The cost is a scalar, but is equivalent to the felt cost of riding a transit trip for 1 second.

| Config key    | Description                                                                                   | Type |
|---------------|-----------------------------------------------------------------------------------------------|:----:|
| `discouraged` | Use a very high cost like `72 000` to eliminate transfers at the stop if not the only option. | int  |
| `allowed`     | Allowed, but not recommended. Use something like `150`.                                       | int  |
| `recommended` | Use a small cost penalty like `60`.                                                           | int  |
| `preferred`   | The best place to do transfers. Should be set to `0`(zero).                                   | int  |

Use values in a range from `0` to `100 000`. **All key/value pairs are required if the
`stopBoardAlightDuringTransferCost` is listed.**


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

<h3 id="transmodelApi_maxNumberOfResultFields">maxNumberOfResultFields</h3>

**Since version:** `2.6` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `1000000`   
**Path:** /transmodelApi 

The maximum number of fields in a GraphQL result

Enforce rate limiting based on query complexity; Queries that return too much data are cancelled.

<h3 id="transmodelApi_tracingHeaderTags">tracingHeaderTags</h3>

**Since version:** `na` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /transmodelApi 

Used to group requests when monitoring OTP.


<!-- PARAMETERS-DETAILS END -->


## Router Config Example

<!-- JSON-EXAMPLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

```JSON
// router-config.json
{
  "configVersion" : "v2.4.0-EN000121",
  "server" : {
    "apiProcessingTimeout" : "7s",
    "traceParameters" : [
      {
        "httpRequestHeader" : "X-Correlation-ID",
        "httpResponseHeader" : "X-Correlation-ID",
        "logKey" : "correlationId",
        "generateIdIfMissing" : true
      }
    ]
  },
  "routingDefaults" : {
    "numItineraries" : 12,
    "transferPenalty" : 0,
    "turnReluctance" : 1.0,
    "elevatorBoardTime" : 90,
    "elevatorBoardCost" : 90,
    "elevatorHopTime" : 20,
    "elevatorHopCost" : 20,
    "bicycle" : {
      "speed" : 5,
      "reluctance" : 5.0,
      "boardCost" : 600,
      "walk" : {
        "reluctance" : 10.0,
        "stairsReluctance" : 150.0
      },
      "rental" : {
        "pickupCost" : 120,
        "dropOffTime" : "30s",
        "dropOffCost" : 30
      },
      "parking" : {
        "time" : "1m",
        "cost" : 120
      },
      "triangle" : {
        "safety" : 0.4,
        "flatness" : 0.3,
        "time" : 0.3
      }
    },
    "car" : {
      "reluctance" : 10,
      "boardCost" : 600,
      "decelerationSpeed" : 2.9,
      "accelerationSpeed" : 2.9,
      "rental" : {
        "pickupCost" : 120,
        "dropOffTime" : "30s",
        "dropOffCost" : 30
      },
      "parking" : {
        "time" : "5m",
        "cost" : 600
      }
    },
    "scooter" : {
      "speed" : 5,
      "reluctance" : 5.0,
      "rental" : {
        "pickupCost" : 120,
        "dropOffTime" : "30s",
        "dropOffCost" : 30
      },
      "triangle" : {
        "safety" : 0.4,
        "flatness" : 0.3,
        "time" : 0.3
      }
    },
    "walk" : {
      "speed" : 1.3,
      "reluctance" : 4.0,
      "stairsReluctance" : 1.65,
      "boardCost" : 600,
      "escalator" : {
        "reluctance" : 1.5,
        "speed" : 0.45
      }
    },
    "waitReluctance" : 1.0,
    "otherThanPreferredRoutesPenalty" : 300,
    "transferSlack" : "2m",
    "boardSlackForMode" : {
      "AIRPLANE" : "35m"
    },
    "alightSlackForMode" : {
      "AIRPLANE" : "15m"
    },
    "transitReluctanceForMode" : {
      "RAIL" : 0.85
    },
    "accessEgress" : {
      "maxDuration" : "45m",
      "maxDurationForMode" : {
        "BIKE_RENTAL" : "20m"
      },
      "maxStopCount" : 500,
      "maxStopCountForMode" : {
        "CAR" : 0
      },
      "penalty" : {
        "FLEXIBLE" : {
          "timePenalty" : "2m + 1.1t",
          "costFactor" : 1.7
        }
      }
    },
    "itineraryFilters" : {
      "transitGeneralizedCostLimit" : {
        "costLimitFunction" : "15m + 1.5 x",
        "intervalRelaxFactor" : 0.4
      },
      "nonTransitGeneralizedCostLimit" : "400 + 1.5x",
      "removeTransitWithHigherCostThanBestOnStreetOnly" : "60 + 1.3x",
      "bikeRentalDistanceRatio" : 0.3,
      "accessibilityScore" : true,
      "minBikeParkingDistance" : 300,
      "debug" : "limit-to-search-window"
    },
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
    "unpreferredCost" : "10m + 2.0 x",
    "streetRoutingTimeout" : "5s",
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
    "stopBoardAlightDuringTransferCost" : {
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
  "gtfsApi" : {
    "tracingTags" : [
      "example-header-name",
      "example-query-parameter-name"
    ]
  },
  "vectorTiles" : {
    "basePath" : "/otp_ct/vectorTiles",
    "layers" : [
      {
        "name" : "stops",
        "type" : "Stop",
        "mapper" : "Digitransit",
        "maxZoom" : 20,
        "minZoom" : 14,
        "cacheMaxSeconds" : 600
      },
      {
        "name" : "areaStops",
        "type" : "AreaStop",
        "mapper" : "OTPRR",
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
    ]
  },
  "timetableUpdates" : {
    "purgeExpiredData" : false,
    "maxSnapshotFrequency" : "2s"
  },
  "updaters" : [
    {
      "type" : "real-time-alerts",
      "frequency" : "30s",
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
      "frequency" : "1m",
      "allowKeepingRentedVehicleAtDestination" : false,
      "geofencingZones" : false,
      "url" : "http://coast.socialbicycles.com/opendata/gbfs.json",
      "headers" : {
        "Auth" : "<any-token>",
        "<key>" : "<value>"
      }
    },
    {
      "type" : "vehicle-parking",
      "sourceType" : "liipi",
      "feedId" : "liipi",
      "timeZone" : "Europe/Helsinki",
      "facilitiesFrequencySec" : 3600,
      "facilitiesUrl" : "https://parking.fintraffic.fi/api/v1/facilities.json?limit=-1",
      "utilizationsFrequencySec" : 600,
      "utilizationsUrl" : "https://parking.fintraffic.fi/api/v1/utilizations.json?limit=-1",
      "hubsUrl" : "https://parking.fintraffic.fi/api/v1/hubs.json?limit=-1"
    },
    {
      "type" : "vehicle-parking",
      "sourceType" : "park-api",
      "feedId" : "parkapi",
      "timeZone" : "Europe/Berlin",
      "frequency" : "10m",
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
      "frequency" : "1m",
      "backwardsDelayPropagationType" : "REQUIRED_NO_DATA",
      "url" : "http://developer.trimet.org/ws/V1/TripUpdate/appID/0123456789ABCDEF",
      "feedId" : "TriMet",
      "headers" : {
        "Authorization" : "A-Token"
      }
    },
    {
      "type" : "mqtt-gtfs-rt-updater",
      "url" : "tcp://pred.rt.hsl.fi",
      "topic" : "gtfsrt/v2/fi/hsl/tu",
      "feedId" : "HSL",
      "fuzzyTripMatching" : true
    },
    {
      "type" : "vehicle-positions",
      "url" : "https://s3.amazonaws.com/kcm-alerts-realtime-prod/vehiclepositions.pb",
      "feedId" : "1",
      "frequency" : "1m",
      "headers" : {
        "Header-Name" : "Header-Value"
      },
      "fuzzyTripMatching" : false,
      "features" : [
        "position"
      ]
    },
    {
      "type" : "siri-et-updater",
      "url" : "https://example.com/some/path",
      "feedId" : "feed_id",
      "timeout" : "30s",
      "headers" : {
        "Authorization" : "Some-Token"
      }
    },
    {
      "type" : "siri-sx-updater",
      "url" : "https://example.com/some/path",
      "feedId" : "feed_id",
      "timeout" : "30s",
      "headers" : {
        "Key" : "Value"
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
    },
    {
      "type" : "siri-azure-et-updater",
      "topic" : "some_topic",
      "authenticationType" : "SharedAccessKey",
      "fullyQualifiedNamespace" : "fully_qualified_namespace",
      "servicebus-url" : "service_bus_url",
      "feedId" : "feed_id",
      "customMidnight" : 4,
      "history" : {
        "url" : "endpoint_url",
        "fromDateTime" : "-P1D",
        "timeout" : 300000
      }
    },
    {
      "type" : "siri-et-google-pubsub-updater",
      "feedId" : "feed_id",
      "reconnectPeriod" : "5s",
      "initialGetDataTimeout" : "1m20s",
      "topicProjectName" : "google_pubsub_topic_project_name",
      "subscriptionProjectName" : "google_pubsub_subscription_project_name",
      "topicName" : "estimated_timetables",
      "dataInitializationUrl" : "https://example.com/some/path",
      "fuzzyTripMatching" : true
    },
    {
      "type" : "vehicle-parking",
      "feedId" : "bikeep",
      "sourceType" : "bikeep",
      "url" : "https://services.bikeep.com/location/v1/public-areas/no-baia-mobility/locations"
    },
    {
      "type" : "vehicle-parking",
      "feedId" : "parking",
      "sourceType" : "siri-fm",
      "url" : "https://transmodel.api.opendatahub.com/siri-lite/fm/parking"
    },
    {
      "type" : "siri-et-lite",
      "feedId" : "sta",
      "url" : "https://example.com/siri-lite/estimated-timetable/xml",
      "fuzzyTripMatching" : true
    },
    {
      "type" : "siri-sx-lite",
      "feedId" : "sta",
      "url" : "https://example.com/siri-lite/situation-exchange/xml"
    }
  ],
  "rideHailingServices" : [
    {
      "type" : "uber-car-hailing",
      "clientId" : "secret-id",
      "clientSecret" : "very-secret",
      "wheelchairAccessibleProductId" : "545de0c4-659f-49c6-be65-0d5e448dffd5",
      "bannedProductIds" : [
        "1196d0dd-423b-4a81-a1d8-615367d3a365",
        "f58761e5-8dd5-4940-a472-872f1236c596"
      ]
    }
  ]
}
```

<!-- JSON-EXAMPLE END -->
