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

| Config Parameter                                                                                                                           |          Type          | Summary                                                                                                                            |  Req./Opt. | Default Value        | Since |
|--------------------------------------------------------------------------------------------------------------------------------------------|:----------------------:|------------------------------------------------------------------------------------------------------------------------------------|:----------:|----------------------|:-----:|
| [configVersion](#configVersion)                                                                                                            |        `string`        | Deployment version of the *router-config.json*.                                                                                    | *Optional* |                      |  2.1  |
| [requestLogFile](#requestLogFile)                                                                                                          |        `string`        | The path of the log file for the requests.                                                                                         | *Optional* |                      |  2.0  |
| [streetRoutingTimeout](#streetRoutingTimeout)                                                                                              |       `duration`       | The maximum time a street routing request is allowed to take before returning a timeout.                                           | *Optional* | `"PT5S"`             |   na  |
| [flex](sandbox/Flex.md)                                                                                                                    |        `object`        | Configuration for flex routing.                                                                                                    | *Optional* |                      |  2.1  |
| [routingDefaults](RouteRequest.md)                                                                                                         |        `object`        | The default parameters for the routing query.                                                                                      | *Optional* |                      |  2.0  |
| timetableUpdates                                                                                                                           |        `object`        | Global configuration for timetable updaters.                                                                                       | *Optional* |                      |  2.2  |
| [transit](#transit)                                                                                                                        |        `object`        | Configuration for transit searches with RAPTOR.                                                                                    | *Optional* |                      |   na  |
|    [iterationDepartureStepInSeconds](#transit_iterationDepartureStepInSeconds)                                                             |        `integer`       | Step for departure times between each RangeRaptor iterations.                                                                      | *Optional* | `60`                 |   na  |
|    [maxNumberOfTransfers](#transit_maxNumberOfTransfers)                                                                                   |        `integer`       | This parameter is used to allocate enough memory space for Raptor.                                                                 | *Optional* | `12`                 |   na  |
|    [scheduledTripBinarySearchThreshold](#transit_scheduledTripBinarySearchThreshold)                                                       |        `integer`       | This threshold is used to determine when to perform a binary trip schedule search.                                                 | *Optional* | `50`                 |   na  |
|    [searchThreadPoolSize](#transit_searchThreadPoolSize)                                                                                   |        `integer`       | Split a travel search in smaller jobs and run them in parallel to improve performance.                                             | *Optional* | `0`                  |   na  |
|    [transferCacheMaxSize](#transit_transferCacheMaxSize)                                                                                   |        `integer`       | The maximum number of distinct transfers parameters to cache pre-calculated transfers for.                                         | *Optional* | `25`                 |   na  |
|    [dynamicSearchWindow](#transit_dynamicSearchWindow)                                                                                     |        `object`        | The dynamic search window coefficients used to calculate the EDT, LAT and SW.                                                      | *Optional* |                      |  2.1  |
|       [maxWindow](#transit_dynamicSearchWindow_maxWindow)                                                                                  |       `duration`       | Upper limit for the search-window calculation.                                                                                     | *Optional* | `"PT3H"`             |  2.2  |
|       [minTransitTimeCoefficient](#transit_dynamicSearchWindow_minTransitTimeCoefficient)                                                  |        `double`        | The coefficient to multiply with `minTransitTime`.                                                                                 | *Optional* | `0.5`                |  2.1  |
|       [minWaitTimeCoefficient](#transit_dynamicSearchWindow_minWaitTimeCoefficient)                                                        |        `double`        | The coefficient to multiply with `minWaitTime`.                                                                                    | *Optional* | `0.5`                |  2.1  |
|       [minWindow](#transit_dynamicSearchWindow_minWindow)                                                                                  |       `duration`       | The constant minimum duration for a raptor-search-window.                                                                          | *Optional* | `"PT40M"`            |  2.2  |
|       [stepMinutes](#transit_dynamicSearchWindow_stepMinutes)                                                                              |        `integer`       | Used to set the steps the search-window is rounded to.                                                                             | *Optional* | `10`                 |  2.1  |
|    [pagingSearchWindowAdjustments](#transit_pagingSearchWindowAdjustments)                                                                 |      `duration[]`      | The provided array of durations is used to increase the search-window for the next/previous page.                                  | *Optional* |                      |   na  |
|    [stopTransferCost](#transit_stopTransferCost)                                                                                           |  `enum map of integer` | Use this to set a stop transfer cost for the given transfer priority                                                               | *Optional* |                      |   na  |
|    [transferCacheRequests](#transit_transferCacheRequests)                                                                                 |       `object[]`       | Routing requests to use for pre-filling the stop-to-stop transfer cache.                                                           | *Optional* |                      |  2.3  |
|       { object }                                                                                                                           |        `object`        | Nested object in array. The object type is determined by the parameters.                                                           | *Optional* |                      |  2.3  |
|          [alightSlack](#transit_transferCacheRequests_0_alightSlack)                                                                       |       `duration`       | The minimum extra time after exiting a public transport vehicle.                                                                   | *Optional* | `"PT0S"`             |  2.0  |
|          allowKeepingRentedBicycleAtDestination                                                                                            |        `boolean`       | If a vehicle should be allowed to be kept at the end of a station-based rental.                                                    | *Optional* | `false`              |  2.2  |
|          arriveBy                                                                                                                          |        `boolean`       | Whether the trip should depart or arrive at the specified date and time.                                                           | *Optional* | `false`              |  2.0  |
|          [bikeBoardCost](#transit_transferCacheRequests_0_bikeBoardCost)                                                                   |        `integer`       | Prevents unnecessary transfers by adding a cost for boarding a vehicle.                                                            | *Optional* | `600`                |  2.0  |
|          bikeParkCost                                                                                                                      |        `integer`       | Cost to park a bike.                                                                                                               | *Optional* | `120`                |  2.0  |
|          bikeParkTime                                                                                                                      |        `integer`       | Time to park a bike.                                                                                                               | *Optional* | `60`                 |  2.0  |
|          bikeReluctance                                                                                                                    |        `double`        | A multiplier for how bad biking is, compared to being in transit for equal lengths of time.                                        | *Optional* | `2.0`                |  2.0  |
|          bikeRentalDropoffCost                                                                                                             |        `integer`       | Cost to drop-off a rented bike.                                                                                                    | *Optional* | `30`                 |  2.0  |
|          bikeRentalDropoffTime                                                                                                             |        `integer`       | Time to drop-off a rented bike.                                                                                                    | *Optional* | `30`                 |  2.0  |
|          bikeRentalPickupCost                                                                                                              |        `integer`       | Cost to rent a bike.                                                                                                               | *Optional* | `120`                |  2.0  |
|          bikeRentalPickupTime                                                                                                              |        `integer`       | Time to rent a bike.                                                                                                               | *Optional* | `60`                 |  2.0  |
|          bikeSpeed                                                                                                                         |        `double`        | Max bike speed along streets, in meters per second                                                                                 | *Optional* | `5.0`                |  2.0  |
|          bikeStairsReluctance                                                                                                              |        `double`        | How bad is it to walk the bicycle up/down a flight of stairs compared to taking a detour.                                          | *Optional* | `10.0`               |  2.3  |
|          bikeSwitchCost                                                                                                                    |        `integer`       | The cost of the user fetching their bike and parking it again.                                                                     | *Optional* | `0`                  |  2.0  |
|          bikeSwitchTime                                                                                                                    |        `integer`       | The time it takes the user to fetch their bike and park it again in seconds.                                                       | *Optional* | `0`                  |  2.0  |
|          bikeTriangleSafetyFactor                                                                                                          |        `double`        | For bike triangle routing, how much safety matters (range 0-1).                                                                    | *Optional* | `0.0`                |  2.0  |
|          bikeTriangleSlopeFactor                                                                                                           |        `double`        | For bike triangle routing, how much slope matters (range 0-1).                                                                     | *Optional* | `0.0`                |  2.0  |
|          bikeTriangleTimeFactor                                                                                                            |        `double`        | For bike triangle routing, how much time matters (range 0-1).                                                                      | *Optional* | `0.0`                |  2.0  |
|          bikeWalkingReluctance                                                                                                             |        `double`        | A multiplier for how bad walking with a bike is, compared to being in transit for equal lengths of time.                           | *Optional* | `5.0`                |  2.1  |
|          bikeWalkingSpeed                                                                                                                  |        `double`        | The user's bike walking speed in meters/second. Defaults to approximately 3 MPH.                                                   | *Optional* | `1.33`               |  2.1  |
|          [boardSlack](#transit_transferCacheRequests_0_boardSlack)                                                                         |       `duration`       | The boardSlack is the minimum extra time to board a public transport vehicle.                                                      | *Optional* | `"PT0S"`             |  2.0  |
|          carAccelerationSpeed                                                                                                              |        `double`        | The acceleration speed of an automobile, in meters per second per second.                                                          | *Optional* | `2.9`                |  2.0  |
|          carDecelerationSpeed                                                                                                              |        `double`        | The deceleration speed of an automobile, in meters per second per second.                                                          | *Optional* | `2.9`                |  2.0  |
|          carDropoffTime                                                                                                                    |        `integer`       | Time to park a car in a park and ride, w/o taking into account driving and walking cost.                                           | *Optional* | `120`                |  2.0  |
|          carParkCost                                                                                                                       |        `integer`       | Cost of parking a car.                                                                                                             | *Optional* | `120`                |  2.1  |
|          carParkTime                                                                                                                       |        `integer`       | Time to park a car                                                                                                                 | *Optional* | `60`                 |  2.1  |
|          carPickupCost                                                                                                                     |        `integer`       | Add a cost for car pickup changes when a pickup or drop off takes place                                                            | *Optional* | `120`                |  2.1  |
|          carPickupTime                                                                                                                     |        `integer`       | Add a time for car pickup changes when a pickup or drop off takes place                                                            | *Optional* | `60`                 |  2.1  |
|          carReluctance                                                                                                                     |        `double`        | A multiplier for how bad driving is, compared to being in transit for equal lengths of time.                                       | *Optional* | `2.0`                |  2.0  |
|          carSpeed                                                                                                                          |        `double`        | Max car speed along streets, in meters per second                                                                                  | *Optional* | `40.0`               |  2.0  |
|          [drivingDirection](#transit_transferCacheRequests_0_drivingDirection)                                                             |         `enum`         | The driving direction to use in the intersection traversal calculation                                                             | *Optional* | `"right"`            |  2.2  |
|          elevatorBoardCost                                                                                                                 |        `integer`       | What is the cost of boarding a elevator?                                                                                           | *Optional* | `90`                 |  2.0  |
|          elevatorBoardTime                                                                                                                 |        `integer`       | How long does it take to get on an elevator, on average.                                                                           | *Optional* | `90`                 |  2.0  |
|          elevatorHopCost                                                                                                                   |        `integer`       | What is the cost of travelling one floor on an elevator?                                                                           | *Optional* | `20`                 |  2.0  |
|          elevatorHopTime                                                                                                                   |        `integer`       | How long does it take to advance one floor on an elevator?                                                                         | *Optional* | `20`                 |  2.0  |
|          geoidElevation                                                                                                                    |        `boolean`       | If true, the Graph's ellipsoidToGeoidDifference is applied to all elevations returned by this query.                               | *Optional* | `false`              |  2.0  |
|          ignoreRealtimeUpdates                                                                                                             |        `boolean`       | When true, realtime updates are ignored during this search.                                                                        | *Optional* | `false`              |  2.0  |
|          [intersectionTraversalModel](#transit_transferCacheRequests_0_intersectionTraversalModel)                                         |         `enum`         | The model that computes the costs of turns.                                                                                        | *Optional* | `"simple"`           |  2.2  |
|          keepingRentedBicycleAtDestinationCost                                                                                             |        `double`        | The cost of arriving at the destination with the rented bicycle, to discourage doing so.                                           | *Optional* | `0.0`                |  2.2  |
|          locale                                                                                                                            |        `locale`        | TODO                                                                                                                               | *Optional* | `"en_US"`            |  2.0  |
|          [maxAccessEgressDuration](#transit_transferCacheRequests_0_maxAccessEgressDuration)                                               |       `duration`       | This is the maximum duration for access/egress for street searches.                                                                | *Optional* | `"PT45M"`            |  2.1  |
|          [maxDirectStreetDuration](#transit_transferCacheRequests_0_maxDirectStreetDuration)                                               |       `duration`       | This is the maximum duration for a direct street search for each mode.                                                             | *Optional* | `"PT4H"`             |  2.1  |
|          [maxJourneyDuration](#transit_transferCacheRequests_0_maxJourneyDuration)                                                         |       `duration`       | The expected maximum time a journey can last across all possible journeys for the current deployment.                              | *Optional* | `"PT24H"`            |  2.1  |
|          modes                                                                                                                             |        `string`        | The set of access/egress/direct/transit modes to be used for the route search.                                                     | *Optional* | `"TRANSIT,WALK"`     |  2.0  |
|          nonpreferredTransferPenalty                                                                                                       |        `integer`       | Penalty (in seconds) for using a non-preferred transfer.                                                                           | *Optional* | `180`                |  2.0  |
|          numItineraries                                                                                                                    |        `integer`       | The maximum number of itineraries to return.                                                                                       | *Optional* | `12`                 |  2.0  |
|          [optimize](#transit_transferCacheRequests_0_optimize)                                                                             |         `enum`         | The set of characteristics that the user wants to optimize for.                                                                    | *Optional* | `"safe"`             |  2.0  |
|          [otherThanPreferredRoutesPenalty](#transit_transferCacheRequests_0_otherThanPreferredRoutesPenalty)                               |        `integer`       | Penalty added for using every route that is not preferred if user set any route as preferred.                                      | *Optional* | `300`                |  2.0  |
|          [relaxTransitSearchGeneralizedCostAtDestination](#transit_transferCacheRequests_0_relaxTransitSearchGeneralizedCostAtDestination) |        `double`        | Whether non-optimal transit paths at the destination should be returned                                                            | *Optional* |                      |  2.3  |
|          [searchWindow](#transit_transferCacheRequests_0_searchWindow)                                                                     |       `duration`       | The duration of the search-window.                                                                                                 | *Optional* |                      |  2.0  |
|          stairsReluctance                                                                                                                  |        `double`        | Used instead of walkReluctance for stairs.                                                                                         | *Optional* | `2.0`                |  2.0  |
|          [stairsTimeFactor](#transit_transferCacheRequests_0_stairsTimeFactor)                                                             |        `double`        | How much more time does it take to walk a flight of stairs compared to walking a similar horizontal length.                        | *Optional* | `3.0`                |  2.1  |
|          [transferPenalty](#transit_transferCacheRequests_0_transferPenalty)                                                               |        `integer`       | An additional penalty added to boardings after the first.                                                                          | *Optional* | `0`                  |  2.0  |
|          [transferSlack](#transit_transferCacheRequests_0_transferSlack)                                                                   |        `integer`       | The extra time needed to make a safe transfer in seconds.                                                                          | *Optional* | `120`                |  2.0  |
|          turnReluctance                                                                                                                    |        `double`        | Multiplicative factor on expected turning time.                                                                                    | *Optional* | `1.0`                |  2.0  |
|          [unpreferredCost](#transit_transferCacheRequests_0_unpreferredCost)                                                               |    `linear-function`   | A cost function used to calculate penalty for an unpreferred route.                                                                | *Optional* | `"f(x) = 0 + 1.0 x"` |  2.2  |
|          [unpreferredVehicleParkingTagCost](#transit_transferCacheRequests_0_unpreferredVehicleParkingTagCost)                             |        `integer`       | What cost to add if a parking facility doesn't contain a preferred tag.                                                            | *Optional* | `300`                |  2.3  |
|          useBikeRentalAvailabilityInformation                                                                                              |        `boolean`       | Whether or not bike rental availability information will be used to plan bike rental trips.                                        | *Optional* | `false`              |  2.0  |
|          waitReluctance                                                                                                                    |        `double`        | How much worse is waiting for a transit vehicle than being on a transit vehicle, as a multiplier.                                  | *Optional* | `1.0`                |  2.0  |
|          walkBoardCost                                                                                                                     |        `integer`       | Prevents unnecessary transfers by adding a cost for boarding a vehicle. This is the cost that is used when boarding while walking. | *Optional* | `600`                |  2.0  |
|          [walkReluctance](#transit_transferCacheRequests_0_walkReluctance)                                                                 |        `double`        | A multiplier for how bad walking is, compared to being in transit for equal lengths of time.                                       | *Optional* | `2.0`                |  2.0  |
|          [walkSafetyFactor](#transit_transferCacheRequests_0_walkSafetyFactor)                                                             |        `double`        | Factor for how much the walk safety is considered in routing.                                                                      | *Optional* | `1.0`                |  2.2  |
|          walkSpeed                                                                                                                         |        `double`        | The user's walking speed in meters/second.                                                                                         | *Optional* | `1.33`               |  2.0  |
|          [alightSlackForMode](#transit_transferCacheRequests_0_alightSlackForMode)                                                         | `enum map of duration` | How much extra time should be given when alighting a vehicle for each given mode.                                                  | *Optional* |                      |  2.0  |
|          [allowedVehicleRentalNetworks](#transit_transferCacheRequests_0_allowedVehicleRentalNetworks)                                     |       `string[]`       | The vehicle rental networks which may be used. If empty all networks may be used.                                                  | *Optional* |                      |  2.1  |
|          [bannedVehicleParkingTags](#transit_transferCacheRequests_0_bannedVehicleParkingTags)                                             |       `string[]`       | Tags with which a vehicle parking will not be used. If empty, no tags are banned.                                                  | *Optional* |                      |  2.1  |
|          [bannedVehicleRentalNetworks](#transit_transferCacheRequests_0_bannedVehicleRentalNetworks)                                       |       `string[]`       | he vehicle rental networks which may not be used. If empty, no networks are banned.                                                | *Optional* |                      |  2.1  |
|          [boardSlackForMode](#transit_transferCacheRequests_0_boardSlackForMode)                                                           | `enum map of duration` | How much extra time should be given when boarding a vehicle for each given mode.                                                   | *Optional* |                      |  2.0  |
|          [itineraryFilters](#transit_transferCacheRequests_0_itineraryFilters)                                                             |        `object`        | Configure itinerary filters that may modify itineraries, sort them, and filter away less preferable results.                       | *Optional* |                      |  2.0  |
|          [maxAccessEgressDurationForMode](#transit_transferCacheRequests_0_maxAccessEgressDurationForMode)                                 | `enum map of duration` | Limit access/egress per street mode.                                                                                               | *Optional* |                      |  2.1  |
|          [maxDirectStreetDurationForMode](#transit_transferCacheRequests_0_maxDirectStreetDurationForMode)                                 | `enum map of duration` | Limit direct route duration per street mode.                                                                                       | *Optional* |                      |  2.2  |
|          [preferredVehicleParkingTags](#transit_transferCacheRequests_0_preferredVehicleParkingTags)                                       |       `string[]`       | Vehicle parking facilities that don't have one of these tags will receive an extra cost and will therefore be penalised.           | *Optional* |                      |  2.3  |
|          [requiredVehicleParkingTags](#transit_transferCacheRequests_0_requiredVehicleParkingTags)                                         |       `string[]`       | Tags without which a vehicle parking will not be used. If empty, no tags are required.                                             | *Optional* |                      |  2.1  |
|          [transferOptimization](#transit_transferCacheRequests_0_transferOptimization)                                                     |        `object`        | Optimize where a transfer between to trip happens.                                                                                 | *Optional* |                      |  2.1  |
|          [transitReluctanceForMode](#transit_transferCacheRequests_0_transitReluctanceForMode)                                             |  `enum map of double`  | Transit reluctance for a given transport mode                                                                                      | *Optional* |                      |  2.1  |
|          [unpreferred](#transit_transferCacheRequests_0_unpreferred)                                                                       |        `object`        | Parameters listing authorities or lines that preferably should not be used in trip patters.                                        | *Optional* |                      |  2.2  |
|          wheelchairAccessibility                                                                                                           |        `object`        | See [Wheelchair Accessibility](Accessibility.md)                                                                                   | *Optional* |                      |  2.2  |
|       { object }                                                                                                                           |        `object`        | Nested object in array. The object type is determined by the parameters.                                                           | *Optional* |                      |  2.3  |
|          [alightSlack](#transit_transferCacheRequests_1_alightSlack)                                                                       |       `duration`       | The minimum extra time after exiting a public transport vehicle.                                                                   | *Optional* | `"PT0S"`             |  2.0  |
|          allowKeepingRentedBicycleAtDestination                                                                                            |        `boolean`       | If a vehicle should be allowed to be kept at the end of a station-based rental.                                                    | *Optional* | `false`              |  2.2  |
|          arriveBy                                                                                                                          |        `boolean`       | Whether the trip should depart or arrive at the specified date and time.                                                           | *Optional* | `false`              |  2.0  |
|          [bikeBoardCost](#transit_transferCacheRequests_1_bikeBoardCost)                                                                   |        `integer`       | Prevents unnecessary transfers by adding a cost for boarding a vehicle.                                                            | *Optional* | `600`                |  2.0  |
|          bikeParkCost                                                                                                                      |        `integer`       | Cost to park a bike.                                                                                                               | *Optional* | `120`                |  2.0  |
|          bikeParkTime                                                                                                                      |        `integer`       | Time to park a bike.                                                                                                               | *Optional* | `60`                 |  2.0  |
|          bikeReluctance                                                                                                                    |        `double`        | A multiplier for how bad biking is, compared to being in transit for equal lengths of time.                                        | *Optional* | `2.0`                |  2.0  |
|          bikeRentalDropoffCost                                                                                                             |        `integer`       | Cost to drop-off a rented bike.                                                                                                    | *Optional* | `30`                 |  2.0  |
|          bikeRentalDropoffTime                                                                                                             |        `integer`       | Time to drop-off a rented bike.                                                                                                    | *Optional* | `30`                 |  2.0  |
|          bikeRentalPickupCost                                                                                                              |        `integer`       | Cost to rent a bike.                                                                                                               | *Optional* | `120`                |  2.0  |
|          bikeRentalPickupTime                                                                                                              |        `integer`       | Time to rent a bike.                                                                                                               | *Optional* | `60`                 |  2.0  |
|          bikeSpeed                                                                                                                         |        `double`        | Max bike speed along streets, in meters per second                                                                                 | *Optional* | `5.0`                |  2.0  |
|          bikeStairsReluctance                                                                                                              |        `double`        | How bad is it to walk the bicycle up/down a flight of stairs compared to taking a detour.                                          | *Optional* | `10.0`               |  2.3  |
|          bikeSwitchCost                                                                                                                    |        `integer`       | The cost of the user fetching their bike and parking it again.                                                                     | *Optional* | `0`                  |  2.0  |
|          bikeSwitchTime                                                                                                                    |        `integer`       | The time it takes the user to fetch their bike and park it again in seconds.                                                       | *Optional* | `0`                  |  2.0  |
|          bikeTriangleSafetyFactor                                                                                                          |        `double`        | For bike triangle routing, how much safety matters (range 0-1).                                                                    | *Optional* | `0.0`                |  2.0  |
|          bikeTriangleSlopeFactor                                                                                                           |        `double`        | For bike triangle routing, how much slope matters (range 0-1).                                                                     | *Optional* | `0.0`                |  2.0  |
|          bikeTriangleTimeFactor                                                                                                            |        `double`        | For bike triangle routing, how much time matters (range 0-1).                                                                      | *Optional* | `0.0`                |  2.0  |
|          bikeWalkingReluctance                                                                                                             |        `double`        | A multiplier for how bad walking with a bike is, compared to being in transit for equal lengths of time.                           | *Optional* | `5.0`                |  2.1  |
|          bikeWalkingSpeed                                                                                                                  |        `double`        | The user's bike walking speed in meters/second. Defaults to approximately 3 MPH.                                                   | *Optional* | `1.33`               |  2.1  |
|          [boardSlack](#transit_transferCacheRequests_1_boardSlack)                                                                         |       `duration`       | The boardSlack is the minimum extra time to board a public transport vehicle.                                                      | *Optional* | `"PT0S"`             |  2.0  |
|          carAccelerationSpeed                                                                                                              |        `double`        | The acceleration speed of an automobile, in meters per second per second.                                                          | *Optional* | `2.9`                |  2.0  |
|          carDecelerationSpeed                                                                                                              |        `double`        | The deceleration speed of an automobile, in meters per second per second.                                                          | *Optional* | `2.9`                |  2.0  |
|          carDropoffTime                                                                                                                    |        `integer`       | Time to park a car in a park and ride, w/o taking into account driving and walking cost.                                           | *Optional* | `120`                |  2.0  |
|          carParkCost                                                                                                                       |        `integer`       | Cost of parking a car.                                                                                                             | *Optional* | `120`                |  2.1  |
|          carParkTime                                                                                                                       |        `integer`       | Time to park a car                                                                                                                 | *Optional* | `60`                 |  2.1  |
|          carPickupCost                                                                                                                     |        `integer`       | Add a cost for car pickup changes when a pickup or drop off takes place                                                            | *Optional* | `120`                |  2.1  |
|          carPickupTime                                                                                                                     |        `integer`       | Add a time for car pickup changes when a pickup or drop off takes place                                                            | *Optional* | `60`                 |  2.1  |
|          carReluctance                                                                                                                     |        `double`        | A multiplier for how bad driving is, compared to being in transit for equal lengths of time.                                       | *Optional* | `2.0`                |  2.0  |
|          carSpeed                                                                                                                          |        `double`        | Max car speed along streets, in meters per second                                                                                  | *Optional* | `40.0`               |  2.0  |
|          [drivingDirection](#transit_transferCacheRequests_1_drivingDirection)                                                             |         `enum`         | The driving direction to use in the intersection traversal calculation                                                             | *Optional* | `"right"`            |  2.2  |
|          elevatorBoardCost                                                                                                                 |        `integer`       | What is the cost of boarding a elevator?                                                                                           | *Optional* | `90`                 |  2.0  |
|          elevatorBoardTime                                                                                                                 |        `integer`       | How long does it take to get on an elevator, on average.                                                                           | *Optional* | `90`                 |  2.0  |
|          elevatorHopCost                                                                                                                   |        `integer`       | What is the cost of travelling one floor on an elevator?                                                                           | *Optional* | `20`                 |  2.0  |
|          elevatorHopTime                                                                                                                   |        `integer`       | How long does it take to advance one floor on an elevator?                                                                         | *Optional* | `20`                 |  2.0  |
|          geoidElevation                                                                                                                    |        `boolean`       | If true, the Graph's ellipsoidToGeoidDifference is applied to all elevations returned by this query.                               | *Optional* | `false`              |  2.0  |
|          ignoreRealtimeUpdates                                                                                                             |        `boolean`       | When true, realtime updates are ignored during this search.                                                                        | *Optional* | `false`              |  2.0  |
|          [intersectionTraversalModel](#transit_transferCacheRequests_1_intersectionTraversalModel)                                         |         `enum`         | The model that computes the costs of turns.                                                                                        | *Optional* | `"simple"`           |  2.2  |
|          keepingRentedBicycleAtDestinationCost                                                                                             |        `double`        | The cost of arriving at the destination with the rented bicycle, to discourage doing so.                                           | *Optional* | `0.0`                |  2.2  |
|          locale                                                                                                                            |        `locale`        | TODO                                                                                                                               | *Optional* | `"en_US"`            |  2.0  |
|          [maxAccessEgressDuration](#transit_transferCacheRequests_1_maxAccessEgressDuration)                                               |       `duration`       | This is the maximum duration for access/egress for street searches.                                                                | *Optional* | `"PT45M"`            |  2.1  |
|          [maxDirectStreetDuration](#transit_transferCacheRequests_1_maxDirectStreetDuration)                                               |       `duration`       | This is the maximum duration for a direct street search for each mode.                                                             | *Optional* | `"PT4H"`             |  2.1  |
|          [maxJourneyDuration](#transit_transferCacheRequests_1_maxJourneyDuration)                                                         |       `duration`       | The expected maximum time a journey can last across all possible journeys for the current deployment.                              | *Optional* | `"PT24H"`            |  2.1  |
|          modes                                                                                                                             |        `string`        | The set of access/egress/direct/transit modes to be used for the route search.                                                     | *Optional* | `"TRANSIT,WALK"`     |  2.0  |
|          nonpreferredTransferPenalty                                                                                                       |        `integer`       | Penalty (in seconds) for using a non-preferred transfer.                                                                           | *Optional* | `180`                |  2.0  |
|          numItineraries                                                                                                                    |        `integer`       | The maximum number of itineraries to return.                                                                                       | *Optional* | `12`                 |  2.0  |
|          [optimize](#transit_transferCacheRequests_1_optimize)                                                                             |         `enum`         | The set of characteristics that the user wants to optimize for.                                                                    | *Optional* | `"safe"`             |  2.0  |
|          [otherThanPreferredRoutesPenalty](#transit_transferCacheRequests_1_otherThanPreferredRoutesPenalty)                               |        `integer`       | Penalty added for using every route that is not preferred if user set any route as preferred.                                      | *Optional* | `300`                |  2.0  |
|          [relaxTransitSearchGeneralizedCostAtDestination](#transit_transferCacheRequests_1_relaxTransitSearchGeneralizedCostAtDestination) |        `double`        | Whether non-optimal transit paths at the destination should be returned                                                            | *Optional* |                      |  2.3  |
|          [searchWindow](#transit_transferCacheRequests_1_searchWindow)                                                                     |       `duration`       | The duration of the search-window.                                                                                                 | *Optional* |                      |  2.0  |
|          stairsReluctance                                                                                                                  |        `double`        | Used instead of walkReluctance for stairs.                                                                                         | *Optional* | `2.0`                |  2.0  |
|          [stairsTimeFactor](#transit_transferCacheRequests_1_stairsTimeFactor)                                                             |        `double`        | How much more time does it take to walk a flight of stairs compared to walking a similar horizontal length.                        | *Optional* | `3.0`                |  2.1  |
|          [transferPenalty](#transit_transferCacheRequests_1_transferPenalty)                                                               |        `integer`       | An additional penalty added to boardings after the first.                                                                          | *Optional* | `0`                  |  2.0  |
|          [transferSlack](#transit_transferCacheRequests_1_transferSlack)                                                                   |        `integer`       | The extra time needed to make a safe transfer in seconds.                                                                          | *Optional* | `120`                |  2.0  |
|          turnReluctance                                                                                                                    |        `double`        | Multiplicative factor on expected turning time.                                                                                    | *Optional* | `1.0`                |  2.0  |
|          [unpreferredCost](#transit_transferCacheRequests_1_unpreferredCost)                                                               |    `linear-function`   | A cost function used to calculate penalty for an unpreferred route.                                                                | *Optional* | `"f(x) = 0 + 1.0 x"` |  2.2  |
|          [unpreferredVehicleParkingTagCost](#transit_transferCacheRequests_1_unpreferredVehicleParkingTagCost)                             |        `integer`       | What cost to add if a parking facility doesn't contain a preferred tag.                                                            | *Optional* | `300`                |  2.3  |
|          useBikeRentalAvailabilityInformation                                                                                              |        `boolean`       | Whether or not bike rental availability information will be used to plan bike rental trips.                                        | *Optional* | `false`              |  2.0  |
|          waitReluctance                                                                                                                    |        `double`        | How much worse is waiting for a transit vehicle than being on a transit vehicle, as a multiplier.                                  | *Optional* | `1.0`                |  2.0  |
|          walkBoardCost                                                                                                                     |        `integer`       | Prevents unnecessary transfers by adding a cost for boarding a vehicle. This is the cost that is used when boarding while walking. | *Optional* | `600`                |  2.0  |
|          [walkReluctance](#transit_transferCacheRequests_1_walkReluctance)                                                                 |        `double`        | A multiplier for how bad walking is, compared to being in transit for equal lengths of time.                                       | *Optional* | `2.0`                |  2.0  |
|          [walkSafetyFactor](#transit_transferCacheRequests_1_walkSafetyFactor)                                                             |        `double`        | Factor for how much the walk safety is considered in routing.                                                                      | *Optional* | `1.0`                |  2.2  |
|          walkSpeed                                                                                                                         |        `double`        | The user's walking speed in meters/second.                                                                                         | *Optional* | `1.33`               |  2.0  |
|          [alightSlackForMode](#transit_transferCacheRequests_1_alightSlackForMode)                                                         | `enum map of duration` | How much extra time should be given when alighting a vehicle for each given mode.                                                  | *Optional* |                      |  2.0  |
|          [allowedVehicleRentalNetworks](#transit_transferCacheRequests_1_allowedVehicleRentalNetworks)                                     |       `string[]`       | The vehicle rental networks which may be used. If empty all networks may be used.                                                  | *Optional* |                      |  2.1  |
|          [bannedVehicleParkingTags](#transit_transferCacheRequests_1_bannedVehicleParkingTags)                                             |       `string[]`       | Tags with which a vehicle parking will not be used. If empty, no tags are banned.                                                  | *Optional* |                      |  2.1  |
|          [bannedVehicleRentalNetworks](#transit_transferCacheRequests_1_bannedVehicleRentalNetworks)                                       |       `string[]`       | he vehicle rental networks which may not be used. If empty, no networks are banned.                                                | *Optional* |                      |  2.1  |
|          [boardSlackForMode](#transit_transferCacheRequests_1_boardSlackForMode)                                                           | `enum map of duration` | How much extra time should be given when boarding a vehicle for each given mode.                                                   | *Optional* |                      |  2.0  |
|          [itineraryFilters](#transit_transferCacheRequests_1_itineraryFilters)                                                             |        `object`        | Configure itinerary filters that may modify itineraries, sort them, and filter away less preferable results.                       | *Optional* |                      |  2.0  |
|          [maxAccessEgressDurationForMode](#transit_transferCacheRequests_1_maxAccessEgressDurationForMode)                                 | `enum map of duration` | Limit access/egress per street mode.                                                                                               | *Optional* |                      |  2.1  |
|          [maxDirectStreetDurationForMode](#transit_transferCacheRequests_1_maxDirectStreetDurationForMode)                                 | `enum map of duration` | Limit direct route duration per street mode.                                                                                       | *Optional* |                      |  2.2  |
|          [preferredVehicleParkingTags](#transit_transferCacheRequests_1_preferredVehicleParkingTags)                                       |       `string[]`       | Vehicle parking facilities that don't have one of these tags will receive an extra cost and will therefore be penalised.           | *Optional* |                      |  2.3  |
|          [requiredVehicleParkingTags](#transit_transferCacheRequests_1_requiredVehicleParkingTags)                                         |       `string[]`       | Tags without which a vehicle parking will not be used. If empty, no tags are required.                                             | *Optional* |                      |  2.1  |
|          [transferOptimization](#transit_transferCacheRequests_1_transferOptimization)                                                     |        `object`        | Optimize where a transfer between to trip happens.                                                                                 | *Optional* |                      |  2.1  |
|          [transitReluctanceForMode](#transit_transferCacheRequests_1_transitReluctanceForMode)                                             |  `enum map of double`  | Transit reluctance for a given transport mode                                                                                      | *Optional* |                      |  2.1  |
|          [unpreferred](#transit_transferCacheRequests_1_unpreferred)                                                                       |        `object`        | Parameters listing authorities or lines that preferably should not be used in trip patters.                                        | *Optional* |                      |  2.2  |
|          wheelchairAccessibility                                                                                                           |        `object`        | See [Wheelchair Accessibility](Accessibility.md)                                                                                   | *Optional* |                      |  2.2  |
|             enabled                                                                                                                        |        `boolean`       | Enable wheelchair accessibility.                                                                                                   | *Optional* | `false`              |  2.0  |
|             inaccessibleStreetReluctance                                                                                                   |        `double`        | The factor to multiply the cost of traversing a street edge that is not wheelchair-accessible.                                     | *Optional* | `25.0`               |  2.2  |
|             [maxSlope](#transit_transferCacheRequests_1_wheelchairAccessibility_maxSlope)                                                  |        `double`        | The maximum slope as a fraction of 1.                                                                                              | *Optional* | `0.083`              |  2.0  |
|             [slopeExceededReluctance](#transit_transferCacheRequests_1_wheelchairAccessibility_slopeExceededReluctance)                    |        `double`        | How much streets with high slope should be avoided.                                                                                | *Optional* | `1.0`                |  2.2  |
|             [stairsReluctance](#transit_transferCacheRequests_1_wheelchairAccessibility_stairsReluctance)                                  |        `double`        | How much stairs should be avoided.                                                                                                 | *Optional* | `100.0`              |  2.2  |
|             elevator                                                                                                                       |        `object`        | Configuration for when to use inaccessible elevators.                                                                              | *Optional* |                      |  2.2  |
|             stop                                                                                                                           |        `object`        | Configuration for when to use inaccessible stops.                                                                                  | *Optional* |                      |  2.2  |
|             trip                                                                                                                           |        `object`        | Configuration for when to use inaccessible trips.                                                                                  | *Optional* |                      |  2.2  |
| transmodelApi                                                                                                                              |        `object`        | Configuration for the Transmodel GraphQL API.                                                                                      | *Optional* |                      |   na  |
|    [hideFeedId](#transmodelApi_hideFeedId)                                                                                                 |        `boolean`       | Hide the FeedId in all API output, and add it to input.                                                                            | *Optional* | `false`              |   na  |
|    [tracingHeaderTags](#transmodelApi_tracingHeaderTags)                                                                                   |       `string[]`       | Used to group requests when monitoring OTP.                                                                                        | *Optional* |                      |   na  |
| [updaters](UpdaterConfig.md)                                                                                                               |       `object[]`       | Configuration for the updaters that import various types of data into OTP.                                                         | *Optional* |                      |  1.5  |
| [vectorTileLayers](sandbox/MapboxVectorTilesApi.md)                                                                                        |       `object[]`       | Configuration of the individual layers for the Mapbox vector tiles.                                                                | *Optional* |                      |  2.0  |
| vehicleRentalServiceDirectory                                                                                                              |        `object`        | Configuration for the vehicle rental service directory.                                                                            | *Optional* |                      |  2.0  |
|    language                                                                                                                                |        `string`        | Language code.                                                                                                                     | *Optional* |                      |   na  |
|    sourcesName                                                                                                                             |        `string`        | Json tag name for updater sources.                                                                                                 | *Optional* | `"systems"`          |   na  |
|    updaterNetworkName                                                                                                                      |        `string`        | Json tag name for the network name for each source.                                                                                | *Optional* | `"id"`               |   na  |
|    updaterUrlName                                                                                                                          |        `string`        | Json tag name for endpoint urls for each source.                                                                                   | *Optional* | `"url"`              |   na  |
|    url                                                                                                                                     |          `uri`         | Endpoint for the VehicleRentalServiceDirectory                                                                                     | *Required* |                      |   na  |
|    [headers](#vehicleRentalServiceDirectory_headers)                                                                                       |     `map of string`    | Http headers.                                                                                                                      | *Optional* |                      |   na  |

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

**Since version:** `na` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT5S"`   
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

**Since version:** `na` ∙ **Type:** `enum map of integer` ∙ **Cardinality:** `Optional`   
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
**Path:** /transit 

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


<h3 id="transit_transferCacheRequests_0_alightSlack">alightSlack</h3>

**Since version:** `2.0` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT0S"`   
**Path:** /transit/transferCacheRequests/[0] 

The minimum extra time after exiting a public transport vehicle.

The slack is added to the time when going from the transit vehicle to the stop.

<h3 id="transit_transferCacheRequests_0_bikeBoardCost">bikeBoardCost</h3>

**Since version:** `2.0` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `600`   
**Path:** /transit/transferCacheRequests/[0] 

Prevents unnecessary transfers by adding a cost for boarding a vehicle.

This is the cost that is used when boarding while cycling.This is usually higher that walkBoardCost.

<h3 id="transit_transferCacheRequests_0_boardSlack">boardSlack</h3>

**Since version:** `2.0` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT0S"`   
**Path:** /transit/transferCacheRequests/[0] 

The boardSlack is the minimum extra time to board a public transport vehicle.

The board time is added to the time when going from the stop (offboard) to onboard a transit
vehicle.

This is the same as the `minimumTransferTime`, except that this also apply to to the first
transit leg in the trip. This is the default value used, if not overridden by the `boardSlackList`.


<h3 id="transit_transferCacheRequests_0_drivingDirection">drivingDirection</h3>

**Since version:** `2.2` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"right"`   
**Path:** /transit/transferCacheRequests/[0]   
**Enum values:** `right` | `left`

The driving direction to use in the intersection traversal calculation

<h3 id="transit_transferCacheRequests_0_intersectionTraversalModel">intersectionTraversalModel</h3>

**Since version:** `2.2` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"simple"`   
**Path:** /transit/transferCacheRequests/[0]   
**Enum values:** `norway` | `simple`

The model that computes the costs of turns.

<h3 id="transit_transferCacheRequests_0_maxAccessEgressDuration">maxAccessEgressDuration</h3>

**Since version:** `2.1` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT45M"`   
**Path:** /transit/transferCacheRequests/[0] 

This is the maximum duration for access/egress for street searches.

This is a performance limit and should therefore be set high. Results close to the limit are not
guaranteed to be optimal. Use itinerary-filters to limit what is presented to the client. The
duration can be set per mode(`maxAccessEgressDurationForMode`), because some street modes searches
are much more resource intensive than others. A default value is applied if the mode specific value
do not exist.


<h3 id="transit_transferCacheRequests_0_maxDirectStreetDuration">maxDirectStreetDuration</h3>

**Since version:** `2.1` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT4H"`   
**Path:** /transit/transferCacheRequests/[0] 

This is the maximum duration for a direct street search for each mode.

This is a performance limit and should therefore be set high. Results close to the limit are not
guaranteed to be optimal. Use itinerary-filters to limit what is presented to the client. The
duration can be set per mode(`maxDirectStreetDurationForMode`), because some street modes searches
are much more resource intensive than others. A default value is applied if the mode specific value
do not exist."


<h3 id="transit_transferCacheRequests_0_maxJourneyDuration">maxJourneyDuration</h3>

**Since version:** `2.1` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT24H"`   
**Path:** /transit/transferCacheRequests/[0] 

The expected maximum time a journey can last across all possible journeys for the current deployment.

Normally you would just do an estimate and add enough slack, so you are sure that there is no
journeys that falls outside this window. The parameter is used find all possible dates for the
journey and then search only the services which run on those dates. The duration must include
access, egress, wait-time and transit time for the whole journey. It should also take low frequency
days/periods like holidays into account. In other words, pick the two points within your area that
has the worst connection and then try to travel on the worst possible day, and find the maximum
journey duration. Using a value that is too high has the effect of including more patterns in the
search, hence, making it a bit slower. Recommended values would be from 12 hours(small town/city),
1 day (region) to 2 days (country like Norway)."


<h3 id="transit_transferCacheRequests_0_optimize">optimize</h3>

**Since version:** `2.0` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"safe"`   
**Path:** /transit/transferCacheRequests/[0]   
**Enum values:** `quick` | `safe` | `flat` | `greenways` | `triangle`

The set of characteristics that the user wants to optimize for.

<h3 id="transit_transferCacheRequests_0_otherThanPreferredRoutesPenalty">otherThanPreferredRoutesPenalty</h3>

**Since version:** `2.0` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `300`   
**Path:** /transit/transferCacheRequests/[0] 

Penalty added for using every route that is not preferred if user set any route as preferred.

We return number of seconds that we are willing to wait for preferred route.

<h3 id="transit_transferCacheRequests_0_relaxTransitSearchGeneralizedCostAtDestination">relaxTransitSearchGeneralizedCostAtDestination</h3>

**Since version:** `2.3` ∙ **Type:** `double` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[0] 

Whether non-optimal transit paths at the destination should be returned

Let c be the existing minimum pareto optimal generalized cost to beat. Then a trip
with cost c' is accepted if the following is true:
`c' < Math.round(c * relaxRaptorCostCriteria)`.

The parameter is optional. If not set a normal comparison is performed.

Values equals or less than zero is not allowed. Values greater than 2.0 are not
supported, due to performance reasons.


<h3 id="transit_transferCacheRequests_0_searchWindow">searchWindow</h3>

**Since version:** `2.0` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[0] 

The duration of the search-window.

This is the time/duration in seconds from the earliest-departure-time(EDT) to the
latest-departure-time(LDT). In case of a reverse search it will be the time from earliest to
latest arrival time (LAT - EAT).

All optimal travels that depart within the search window is guarantied to be found.

This is sometimes referred to as the Range Raptor Search Window - but could be used in a none
Transit search as well; Hence this is named search-window and not raptor-search-window.

This is normally dynamically calculated by the server. Use `null` to unset, and *zero* to do one
Raptor iteration. The value is dynamically  assigned a suitable value, if not set. In a small to
medium size operation you may use a fixed value, like 60 minutes. If you have a mixture of high
frequency cities routes and  infrequent long distant journeys, the best option is normally to use
the dynamic auto assignment. If not provided the value is resolved depending on the other input
parameters, available transit options and realtime changes.

There is no need to set this when going to the next/previous page. The OTP Server will
increase/decrease the search-window when paging to match the requested number of itineraries.


<h3 id="transit_transferCacheRequests_0_stairsTimeFactor">stairsTimeFactor</h3>

**Since version:** `2.1` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `3.0`   
**Path:** /transit/transferCacheRequests/[0] 

How much more time does it take to walk a flight of stairs compared to walking a similar horizontal length.

Default value is based on: Fujiyama, T., & Tyler, N. (2010). Predicting the walking
speed of pedestrians on stairs. Transportation Planning and Technology, 33(2), 177–202.


<h3 id="transit_transferCacheRequests_0_transferPenalty">transferPenalty</h3>

**Since version:** `2.0` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0`   
**Path:** /transit/transferCacheRequests/[0] 

An additional penalty added to boardings after the first.

The value is in OTP's internal weight units, which are roughly equivalent to seconds.
Set this to a high value to discourage transfers. Of course, transfers that save
significant time or walking will still be taken.


<h3 id="transit_transferCacheRequests_0_transferSlack">transferSlack</h3>

**Since version:** `2.0` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `120`   
**Path:** /transit/transferCacheRequests/[0] 

The extra time needed to make a safe transfer in seconds.

An expected transfer time in seconds that specifies the amount of time that must pass
between exiting one public transport vehicle and boarding another. This time is in
addition to time it might take to walk between stops, the board-slack, and the
alight-slack."


<h3 id="transit_transferCacheRequests_0_unpreferredCost">unpreferredCost</h3>

**Since version:** `2.2` ∙ **Type:** `linear-function` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"f(x) = 0 + 1.0 x"`   
**Path:** /transit/transferCacheRequests/[0] 

A cost function used to calculate penalty for an unpreferred route.

Function should return number of seconds that we are willing to wait for preferred route
or for an unpreferred agency's departure. For example, 600 + 2.0 x


<h3 id="transit_transferCacheRequests_0_unpreferredVehicleParkingTagCost">unpreferredVehicleParkingTagCost</h3>

**Since version:** `2.3` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `300`   
**Path:** /transit/transferCacheRequests/[0] 

What cost to add if a parking facility doesn't contain a preferred tag.

See `preferredVehicleParkingTags`.

<h3 id="transit_transferCacheRequests_0_walkReluctance">walkReluctance</h3>

**Since version:** `2.0` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `2.0`   
**Path:** /transit/transferCacheRequests/[0] 

A multiplier for how bad walking is, compared to being in transit for equal lengths of time.

Empirically, values between 2 and 4 seem to correspond well to the concept of not wanting to walk
too much without asking for totally ridiculous itineraries, but this observation should in no way
be taken as scientific or definitive. Your mileage may vary.
See https://github.com/opentripplanner/OpenTripPlanner/issues/4090 for impact on performance with
high values.


<h3 id="transit_transferCacheRequests_0_walkSafetyFactor">walkSafetyFactor</h3>

**Since version:** `2.2` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `1.0`   
**Path:** /transit/transferCacheRequests/[0] 

Factor for how much the walk safety is considered in routing.

Value should be between 0 and 1. If the value is set to be 0, safety is ignored.

<h3 id="transit_transferCacheRequests_0_alightSlackForMode">alightSlackForMode</h3>

**Since version:** `2.0` ∙ **Type:** `enum map of duration` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[0]   
**Enum keys:** `rail` | `coach` | `subway` | `bus` | `tram` | `ferry` | `airplane` | `cable-car` | `gondola` | `funicular` | `trolleybus` | `monorail` | `carpool` | `taxi`

How much extra time should be given when alighting a vehicle for each given mode.

Sometimes there is a need to configure a longer alighting times for specific modes, such as airplanes or ferries.

<h3 id="transit_transferCacheRequests_0_allowedVehicleRentalNetworks">allowedVehicleRentalNetworks</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[0] 

The vehicle rental networks which may be used. If empty all networks may be used.

<h3 id="transit_transferCacheRequests_0_bannedVehicleParkingTags">bannedVehicleParkingTags</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[0] 

Tags with which a vehicle parking will not be used. If empty, no tags are banned.

<h3 id="transit_transferCacheRequests_0_bannedVehicleRentalNetworks">bannedVehicleRentalNetworks</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[0] 

he vehicle rental networks which may not be used. If empty, no networks are banned.

<h3 id="transit_transferCacheRequests_0_boardSlackForMode">boardSlackForMode</h3>

**Since version:** `2.0` ∙ **Type:** `enum map of duration` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[0]   
**Enum keys:** `rail` | `coach` | `subway` | `bus` | `tram` | `ferry` | `airplane` | `cable-car` | `gondola` | `funicular` | `trolleybus` | `monorail` | `carpool` | `taxi`

How much extra time should be given when boarding a vehicle for each given mode.

Sometimes there is a need to configure a board times for specific modes, such as airplanes or
ferries, where the check-in process needs to be done in good time before ride.


<h3 id="transit_transferCacheRequests_0_itineraryFilters">itineraryFilters</h3>

**Since version:** `2.0` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[0] 

Configure itinerary filters that may modify itineraries, sort them, and filter away less preferable results.

The purpose of the itinerary filter chain is to post process the result returned by the routing
search. The filters may modify itineraries, sort them, and filter away less preferable results.

OTP2 may produce numerous _pareto-optimal_ results when using `time`, `number-of-transfers` and
`generalized-cost` as criteria. Use the parameters listed here to reduce/filter the itineraries
return by the search engine before returning the results to client. There is also a few mandatory
non-configurable filters removing none optimal results. You may see these filters pop-up in the
filter debugging.

#### Group by similarity filters

The group-by-filter is a bit complex, but should be simple to use. Set `debug=true` and experiment
with `searchWindow` and the three group-by parameters(`groupSimilarityKeepOne`,
`groupSimilarityKeepThree` and `groupedOtherThanSameLegsMaxCostMultiplier`).

The group-by-filter work by grouping itineraries together and then reducing the number of
itineraries in each group, keeping the itinerary/itineraries with the best itinerary
_generalized-cost_. The group-by function first pick all transit legs that account for more than N%
of the itinerary based on distance traveled. This become the group-key. Two keys are the same if all
legs in one of the keys also exist in the other. Note, one key may have a larger set of legs than the
other, but they can still be the same. When comparing two legs we compare the `tripId` and make sure
the legs overlap in place and time. Two legs are the same if both legs ride at least a common
subsection of the same trip. The `keepOne` filter will keep ONE itinerary in each group. The
`keepThree` keeps 3 itineraries for each group.

The grouped itineraries can be further reduced by using `groupedOtherThanSameLegsMaxCostMultiplier`.
This parameter filters out itineraries, where the legs that are not common for all the grouped
itineraries have a much higher cost, than the lowest in the group. By default, it filters out
itineraries that are at least double in cost for the non-grouped legs.


<h3 id="transit_transferCacheRequests_0_maxAccessEgressDurationForMode">maxAccessEgressDurationForMode</h3>

**Since version:** `2.1` ∙ **Type:** `enum map of duration` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[0]   
**Enum keys:** `not-set` | `walk` | `bike` | `bike-to-park` | `bike-rental` | `scooter-rental` | `car` | `car-to-park` | `car-pickup` | `car-rental` | `flexible`

Limit access/egress per street mode.

Override the settings in `maxAccessEgressDuration` for specific street modes. This is
done because some street modes searches are much more resource intensive than others.


<h3 id="transit_transferCacheRequests_0_maxDirectStreetDurationForMode">maxDirectStreetDurationForMode</h3>

**Since version:** `2.2` ∙ **Type:** `enum map of duration` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[0]   
**Enum keys:** `not-set` | `walk` | `bike` | `bike-to-park` | `bike-rental` | `scooter-rental` | `car` | `car-to-park` | `car-pickup` | `car-rental` | `flexible`

Limit direct route duration per street mode.

Override the settings in `maxDirectStreetDuration` for specific street modes. This is
done because some street modes searches are much more resource intensive than others.


<h3 id="transit_transferCacheRequests_0_preferredVehicleParkingTags">preferredVehicleParkingTags</h3>

**Since version:** `2.3` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[0] 

Vehicle parking facilities that don't have one of these tags will receive an extra cost and will therefore be penalised.

<h3 id="transit_transferCacheRequests_0_requiredVehicleParkingTags">requiredVehicleParkingTags</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[0] 

Tags without which a vehicle parking will not be used. If empty, no tags are required.

<h3 id="transit_transferCacheRequests_0_transferOptimization">transferOptimization</h3>

**Since version:** `2.1` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[0] 

Optimize where a transfer between to trip happens. 

The main purpose of transfer optimization is to handle cases where it is possible to transfer
between two routes at more than one point (pair of stops). The transfer optimization ensures that
transfers occur at the best possible location. By post-processing all paths returned by the router,
OTP can apply sophisticated calculations that are too slow or not algorithmically valid within
Raptor. Transfers are optimized is done after the Raptor search and before the paths are passed
to the itinerary-filter-chain.

To toggle transfer optimization on or off use the OTPFeature `OptimizeTransfers` (default is on).
You should leave this on unless there is a critical issue with it. The OTPFeature
`GuaranteedTransfers` will toggle on and off the priority optimization (part of OptimizeTransfers).

The optimized transfer service will try to, in order:

1. Use transfer priority. This includes stay-seated and guaranteed transfers.
2. Use the transfers with the best distribution of the wait-time, and avoid very short transfers.
3. Avoid back-travel
4. Boost stop-priority to select preferred and recommended stops.

If two paths have the same transfer priority level, then we break the tie by looking at waiting
times. The goal is to maximize the wait-time for each stop, avoiding situations where there is
little time available to make the transfer. This is balanced with the generalized-cost. The cost
is adjusted with a new cost for wait-time (optimized-wait-time-cost).

The defaults should work fine, but if you have results with short wait-times dominating a better
option or "back-travel", then try to increase the `minSafeWaitTimeFactor`,
`backTravelWaitTimeFactor` and/or `extraStopBoardAlightCostsFactor`.

For details on the logic/design see [transfer optimization](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/src/main/java/org/opentripplanner/routing/algorithm/transferoptimization/package.md)
package documentation.


<h3 id="transit_transferCacheRequests_0_transitReluctanceForMode">transitReluctanceForMode</h3>

**Since version:** `2.1` ∙ **Type:** `enum map of double` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[0]   
**Enum keys:** `rail` | `coach` | `subway` | `bus` | `tram` | `ferry` | `airplane` | `cable-car` | `gondola` | `funicular` | `trolleybus` | `monorail` | `carpool` | `taxi`

Transit reluctance for a given transport mode

<h3 id="transit_transferCacheRequests_0_unpreferred">unpreferred</h3>

**Since version:** `2.2` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[0] 

Parameters listing authorities or lines that preferably should not be used in trip patters.

A cost is applied to boarding nonpreferred authorities or routes.

The routing engine will add extra penalty - on the *unpreferred* routes and/or agencies using a
cost function. The cost function (`unpreferredCost`) is defined as a linear function of the form
`A + B x`, where `A` is a fixed cost (in seconds) and `B` is reluctance multiplier for transit leg
travel time `x` (in seconds).


<h3 id="transit_transferCacheRequests_1_alightSlack">alightSlack</h3>

**Since version:** `2.0` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT0S"`   
**Path:** /transit/transferCacheRequests/[1] 

The minimum extra time after exiting a public transport vehicle.

The slack is added to the time when going from the transit vehicle to the stop.

<h3 id="transit_transferCacheRequests_1_bikeBoardCost">bikeBoardCost</h3>

**Since version:** `2.0` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `600`   
**Path:** /transit/transferCacheRequests/[1] 

Prevents unnecessary transfers by adding a cost for boarding a vehicle.

This is the cost that is used when boarding while cycling.This is usually higher that walkBoardCost.

<h3 id="transit_transferCacheRequests_1_boardSlack">boardSlack</h3>

**Since version:** `2.0` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT0S"`   
**Path:** /transit/transferCacheRequests/[1] 

The boardSlack is the minimum extra time to board a public transport vehicle.

The board time is added to the time when going from the stop (offboard) to onboard a transit
vehicle.

This is the same as the `minimumTransferTime`, except that this also apply to to the first
transit leg in the trip. This is the default value used, if not overridden by the `boardSlackList`.


<h3 id="transit_transferCacheRequests_1_drivingDirection">drivingDirection</h3>

**Since version:** `2.2` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"right"`   
**Path:** /transit/transferCacheRequests/[1]   
**Enum values:** `right` | `left`

The driving direction to use in the intersection traversal calculation

<h3 id="transit_transferCacheRequests_1_intersectionTraversalModel">intersectionTraversalModel</h3>

**Since version:** `2.2` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"simple"`   
**Path:** /transit/transferCacheRequests/[1]   
**Enum values:** `norway` | `simple`

The model that computes the costs of turns.

<h3 id="transit_transferCacheRequests_1_maxAccessEgressDuration">maxAccessEgressDuration</h3>

**Since version:** `2.1` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT45M"`   
**Path:** /transit/transferCacheRequests/[1] 

This is the maximum duration for access/egress for street searches.

This is a performance limit and should therefore be set high. Results close to the limit are not
guaranteed to be optimal. Use itinerary-filters to limit what is presented to the client. The
duration can be set per mode(`maxAccessEgressDurationForMode`), because some street modes searches
are much more resource intensive than others. A default value is applied if the mode specific value
do not exist.


<h3 id="transit_transferCacheRequests_1_maxDirectStreetDuration">maxDirectStreetDuration</h3>

**Since version:** `2.1` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT4H"`   
**Path:** /transit/transferCacheRequests/[1] 

This is the maximum duration for a direct street search for each mode.

This is a performance limit and should therefore be set high. Results close to the limit are not
guaranteed to be optimal. Use itinerary-filters to limit what is presented to the client. The
duration can be set per mode(`maxDirectStreetDurationForMode`), because some street modes searches
are much more resource intensive than others. A default value is applied if the mode specific value
do not exist."


<h3 id="transit_transferCacheRequests_1_maxJourneyDuration">maxJourneyDuration</h3>

**Since version:** `2.1` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT24H"`   
**Path:** /transit/transferCacheRequests/[1] 

The expected maximum time a journey can last across all possible journeys for the current deployment.

Normally you would just do an estimate and add enough slack, so you are sure that there is no
journeys that falls outside this window. The parameter is used find all possible dates for the
journey and then search only the services which run on those dates. The duration must include
access, egress, wait-time and transit time for the whole journey. It should also take low frequency
days/periods like holidays into account. In other words, pick the two points within your area that
has the worst connection and then try to travel on the worst possible day, and find the maximum
journey duration. Using a value that is too high has the effect of including more patterns in the
search, hence, making it a bit slower. Recommended values would be from 12 hours(small town/city),
1 day (region) to 2 days (country like Norway)."


<h3 id="transit_transferCacheRequests_1_optimize">optimize</h3>

**Since version:** `2.0` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"safe"`   
**Path:** /transit/transferCacheRequests/[1]   
**Enum values:** `quick` | `safe` | `flat` | `greenways` | `triangle`

The set of characteristics that the user wants to optimize for.

<h3 id="transit_transferCacheRequests_1_otherThanPreferredRoutesPenalty">otherThanPreferredRoutesPenalty</h3>

**Since version:** `2.0` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `300`   
**Path:** /transit/transferCacheRequests/[1] 

Penalty added for using every route that is not preferred if user set any route as preferred.

We return number of seconds that we are willing to wait for preferred route.

<h3 id="transit_transferCacheRequests_1_relaxTransitSearchGeneralizedCostAtDestination">relaxTransitSearchGeneralizedCostAtDestination</h3>

**Since version:** `2.3` ∙ **Type:** `double` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[1] 

Whether non-optimal transit paths at the destination should be returned

Let c be the existing minimum pareto optimal generalized cost to beat. Then a trip
with cost c' is accepted if the following is true:
`c' < Math.round(c * relaxRaptorCostCriteria)`.

The parameter is optional. If not set a normal comparison is performed.

Values equals or less than zero is not allowed. Values greater than 2.0 are not
supported, due to performance reasons.


<h3 id="transit_transferCacheRequests_1_searchWindow">searchWindow</h3>

**Since version:** `2.0` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[1] 

The duration of the search-window.

This is the time/duration in seconds from the earliest-departure-time(EDT) to the
latest-departure-time(LDT). In case of a reverse search it will be the time from earliest to
latest arrival time (LAT - EAT).

All optimal travels that depart within the search window is guarantied to be found.

This is sometimes referred to as the Range Raptor Search Window - but could be used in a none
Transit search as well; Hence this is named search-window and not raptor-search-window.

This is normally dynamically calculated by the server. Use `null` to unset, and *zero* to do one
Raptor iteration. The value is dynamically  assigned a suitable value, if not set. In a small to
medium size operation you may use a fixed value, like 60 minutes. If you have a mixture of high
frequency cities routes and  infrequent long distant journeys, the best option is normally to use
the dynamic auto assignment. If not provided the value is resolved depending on the other input
parameters, available transit options and realtime changes.

There is no need to set this when going to the next/previous page. The OTP Server will
increase/decrease the search-window when paging to match the requested number of itineraries.


<h3 id="transit_transferCacheRequests_1_stairsTimeFactor">stairsTimeFactor</h3>

**Since version:** `2.1` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `3.0`   
**Path:** /transit/transferCacheRequests/[1] 

How much more time does it take to walk a flight of stairs compared to walking a similar horizontal length.

Default value is based on: Fujiyama, T., & Tyler, N. (2010). Predicting the walking
speed of pedestrians on stairs. Transportation Planning and Technology, 33(2), 177–202.


<h3 id="transit_transferCacheRequests_1_transferPenalty">transferPenalty</h3>

**Since version:** `2.0` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0`   
**Path:** /transit/transferCacheRequests/[1] 

An additional penalty added to boardings after the first.

The value is in OTP's internal weight units, which are roughly equivalent to seconds.
Set this to a high value to discourage transfers. Of course, transfers that save
significant time or walking will still be taken.


<h3 id="transit_transferCacheRequests_1_transferSlack">transferSlack</h3>

**Since version:** `2.0` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `120`   
**Path:** /transit/transferCacheRequests/[1] 

The extra time needed to make a safe transfer in seconds.

An expected transfer time in seconds that specifies the amount of time that must pass
between exiting one public transport vehicle and boarding another. This time is in
addition to time it might take to walk between stops, the board-slack, and the
alight-slack."


<h3 id="transit_transferCacheRequests_1_unpreferredCost">unpreferredCost</h3>

**Since version:** `2.2` ∙ **Type:** `linear-function` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"f(x) = 0 + 1.0 x"`   
**Path:** /transit/transferCacheRequests/[1] 

A cost function used to calculate penalty for an unpreferred route.

Function should return number of seconds that we are willing to wait for preferred route
or for an unpreferred agency's departure. For example, 600 + 2.0 x


<h3 id="transit_transferCacheRequests_1_unpreferredVehicleParkingTagCost">unpreferredVehicleParkingTagCost</h3>

**Since version:** `2.3` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `300`   
**Path:** /transit/transferCacheRequests/[1] 

What cost to add if a parking facility doesn't contain a preferred tag.

See `preferredVehicleParkingTags`.

<h3 id="transit_transferCacheRequests_1_walkReluctance">walkReluctance</h3>

**Since version:** `2.0` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `2.0`   
**Path:** /transit/transferCacheRequests/[1] 

A multiplier for how bad walking is, compared to being in transit for equal lengths of time.

Empirically, values between 2 and 4 seem to correspond well to the concept of not wanting to walk
too much without asking for totally ridiculous itineraries, but this observation should in no way
be taken as scientific or definitive. Your mileage may vary.
See https://github.com/opentripplanner/OpenTripPlanner/issues/4090 for impact on performance with
high values.


<h3 id="transit_transferCacheRequests_1_walkSafetyFactor">walkSafetyFactor</h3>

**Since version:** `2.2` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `1.0`   
**Path:** /transit/transferCacheRequests/[1] 

Factor for how much the walk safety is considered in routing.

Value should be between 0 and 1. If the value is set to be 0, safety is ignored.

<h3 id="transit_transferCacheRequests_1_alightSlackForMode">alightSlackForMode</h3>

**Since version:** `2.0` ∙ **Type:** `enum map of duration` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[1]   
**Enum keys:** `rail` | `coach` | `subway` | `bus` | `tram` | `ferry` | `airplane` | `cable-car` | `gondola` | `funicular` | `trolleybus` | `monorail` | `carpool` | `taxi`

How much extra time should be given when alighting a vehicle for each given mode.

Sometimes there is a need to configure a longer alighting times for specific modes, such as airplanes or ferries.

<h3 id="transit_transferCacheRequests_1_allowedVehicleRentalNetworks">allowedVehicleRentalNetworks</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[1] 

The vehicle rental networks which may be used. If empty all networks may be used.

<h3 id="transit_transferCacheRequests_1_bannedVehicleParkingTags">bannedVehicleParkingTags</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[1] 

Tags with which a vehicle parking will not be used. If empty, no tags are banned.

<h3 id="transit_transferCacheRequests_1_bannedVehicleRentalNetworks">bannedVehicleRentalNetworks</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[1] 

he vehicle rental networks which may not be used. If empty, no networks are banned.

<h3 id="transit_transferCacheRequests_1_boardSlackForMode">boardSlackForMode</h3>

**Since version:** `2.0` ∙ **Type:** `enum map of duration` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[1]   
**Enum keys:** `rail` | `coach` | `subway` | `bus` | `tram` | `ferry` | `airplane` | `cable-car` | `gondola` | `funicular` | `trolleybus` | `monorail` | `carpool` | `taxi`

How much extra time should be given when boarding a vehicle for each given mode.

Sometimes there is a need to configure a board times for specific modes, such as airplanes or
ferries, where the check-in process needs to be done in good time before ride.


<h3 id="transit_transferCacheRequests_1_itineraryFilters">itineraryFilters</h3>

**Since version:** `2.0` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[1] 

Configure itinerary filters that may modify itineraries, sort them, and filter away less preferable results.

The purpose of the itinerary filter chain is to post process the result returned by the routing
search. The filters may modify itineraries, sort them, and filter away less preferable results.

OTP2 may produce numerous _pareto-optimal_ results when using `time`, `number-of-transfers` and
`generalized-cost` as criteria. Use the parameters listed here to reduce/filter the itineraries
return by the search engine before returning the results to client. There is also a few mandatory
non-configurable filters removing none optimal results. You may see these filters pop-up in the
filter debugging.

#### Group by similarity filters

The group-by-filter is a bit complex, but should be simple to use. Set `debug=true` and experiment
with `searchWindow` and the three group-by parameters(`groupSimilarityKeepOne`,
`groupSimilarityKeepThree` and `groupedOtherThanSameLegsMaxCostMultiplier`).

The group-by-filter work by grouping itineraries together and then reducing the number of
itineraries in each group, keeping the itinerary/itineraries with the best itinerary
_generalized-cost_. The group-by function first pick all transit legs that account for more than N%
of the itinerary based on distance traveled. This become the group-key. Two keys are the same if all
legs in one of the keys also exist in the other. Note, one key may have a larger set of legs than the
other, but they can still be the same. When comparing two legs we compare the `tripId` and make sure
the legs overlap in place and time. Two legs are the same if both legs ride at least a common
subsection of the same trip. The `keepOne` filter will keep ONE itinerary in each group. The
`keepThree` keeps 3 itineraries for each group.

The grouped itineraries can be further reduced by using `groupedOtherThanSameLegsMaxCostMultiplier`.
This parameter filters out itineraries, where the legs that are not common for all the grouped
itineraries have a much higher cost, than the lowest in the group. By default, it filters out
itineraries that are at least double in cost for the non-grouped legs.


<h3 id="transit_transferCacheRequests_1_maxAccessEgressDurationForMode">maxAccessEgressDurationForMode</h3>

**Since version:** `2.1` ∙ **Type:** `enum map of duration` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[1]   
**Enum keys:** `not-set` | `walk` | `bike` | `bike-to-park` | `bike-rental` | `scooter-rental` | `car` | `car-to-park` | `car-pickup` | `car-rental` | `flexible`

Limit access/egress per street mode.

Override the settings in `maxAccessEgressDuration` for specific street modes. This is
done because some street modes searches are much more resource intensive than others.


<h3 id="transit_transferCacheRequests_1_maxDirectStreetDurationForMode">maxDirectStreetDurationForMode</h3>

**Since version:** `2.2` ∙ **Type:** `enum map of duration` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[1]   
**Enum keys:** `not-set` | `walk` | `bike` | `bike-to-park` | `bike-rental` | `scooter-rental` | `car` | `car-to-park` | `car-pickup` | `car-rental` | `flexible`

Limit direct route duration per street mode.

Override the settings in `maxDirectStreetDuration` for specific street modes. This is
done because some street modes searches are much more resource intensive than others.


<h3 id="transit_transferCacheRequests_1_preferredVehicleParkingTags">preferredVehicleParkingTags</h3>

**Since version:** `2.3` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[1] 

Vehicle parking facilities that don't have one of these tags will receive an extra cost and will therefore be penalised.

<h3 id="transit_transferCacheRequests_1_requiredVehicleParkingTags">requiredVehicleParkingTags</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[1] 

Tags without which a vehicle parking will not be used. If empty, no tags are required.

<h3 id="transit_transferCacheRequests_1_transferOptimization">transferOptimization</h3>

**Since version:** `2.1` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[1] 

Optimize where a transfer between to trip happens. 

The main purpose of transfer optimization is to handle cases where it is possible to transfer
between two routes at more than one point (pair of stops). The transfer optimization ensures that
transfers occur at the best possible location. By post-processing all paths returned by the router,
OTP can apply sophisticated calculations that are too slow or not algorithmically valid within
Raptor. Transfers are optimized is done after the Raptor search and before the paths are passed
to the itinerary-filter-chain.

To toggle transfer optimization on or off use the OTPFeature `OptimizeTransfers` (default is on).
You should leave this on unless there is a critical issue with it. The OTPFeature
`GuaranteedTransfers` will toggle on and off the priority optimization (part of OptimizeTransfers).

The optimized transfer service will try to, in order:

1. Use transfer priority. This includes stay-seated and guaranteed transfers.
2. Use the transfers with the best distribution of the wait-time, and avoid very short transfers.
3. Avoid back-travel
4. Boost stop-priority to select preferred and recommended stops.

If two paths have the same transfer priority level, then we break the tie by looking at waiting
times. The goal is to maximize the wait-time for each stop, avoiding situations where there is
little time available to make the transfer. This is balanced with the generalized-cost. The cost
is adjusted with a new cost for wait-time (optimized-wait-time-cost).

The defaults should work fine, but if you have results with short wait-times dominating a better
option or "back-travel", then try to increase the `minSafeWaitTimeFactor`,
`backTravelWaitTimeFactor` and/or `extraStopBoardAlightCostsFactor`.

For details on the logic/design see [transfer optimization](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/src/main/java/org/opentripplanner/routing/algorithm/transferoptimization/package.md)
package documentation.


<h3 id="transit_transferCacheRequests_1_transitReluctanceForMode">transitReluctanceForMode</h3>

**Since version:** `2.1` ∙ **Type:** `enum map of double` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[1]   
**Enum keys:** `rail` | `coach` | `subway` | `bus` | `tram` | `ferry` | `airplane` | `cable-car` | `gondola` | `funicular` | `trolleybus` | `monorail` | `carpool` | `taxi`

Transit reluctance for a given transport mode

<h3 id="transit_transferCacheRequests_1_unpreferred">unpreferred</h3>

**Since version:** `2.2` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transit/transferCacheRequests/[1] 

Parameters listing authorities or lines that preferably should not be used in trip patters.

A cost is applied to boarding nonpreferred authorities or routes.

The routing engine will add extra penalty - on the *unpreferred* routes and/or agencies using a
cost function. The cost function (`unpreferredCost`) is defined as a linear function of the form
`A + B x`, where `A` is a fixed cost (in seconds) and `B` is reluctance multiplier for transit leg
travel time `x` (in seconds).


<h3 id="transit_transferCacheRequests_1_wheelchairAccessibility_maxSlope">maxSlope</h3>

**Since version:** `2.0` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.083`   
**Path:** /transit/transferCacheRequests/[1]/wheelchairAccessibility 

The maximum slope as a fraction of 1.

9 percent would be `0.09`

<h3 id="transit_transferCacheRequests_1_wheelchairAccessibility_slopeExceededReluctance">slopeExceededReluctance</h3>

**Since version:** `2.2` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `1.0`   
**Path:** /transit/transferCacheRequests/[1]/wheelchairAccessibility 

How much streets with high slope should be avoided.

What factor should be given to street edges, which are over the
max slope. The penalty is not static but scales with how much you
exceed the maximum slope. Set to negative to disable routing on
too steep edges.


<h3 id="transit_transferCacheRequests_1_wheelchairAccessibility_stairsReluctance">stairsReluctance</h3>

**Since version:** `2.2` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `100.0`   
**Path:** /transit/transferCacheRequests/[1]/wheelchairAccessibility 

How much stairs should be avoided.

Stairs are not completely excluded for wheelchair users but
severely punished. This value determines how much they are
punished. This should be a very high value as you want to only
include stairs as a last result.

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
  ]
}
```

<!-- JSON-EXAMPLE END -->
