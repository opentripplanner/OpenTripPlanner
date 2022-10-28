<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc-templates
   - Generated directory is: /docs 
-->

# Router configuration

This section covers all options that can be set for each router using the `router-config.json` file.
These options can be applied by the OTP server without rebuilding the graph.


<!-- INSERT: PARAMETERS-TABLE -->


## Routing defaults

There are many trip planning options used in the OTP web API, and more exist internally that are not
exposed via the API. You may want to change the default value for some of these parameters, i.e. the
value which will be applied unless it is overridden in a web API request.

A full list of them can be found in the [RoutingRequest](/docs/RouteRequest.md).



## Tuning transit routing

Nested inside `transit {...}` in `router-config.json`.

Some of these parameters for tuning transit routing is only available through configuration and
cannot be set in the routing request. These parameters work together with the default routing
request and the actual routing request.

| config key                           | description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | value type | value default                             |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|-------------------------------------------|
| `maxNumberOfTransfers`               | Use this parameter to allocate enough space for Raptor. Set it to the maximum number of transfers for any given itinerary expected to be found within the entire transit network. The memory overhead of setting this higher than the maximum number of transfers is very little so it is better to set it too high then to low.                                                                                                                                                                                                                                                                                                                      | int        | `12`                                      |
| `scheduledTripBinarySearchThreshold` | The threshold is used to determine when to perform a binary trip schedule search to reduce the number of trips departure time lookups and comparisons. When testing with data from Entur and all of Norway as a Graph, the optimal value was around 50. Changing this may improve the performance with just a few percent.                                                                                                                                                                                                                                                                                                                            | int        | `50`                                      |
| `iterationDepartureStepInSeconds`    | Step for departure times between each RangeRaptor iterations. A transit network usually uses minute resolution for its depature and arrival times. To match that, set this variable to 60 seconds.                                                                                                                                                                                                                                                                                                                                                                                                                                                    | int        | `60`                                      |
| `searchThreadPoolSize`               | Split a travel search in smaller jobs and run them in parallel to improve performance. Use this parameter to set the total number of executable threads available across all searches. Multiple searches can run in parallel - this parameter have no effect with regard to that. If 0, no extra threads are started and the search is done in one thread.                                                                                                                                                                                                                                                                                            | int        | `0`                                       |
| `dynamicSearchWindow`                | The dynamic search window coefficients used to calculate the EDT(earliest-departure-time), LAT(latest-arrival-time) and SW(raptor-search-window) using heuristics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | object     | `null`                                    |
| `stopTransferCost`                   | Use this to set a stop transfer cost for the given [TransferPriority](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/src/main/java/org/opentripplanner/model/TransferPriority.java). The cost is applied to boarding and alighting at all stops. All stops have a transfer cost priority set, the default is `ALLOWED`. The `stopTransferCost` parameter is optional, but if listed all values must be set.                                                                                                                                                                                                                          | enum map   | `null`                                    |
| `transferCacheMaxSize`               | The maximum number of distinct transfers parameters (`RoutingRequest`s) to cache pre-calculated transfers for. If too low, requests may be slower. If too high, more memory may be used then required.                                                                                                                                                                                                                                                                                                                                                                                                                                                | int        | `25`                                      |
| `pagingSearchWindowAdjustments`      | The provided array of durations is used to increase the search-window for the next/previous page when the current page return few options. If ZERO results is returned the first duration in the list is used, if ONE result is returned then the second duration is used and so on. The duration is added to the existing search-window and inserted into the next and previous page cursor. See JavaDoc for [TransitTuningParameters#pagingSearchWindowAdjustments](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/src/main/java/org/opentripplanner/routing/algorithm/raptor/transit/TransitTuningParameters.java) for more info. | duration[] | `["4h", "2h", "1h", "30m", "20m", "10m"]` |

### Tuning transit routing - Dynamic search window

Nested inside `transit : { dynamicSearchWindow : { ... } }` in `router-config.json`.

| config key                  | description                                                                                                                                                                                                                                                                       | value type | value default   |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|-----------------|
| `minTransitTimeCoefficient` | The coefficient to multiply with minimum transit time found using a heuristic search. This scaled value is added to the `minWinTimeMinutes`. A value between `0.0` to `3.0` is expected to give ok results.                                                                       | double     | `0.5`           |
| `minWaitTimeCoefficient`    | The coefficient to multiply with a minimum wait time estimated based on the heuristic search. This will increase the search-window in low transit frequency areas. This value is added to the `minWinTimeMinutes`. A value between `0.0` to `1.0` is expected to give ok results. | double     | `0.5`           |
| `minWinTimeMinutes`         | The constant minimum number of minutes for a raptor search window. Use a value between 20-180 minutes in a normal deployment.                                                                                                                                                     | int        | `40`            |
| `maxWinTimeMinutes`         | Set an upper limit to the calculation of the dynamic search window to prevent exceptionable cases to cause very long search windows. Long search windows consumes a lot of resources and may take a long time. Use this parameter to tune the desired maximum search time.        | int        | `180` (3 hours) |
| `stepMinutes`               | The search window is rounded of to the closest multiplication of N minutes. If N=10 minutes, the search-window can be 10, 20, 30 ... minutes. If the computed search-window is 5 minutes and 17 seconds, it will be rounded up to 10 minutes.                                      | int        | `10`            |

### Tuning transit routing - Stop transfer cost

Nested inside `transit : { stopTransferCost : { ... } }` in `router-config.json`.

This _cost_ is in addition to other costs like `boardCost` and indirect cost from waiting (
board-/alight-/transfer slack). You should account for this when you tune the routing search
parameters.

If not set the `stopTransferCost` is ignored. This is only available for NeTEx imported Stops.

The cost is a scalar, but is equivalent to the felt cost of riding a transit trip for 1 second.

| config key    | description                                                                                    | value type |
|---------------|------------------------------------------------------------------------------------------------|------------|
| `DISCOURAGED` | Use a very high cost like `72 000` to eliminate transfers ath the stop if not the only option. | int        |
| `ALLOWED`     | Allowed, but not recommended. Use something like `150`.                                        | int        |
| `RECOMMENDED` | Use a small cost penalty like `60`.                                                            | int        |
| `PREFERRED`   | The best place to do transfers. Should be set to `0`(zero).                                    | int        |

Use values in a range from `0` to `100 000`. **All key/value pairs are required if
the `stopTransferCost` is listed.**

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


# Configure using command-line arguments

Certain settings can be provided on the command line, when starting OpenTripPlanner. See
the `CommandLineParameters` class
for [a full list of arguments](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/src/main/java/org/opentripplanner/standalone/config/CommandLineParameters.java)
.


## Parameter Details

<!-- INSERT: PARAMETERS-DETAILS -->
