<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc/templates
   - Generated directory is: /doc/user 
-->

# Route Request

The RouteRequest is the type for the [routingDefaults in router-config.json](RouterConfiguration.md#routingDefaults) 
and in the [transferRequests in build-config.json](BuildConfiguration.md#transferRequests).

<!-- PARAMETERS-TABLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                                                                             |          Type          | Summary                                                                                                                                                  |  Req./Opt. | Default Value    | Since |
|--------------------------------------------------------------------------------------------------------------|:----------------------:|----------------------------------------------------------------------------------------------------------------------------------------------------------|:----------:|------------------|:-----:|
| [alightSlack](#rd_alightSlack)                                                                               |       `duration`       | The time safety margin when alighting from a vehicle.                                                                                                    | *Optional* | `"PT0S"`         |  2.0  |
| arriveBy                                                                                                     |        `boolean`       | Whether the trip should depart or arrive at the specified date and time.                                                                                 | *Optional* | `false`          |  2.0  |
| [boardSlack](#rd_boardSlack)                                                                                 |       `duration`       | The time safety margin when boarding a vehicle.                                                                                                          | *Optional* | `"PT0S"`         |  2.0  |
| [drivingDirection](#rd_drivingDirection)                                                                     |         `enum`         | The driving direction to use in the intersection traversal calculation                                                                                   | *Optional* | `"right"`        |  2.2  |
| elevatorBoardCost                                                                                            |        `integer`       | What is the cost of boarding a elevator?                                                                                                                 | *Optional* | `90`             |  2.0  |
| elevatorBoardTime                                                                                            |        `integer`       | How long does it take to get on an elevator, on average.                                                                                                 | *Optional* | `90`             |  2.0  |
| elevatorHopCost                                                                                              |        `integer`       | What is the cost of travelling one floor on an elevator?                                                                                                 | *Optional* | `20`             |  2.0  |
| elevatorHopTime                                                                                              |        `integer`       | How long does it take to advance one floor on an elevator?                                                                                               | *Optional* | `20`             |  2.0  |
| geoidElevation                                                                                               |        `boolean`       | If true, the Graph's ellipsoidToGeoidDifference is applied to all elevations returned by this query.                                                     | *Optional* | `false`          |  2.0  |
| ignoreRealtimeUpdates                                                                                        |        `boolean`       | When true, real-time updates are ignored during this search.                                                                                             | *Optional* | `false`          |  2.0  |
| [intersectionTraversalModel](#rd_intersectionTraversalModel)                                                 |         `enum`         | The model that computes the costs of turns.                                                                                                              | *Optional* | `"simple"`       |  2.2  |
| locale                                                                                                       |        `locale`        | TODO                                                                                                                                                     | *Optional* | `"en_US"`        |  2.0  |
| [maxDirectStreetDuration](#rd_maxDirectStreetDuration)                                                       |       `duration`       | This is the maximum duration for a direct street search for each mode.                                                                                   | *Optional* | `"PT4H"`         |  2.1  |
| [maxJourneyDuration](#rd_maxJourneyDuration)                                                                 |       `duration`       | The expected maximum time a journey can last across all possible journeys for the current deployment.                                                    | *Optional* | `"PT24H"`        |  2.1  |
| modes                                                                                                        |        `string`        | The set of access/egress/direct/transfer modes (separated by a comma) to be used for the route search.                                                   | *Optional* | `"WALK"`         |  2.0  |
| nonpreferredTransferPenalty                                                                                  |        `integer`       | Penalty (in seconds) for using a non-preferred transfer.                                                                                                 | *Optional* | `180`            |  2.0  |
| numItineraries                                                                                               |        `integer`       | The maximum number of itineraries to return.                                                                                                             | *Optional* | `50`             |  2.0  |
| [otherThanPreferredRoutesPenalty](#rd_otherThanPreferredRoutesPenalty)                                       |        `integer`       | Penalty added for using every route that is not preferred if user set any route as preferred.                                                            | *Optional* | `300`            |  2.0  |
| [relaxTransitGroupPriority](#rd_relaxTransitGroupPriority)                                                   |        `string`        | The relax function for transit-group-priority                                                                                                            | *Optional* | `"0s + 1.00 t"`  |  2.5  |
| [relaxTransitSearchGeneralizedCostAtDestination](#rd_relaxTransitSearchGeneralizedCostAtDestination)         |        `double`        | Whether non-optimal transit paths at the destination should be returned                                                                                  | *Optional* |                  |  2.3  |
| [searchWindow](#rd_searchWindow)                                                                             |       `duration`       | The duration of the search-window.                                                                                                                       | *Optional* |                  |  2.0  |
| [streetRoutingTimeout](#rd_streetRoutingTimeout)                                                             |       `duration`       | The maximum time a street routing request is allowed to take before returning the results.                                                               | *Optional* | `"PT5S"`         |  2.2  |
| [transferPenalty](#rd_transferPenalty)                                                                       |        `integer`       | An additional penalty added to boardings after the first.                                                                                                | *Optional* | `0`              |  2.0  |
| [transferSlack](#rd_transferSlack)                                                                           |       `duration`       | The extra time needed to make a safe transfer.                                                                                                           | *Optional* | `"PT2M"`         |  2.0  |
| turnReluctance                                                                                               |        `double`        | Multiplicative factor on expected turning time.                                                                                                          | *Optional* | `1.0`            |  2.0  |
| [unpreferredCost](#rd_unpreferredCost)                                                                       | `cost-linear-function` | A cost function used to calculate penalty for an unpreferred route.                                                                                      | *Optional* | `"0s + 1.00 t"`  |  2.2  |
| waitReluctance                                                                                               |        `double`        | How much worse is waiting for a transit vehicle than being on a transit vehicle, as a multiplier.                                                        | *Optional* | `1.0`            |  2.0  |
| accessEgress                                                                                                 |        `object`        | Parameters for access and egress routing.                                                                                                                | *Optional* |                  |  2.4  |
|    [maxDuration](#rd_accessEgress_maxDuration)                                                               |       `duration`       | This is the maximum duration for access/egress for street searches.                                                                                      | *Optional* | `"PT45M"`        |  2.1  |
|    [maxStopCount](#rd_accessEgress_maxStopCount)                                                             |        `integer`       | Maximal number of stops collected in access/egress routing                                                                                               | *Optional* | `500`            |  2.4  |
|    [maxDurationForMode](#rd_accessEgress_maxDurationForMode)                                                 | `enum map of duration` | Limit access/egress per street mode.                                                                                                                     | *Optional* |                  |  2.1  |
|    [maxStopCountForMode](#rd_accessEgress_maxStopCountForMode)                                               |  `enum map of integer` | Maximal number of stops collected in access/egress routing for the given mode                                                                            | *Optional* |                  |  2.7  |
|    [penalty](#rd_accessEgress_penalty)                                                                       |  `enum map of object`  | Penalty for access/egress by street mode.                                                                                                                | *Optional* |                  |  2.4  |
|       FLEXIBLE                                                                                               |        `object`        | NA                                                                                                                                                       | *Optional* |                  |  2.4  |
|          costFactor                                                                                          |        `double`        | A factor multiplied with the time-penalty to get the cost-penalty.                                                                                       | *Optional* | `0.0`            |  2.4  |
|          timePenalty                                                                                         |     `time-penalty`     | Penalty added to the time of a path/leg.                                                                                                                 | *Optional* | `"0s + 0.00 t"`  |  2.4  |
| [alightSlackForMode](#rd_alightSlackForMode)                                                                 | `enum map of duration` | How much extra time should be given when alighting a vehicle for each given mode.                                                                        | *Optional* |                  |  2.0  |
| bicycle                                                                                                      |        `object`        | Bicycle preferences.                                                                                                                                     | *Optional* |                  |  2.5  |
|    [boardCost](#rd_bicycle_boardCost)                                                                        |        `integer`       | Prevents unnecessary transfers by adding a cost for boarding a transit vehicle.                                                                          | *Optional* | `600`            |  2.0  |
|    [optimization](#rd_bicycle_optimization)                                                                  |         `enum`         | The set of characteristics that the user wants to optimize for.                                                                                          | *Optional* | `"safe-streets"` |  2.0  |
|    reluctance                                                                                                |        `double`        | A multiplier for how bad cycling is, compared to being in transit for equal lengths of time.                                                             | *Optional* | `2.0`            |  2.0  |
|    speed                                                                                                     |        `double`        | Max bicycle speed along streets, in meters per second                                                                                                    | *Optional* | `5.0`            |  2.0  |
|    parking                                                                                                   |        `object`        | Preferences for parking a vehicle.                                                                                                                       | *Optional* |                  |  2.5  |
|       cost                                                                                                   |        `integer`       | Cost to park a vehicle.                                                                                                                                  | *Optional* | `120`            |  2.0  |
|       time                                                                                                   |       `duration`       | Time to park a vehicle.                                                                                                                                  | *Optional* | `"PT1M"`         |  2.0  |
|       [unpreferredVehicleParkingTagCost](#rd_bicycle_parking_unpreferredVehicleParkingTagCost)               |        `integer`       | What cost to add if a parking facility doesn't contain a preferred tag.                                                                                  | *Optional* | `300`            |  2.3  |
|       [bannedVehicleParkingTags](#rd_bicycle_parking_bannedVehicleParkingTags)                               |       `string[]`       | Tags with which a vehicle parking will not be used. If empty, no tags are banned.                                                                        | *Optional* |                  |  2.1  |
|       [preferredVehicleParkingTags](#rd_bicycle_parking_preferredVehicleParkingTags)                         |       `string[]`       | Vehicle parking facilities that don't have one of these tags will receive an extra cost and will therefore be penalised.                                 | *Optional* |                  |  2.3  |
|       [requiredVehicleParkingTags](#rd_bicycle_parking_requiredVehicleParkingTags)                           |       `string[]`       | Tags without which a vehicle parking will not be used. If empty, no tags are required.                                                                   | *Optional* |                  |  2.1  |
|    rental                                                                                                    |        `object`        | Vehicle rental options                                                                                                                                   | *Optional* |                  |  2.3  |
|       allowKeepingAtDestination                                                                              |        `boolean`       | If a vehicle should be allowed to be kept at the end of a station-based rental.                                                                          | *Optional* | `false`          |  2.2  |
|       dropOffCost                                                                                            |        `integer`       | Cost to drop-off a rented vehicle.                                                                                                                       | *Optional* | `30`             |  2.0  |
|       dropOffTime                                                                                            |       `duration`       | Time to drop-off a rented vehicle.                                                                                                                       | *Optional* | `"PT30S"`        |  2.0  |
|       keepingAtDestinationCost                                                                               |        `integer`       | The cost of arriving at the destination with the rented vehicle, to discourage doing so.                                                                 | *Optional* | `0`              |  2.2  |
|       pickupCost                                                                                             |        `integer`       | Cost to rent a vehicle.                                                                                                                                  | *Optional* | `120`            |  2.0  |
|       pickupTime                                                                                             |       `duration`       | Time to rent a vehicle.                                                                                                                                  | *Optional* | `"PT1M"`         |  2.0  |
|       useAvailabilityInformation                                                                             |        `boolean`       | Whether or not vehicle rental availability information will be used to plan vehicle rental trips.                                                        | *Optional* | `false`          |  2.0  |
|       [allowedNetworks](#rd_bicycle_rental_allowedNetworks)                                                  |       `string[]`       | The vehicle rental networks which may be used. If empty all networks may be used.                                                                        | *Optional* |                  |  2.1  |
|       [bannedNetworks](#rd_bicycle_rental_bannedNetworks)                                                    |       `string[]`       | The vehicle rental networks which may not be used. If empty, no networks are banned.                                                                     | *Optional* |                  |  2.1  |
|    [triangle](#rd_bicycle_triangle)                                                                          |        `object`        | Triangle optimization criteria.                                                                                                                          | *Optional* |                  |  2.5  |
|       flatness                                                                                               |        `double`        | Relative importance of flat terrain (range 0-1).                                                                                                         | *Optional* | `0.0`            |  2.0  |
|       [safety](#rd_bicycle_triangle_safety)                                                                  |        `double`        | Relative importance of safety (range 0-1).                                                                                                               | *Optional* | `0.0`            |  2.0  |
|       time                                                                                                   |        `double`        | Relative importance of duration of travel (range 0-1).                                                                                                   | *Optional* | `0.0`            |  2.0  |
|    walk                                                                                                      |        `object`        | Preferences for walking a vehicle.                                                                                                                       | *Optional* |                  |  2.5  |
|       [mountDismountCost](#rd_bicycle_walk_mountDismountCost)                                                |        `integer`       | The cost of hopping on or off a vehicle.                                                                                                                 | *Optional* | `0`              |  2.0  |
|       [mountDismountTime](#rd_bicycle_walk_mountDismountTime)                                                |       `duration`       | The time it takes the user to hop on or off a vehicle.                                                                                                   | *Optional* | `"PT0S"`         |  2.0  |
|       reluctance                                                                                             |        `double`        | A multiplier for how bad walking with a vehicle is, compared to being in transit for equal lengths of time.                                              | *Optional* | `5.0`            |  2.1  |
|       speed                                                                                                  |        `double`        | The user's vehicle walking speed in meters/second. Defaults to approximately 3 MPH.                                                                      | *Optional* | `1.33`           |  2.1  |
|       stairsReluctance                                                                                       |        `double`        | How bad is it to walk the vehicle up/down a flight of stairs compared to taking a detour.                                                                | *Optional* | `10.0`           |  2.3  |
| [boardSlackForMode](#rd_boardSlackForMode)                                                                   | `enum map of duration` | How much extra time should be given when boarding a vehicle for each given mode.                                                                         | *Optional* |                  |  2.0  |
| car                                                                                                          |        `object`        | Car preferences.                                                                                                                                         | *Optional* |                  |  2.5  |
|    accelerationSpeed                                                                                         |        `double`        | The acceleration speed of an automobile, in meters per second per second.                                                                                | *Optional* | `2.9`            |  2.0  |
|    [boardCost](#rd_car_boardCost)                                                                            |        `integer`       | Prevents unnecessary transfers by adding a cost for boarding a transit vehicle.                                                                          | *Optional* | `600`            |  2.7  |
|    decelerationSpeed                                                                                         |        `double`        | The deceleration speed of an automobile, in meters per second per second.                                                                                | *Optional* | `2.9`            |  2.0  |
|    pickupCost                                                                                                |        `integer`       | Add a cost for car pickup changes when a pickup or drop off takes place                                                                                  | *Optional* | `120`            |  2.1  |
|    pickupTime                                                                                                |       `duration`       | Add a time for car pickup changes when a pickup or drop off takes place                                                                                  | *Optional* | `"PT1M"`         |  2.1  |
|    reluctance                                                                                                |        `double`        | A multiplier for how bad driving is, compared to being in transit for equal lengths of time.                                                             | *Optional* | `2.0`            |  2.0  |
|    parking                                                                                                   |        `object`        | Preferences for parking a vehicle.                                                                                                                       | *Optional* |                  |  2.5  |
|       cost                                                                                                   |        `integer`       | Cost to park a vehicle.                                                                                                                                  | *Optional* | `120`            |  2.0  |
|       time                                                                                                   |       `duration`       | Time to park a vehicle.                                                                                                                                  | *Optional* | `"PT1M"`         |  2.0  |
|       [unpreferredVehicleParkingTagCost](#rd_car_parking_unpreferredVehicleParkingTagCost)                   |        `integer`       | What cost to add if a parking facility doesn't contain a preferred tag.                                                                                  | *Optional* | `300`            |  2.3  |
|       [bannedVehicleParkingTags](#rd_car_parking_bannedVehicleParkingTags)                                   |       `string[]`       | Tags with which a vehicle parking will not be used. If empty, no tags are banned.                                                                        | *Optional* |                  |  2.1  |
|       [preferredVehicleParkingTags](#rd_car_parking_preferredVehicleParkingTags)                             |       `string[]`       | Vehicle parking facilities that don't have one of these tags will receive an extra cost and will therefore be penalised.                                 | *Optional* |                  |  2.3  |
|       [requiredVehicleParkingTags](#rd_car_parking_requiredVehicleParkingTags)                               |       `string[]`       | Tags without which a vehicle parking will not be used. If empty, no tags are required.                                                                   | *Optional* |                  |  2.1  |
|    rental                                                                                                    |        `object`        | Vehicle rental options                                                                                                                                   | *Optional* |                  |  2.3  |
|       allowKeepingAtDestination                                                                              |        `boolean`       | If a vehicle should be allowed to be kept at the end of a station-based rental.                                                                          | *Optional* | `false`          |  2.2  |
|       dropOffCost                                                                                            |        `integer`       | Cost to drop-off a rented vehicle.                                                                                                                       | *Optional* | `30`             |  2.0  |
|       dropOffTime                                                                                            |       `duration`       | Time to drop-off a rented vehicle.                                                                                                                       | *Optional* | `"PT30S"`        |  2.0  |
|       keepingAtDestinationCost                                                                               |        `integer`       | The cost of arriving at the destination with the rented vehicle, to discourage doing so.                                                                 | *Optional* | `0`              |  2.2  |
|       pickupCost                                                                                             |        `integer`       | Cost to rent a vehicle.                                                                                                                                  | *Optional* | `120`            |  2.0  |
|       pickupTime                                                                                             |       `duration`       | Time to rent a vehicle.                                                                                                                                  | *Optional* | `"PT1M"`         |  2.0  |
|       useAvailabilityInformation                                                                             |        `boolean`       | Whether or not vehicle rental availability information will be used to plan vehicle rental trips.                                                        | *Optional* | `false`          |  2.0  |
|       [allowedNetworks](#rd_car_rental_allowedNetworks)                                                      |       `string[]`       | The vehicle rental networks which may be used. If empty all networks may be used.                                                                        | *Optional* |                  |  2.1  |
|       [bannedNetworks](#rd_car_rental_bannedNetworks)                                                        |       `string[]`       | The vehicle rental networks which may not be used. If empty, no networks are banned.                                                                     | *Optional* |                  |  2.1  |
| [itineraryFilters](#rd_itineraryFilters)                                                                     |        `object`        | Configure itinerary filters that may modify itineraries, sort them, and filter away less preferable results.                                             | *Optional* |                  |  2.0  |
|    [accessibilityScore](#rd_if_accessibilityScore)                                                           |        `boolean`       | An experimental feature contributed by IBI which adds a sandbox accessibility *score* between 0 and 1 for each leg and itinerary.                        | *Optional* | `false`          |  2.2  |
|    [bikeRentalDistanceRatio](#rd_if_bikeRentalDistanceRatio)                                                 |        `double`        | Filter routes that consist of bike-rental and walking by the minimum fraction of the bike-rental leg using _distance_.                                   | *Optional* | `0.0`            |  2.1  |
|    [debug](#rd_if_debug)                                                                                     |         `enum`         | Enable this to attach a system notice to itineraries instead of removing them. This is very convenient when tuning the itinerary-filter-chain.           | *Optional* | `"off"`          |  2.0  |
|    [filterDirectFlexBySearchWindow](#rd_if_filterDirectFlexBySearchWindow)                                   |        `boolean`       | Filter direct flex results by the search window. The search-window is not used during flex routing, but we use one end to align it with transit results. | *Optional* | `true`           |  2.7  |
|    [filterItinerariesWithSameFirstOrLastTrip](#rd_if_filterItinerariesWithSameFirstOrLastTrip)               |        `boolean`       | If more than one itinerary begins or ends with same trip, filter out one of those itineraries so that only one remains.                                  | *Optional* | `false`          |  2.2  |
|    groupSimilarityKeepOne                                                                                    |        `double`        | Pick ONE itinerary from each group after putting itineraries that are 85% similar together.                                                              | *Optional* | `0.85`           |  2.1  |
|    groupSimilarityKeepThree                                                                                  |        `double`        | Reduce the number of itineraries to three itineraries by reducing each group of itineraries grouped by 68% similarity.                                   | *Optional* | `0.68`           |  2.1  |
|    [groupedOtherThanSameLegsMaxCostMultiplier](#rd_if_groupedOtherThanSameLegsMaxCostMultiplier)             |        `double`        | Filter grouped itineraries, where the non-grouped legs are more expensive than in the lowest cost one.                                                   | *Optional* | `2.0`            |  2.1  |
|    [minBikeParkingDistance](#rd_if_minBikeParkingDistance)                                                   |        `double`        | Filter out bike park+ride results that have fewer meters of cycling than this value.                                                                     | *Optional* | `0.0`            |  2.3  |
|    [nonTransitGeneralizedCostLimit](#rd_if_nonTransitGeneralizedCostLimit)                                   | `cost-linear-function` | The function define a max-limit for generalized-cost for non-transit itineraries.                                                                        | *Optional* | `"1h + 2.0 t"`   |  2.1  |
|    [parkAndRideDurationRatio](#rd_if_parkAndRideDurationRatio)                                               |        `double`        | Filter P+R routes that consist of driving and walking by the minimum fraction of the driving using of _time_.                                            | *Optional* | `0.0`            |  2.1  |
|    [removeItinerariesWithSameRoutesAndStops](#rd_if_removeItinerariesWithSameRoutesAndStops)                 |        `boolean`       | Set to true if you want to list only the first itinerary  which goes through the same stops and routes.                                                  | *Optional* | `false`          |  2.2  |
|    [removeTransitWithHigherCostThanBestOnStreetOnly](#rd_if_removeTransitWithHigherCostThanBestOnStreetOnly) | `cost-linear-function` | Limit function for generalized-cost computed from street-only itineries applied to transit itineraries.                                                  | *Optional* | `"1m + 1.30 t"`  |  2.4  |
|    [transitGeneralizedCostLimit](#rd_if_transitGeneralizedCostLimit)                                         |        `object`        | A relative limit for the generalized-cost for transit itineraries.                                                                                       | *Optional* |                  |  2.1  |
|       [costLimitFunction](#rd_if_transitGeneralizedCostLimit_costLimitFunction)                              | `cost-linear-function` | The base function used by the filter.                                                                                                                    | *Optional* | `"15m + 1.50 t"` |  2.2  |
|       [intervalRelaxFactor](#rd_if_transitGeneralizedCostLimit_intervalRelaxFactor)                          |        `double`        | How much the filter should be relaxed for itineraries that do not overlap in time.                                                                       | *Optional* | `0.4`            |  2.2  |
| [maxDirectStreetDurationForMode](#rd_maxDirectStreetDurationForMode)                                         | `enum map of duration` | Limit direct route duration per street mode.                                                                                                             | *Optional* |                  |  2.2  |
| scooter                                                                                                      |        `object`        | Scooter preferences.                                                                                                                                     | *Optional* |                  |  2.5  |
|    [optimization](#rd_scooter_optimization)                                                                  |         `enum`         | The set of characteristics that the user wants to optimize for.                                                                                          | *Optional* | `"safe-streets"` |  2.0  |
|    reluctance                                                                                                |        `double`        | A multiplier for how bad scooter travel is, compared to being in transit for equal lengths of time.                                                      | *Optional* | `2.0`            |  2.0  |
|    speed                                                                                                     |        `double`        | Max scooter speed along streets, in meters per second                                                                                                    | *Optional* | `5.0`            |  2.0  |
|    rental                                                                                                    |        `object`        | Vehicle rental options                                                                                                                                   | *Optional* |                  |  2.3  |
|       allowKeepingAtDestination                                                                              |        `boolean`       | If a vehicle should be allowed to be kept at the end of a station-based rental.                                                                          | *Optional* | `false`          |  2.2  |
|       dropOffCost                                                                                            |        `integer`       | Cost to drop-off a rented vehicle.                                                                                                                       | *Optional* | `30`             |  2.0  |
|       dropOffTime                                                                                            |       `duration`       | Time to drop-off a rented vehicle.                                                                                                                       | *Optional* | `"PT30S"`        |  2.0  |
|       keepingAtDestinationCost                                                                               |        `integer`       | The cost of arriving at the destination with the rented vehicle, to discourage doing so.                                                                 | *Optional* | `0`              |  2.2  |
|       pickupCost                                                                                             |        `integer`       | Cost to rent a vehicle.                                                                                                                                  | *Optional* | `120`            |  2.0  |
|       pickupTime                                                                                             |       `duration`       | Time to rent a vehicle.                                                                                                                                  | *Optional* | `"PT1M"`         |  2.0  |
|       useAvailabilityInformation                                                                             |        `boolean`       | Whether or not vehicle rental availability information will be used to plan vehicle rental trips.                                                        | *Optional* | `false`          |  2.0  |
|       [allowedNetworks](#rd_scooter_rental_allowedNetworks)                                                  |       `string[]`       | The vehicle rental networks which may be used. If empty all networks may be used.                                                                        | *Optional* |                  |  2.1  |
|       [bannedNetworks](#rd_scooter_rental_bannedNetworks)                                                    |       `string[]`       | The vehicle rental networks which may not be used. If empty, no networks are banned.                                                                     | *Optional* |                  |  2.1  |
|    [triangle](#rd_scooter_triangle)                                                                          |        `object`        | Triangle optimization criteria.                                                                                                                          | *Optional* |                  |  2.5  |
|       flatness                                                                                               |        `double`        | Relative importance of flat terrain (range 0-1).                                                                                                         | *Optional* | `0.0`            |  2.0  |
|       [safety](#rd_scooter_triangle_safety)                                                                  |        `double`        | Relative importance of safety (range 0-1).                                                                                                               | *Optional* | `0.0`            |  2.0  |
|       time                                                                                                   |        `double`        | Relative importance of duration of travel (range 0-1).                                                                                                   | *Optional* | `0.0`            |  2.0  |
| [transferOptimization](#rd_transferOptimization)                                                             |        `object`        | Optimize where a transfer between to trip happens.                                                                                                       | *Optional* |                  |  2.1  |
|    [backTravelWaitTimeFactor](#rd_to_backTravelWaitTimeFactor)                                               |        `double`        | To reduce back-travel we favor waiting, this reduces the cost of waiting.                                                                                | *Optional* | `1.0`            |  2.1  |
|    [extraStopBoardAlightCostsFactor](#rd_to_extraStopBoardAlightCostsFactor)                                 |        `double`        | Add an extra board- and alight-cost for prioritized stops.                                                                                               | *Optional* | `0.0`            |  2.1  |
|    [minSafeWaitTimeFactor](#rd_to_minSafeWaitTimeFactor)                                                     |        `double`        | Used to set a maximum wait-time cost, base on min-safe-transfer-time.                                                                                    | *Optional* | `5.0`            |  2.1  |
|    [optimizeTransferWaitTime](#rd_to_optimizeTransferWaitTime)                                               |        `boolean`       | This enables the transfer wait time optimization.                                                                                                        | *Optional* | `true`           |  2.1  |
| [transitGroupPriority](#rd_transitGroupPriority)                                                             |        `object`        | Group transit patterns and give each group a mutual advantage in the Raptor search.                                                                      | *Optional* |                  |  2.5  |
| [transitReluctanceForMode](#rd_transitReluctanceForMode)                                                     |  `enum map of double`  | Transit reluctance for a given transport mode                                                                                                            | *Optional* |                  |  2.1  |
| [unpreferred](#rd_unpreferred)                                                                               |        `object`        | Parameters listing authorities or lines that preferably should not be used in trip patters.                                                              | *Optional* |                  |  2.2  |
|    [agencies](#rd_unpreferred_agencies)                                                                      |   `feed-scoped-id[]`   | The ids of the agencies that incur an extra cost when being used. Format: `FeedId:AgencyId`                                                              | *Optional* |                  |  2.2  |
|    [routes](#rd_unpreferred_routes)                                                                          |   `feed-scoped-id[]`   | The ids of the routes that incur an extra cost when being used. Format: `FeedId:RouteId`                                                                 | *Optional* |                  |  2.2  |
| walk                                                                                                         |        `object`        | Walking preferences.                                                                                                                                     | *Optional* |                  |  2.5  |
|    boardCost                                                                                                 |        `integer`       | Prevents unnecessary transfers by adding a cost for boarding a vehicle. This is the cost that is used when boarding while walking.                       | *Optional* | `600`            |  2.0  |
|    [reluctance](#rd_walk_reluctance)                                                                         |        `double`        | A multiplier for how bad walking is, compared to being in transit for equal lengths of time.                                                             | *Optional* | `2.0`            |  2.0  |
|    [safetyFactor](#rd_walk_safetyFactor)                                                                     |        `double`        | Factor for how much the walk safety is considered in routing.                                                                                            | *Optional* | `1.0`            |  2.2  |
|    speed                                                                                                     |        `double`        | The user's walking speed in meters/second.                                                                                                               | *Optional* | `1.33`           |  2.0  |
|    stairsReluctance                                                                                          |        `double`        | Used instead of walkReluctance for stairs.                                                                                                               | *Optional* | `2.0`            |  2.0  |
|    [stairsTimeFactor](#rd_walk_stairsTimeFactor)                                                             |        `double`        | How much more time does it take to walk a flight of stairs compared to walking a similar horizontal length.                                              | *Optional* | `3.0`            |  2.1  |
|    escalator                                                                                                 |        `object`        | Escalator preferences.                                                                                                                                   | *Optional* |                  |  2.7  |
|       reluctance                                                                                             |        `double`        | A multiplier for how bad being in an escalator is compared to being in transit for equal lengths of time                                                 | *Optional* | `1.5`            |  2.4  |
|       [speed](#rd_walk_escalator_speed)                                                                      |        `double`        | How fast does an escalator move horizontally?                                                                                                            | *Optional* | `0.45`           |  2.7  |
| wheelchairAccessibility                                                                                      |        `object`        | See [Wheelchair Accessibility](Accessibility.md)                                                                                                         | *Optional* |                  |  2.2  |
|    enabled                                                                                                   |        `boolean`       | Enable wheelchair accessibility.                                                                                                                         | *Optional* | `false`          |  2.0  |
|    inaccessibleStreetReluctance                                                                              |        `double`        | The factor to multiply the cost of traversing a street edge that is not wheelchair-accessible.                                                           | *Optional* | `25.0`           |  2.2  |
|    [maxSlope](#rd_wheelchairAccessibility_maxSlope)                                                          |        `double`        | The maximum slope as a fraction of 1.                                                                                                                    | *Optional* | `0.083`          |  2.0  |
|    [slopeExceededReluctance](#rd_wheelchairAccessibility_slopeExceededReluctance)                            |        `double`        | How much streets with high slope should be avoided.                                                                                                      | *Optional* | `1.0`            |  2.2  |
|    [stairsReluctance](#rd_wheelchairAccessibility_stairsReluctance)                                          |        `double`        | How much stairs should be avoided.                                                                                                                       | *Optional* | `100.0`          |  2.2  |
|    elevator                                                                                                  |        `object`        | Configuration for when to use inaccessible elevators.                                                                                                    | *Optional* |                  |  2.2  |
|       inaccessibleCost                                                                                       |        `integer`       | The cost to add when traversing an entity which is know to be inaccessible.                                                                              | *Optional* | `3600`           |  2.2  |
|       onlyConsiderAccessible                                                                                 |        `boolean`       | Whether to only use this entity if it is explicitly marked as wheelchair accessible.                                                                     | *Optional* | `false`          |  2.2  |
|       unknownCost                                                                                            |        `integer`       | The cost to add when traversing an entity with unknown accessibility information.                                                                        | *Optional* | `20`             |  2.2  |
|    stop                                                                                                      |        `object`        | Configuration for when to use inaccessible stops.                                                                                                        | *Optional* |                  |  2.2  |
|       inaccessibleCost                                                                                       |        `integer`       | The cost to add when traversing an entity which is know to be inaccessible.                                                                              | *Optional* | `3600`           |  2.2  |
|       onlyConsiderAccessible                                                                                 |        `boolean`       | Whether to only use this entity if it is explicitly marked as wheelchair accessible.                                                                     | *Optional* | `true`           |  2.2  |
|       unknownCost                                                                                            |        `integer`       | The cost to add when traversing an entity with unknown accessibility information.                                                                        | *Optional* | `600`            |  2.2  |
|    trip                                                                                                      |        `object`        | Configuration for when to use inaccessible trips.                                                                                                        | *Optional* |                  |  2.2  |
|       inaccessibleCost                                                                                       |        `integer`       | The cost to add when traversing an entity which is know to be inaccessible.                                                                              | *Optional* | `3600`           |  2.2  |
|       onlyConsiderAccessible                                                                                 |        `boolean`       | Whether to only use this entity if it is explicitly marked as wheelchair accessible.                                                                     | *Optional* | `true`           |  2.2  |
|       unknownCost                                                                                            |        `integer`       | The cost to add when traversing an entity with unknown accessibility information.                                                                        | *Optional* | `600`            |  2.2  |

<!-- PARAMETERS-TABLE END -->


## Parameter Details

<!-- PARAMETERS-DETAILS BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

<h3 id="rd_alightSlack">alightSlack</h3>

**Since version:** `2.0` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT0S"`   
**Path:** /routingDefaults 

The time safety margin when alighting from a vehicle.

This time slack is added to arrival time of the vehicle before any transfer or onward travel.

This time slack helps model potential delays or procedures a passenger experiences during the process of passing through the alighting location. This
parameter is intended to be set by agencies not individual users. For specific modes, like airplane and
subway, that need more time than others, this is also configurable per mode with `alightSlackForMode`.
A related parameter (transferSlack) exists to help avoid missed connections when there are minor schedule variations.


<h3 id="rd_boardSlack">boardSlack</h3>

**Since version:** `2.0` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT0S"`   
**Path:** /routingDefaults 

The time safety margin when boarding a vehicle.

The board slack is added to the passenger's arrival time at a stop, before evaluating which
vehicles can be boarded.

This time slack helps model potential delays or procedures a passenger experiences during the process
of passing through the boarding location, as well as some minor schedule variation. This parameter is
intended to be set by agencies not individual users.

Agencies can use this parameter to ensure that the trip planner does not instruct passengers to arrive
at the last second. This slack is added at every boarding including the first vehicle and transfers
except for in-seat transfers and guaranteed transfers.

For specific modes, like airplane and subway, that need more time than others, this is also
configurable per mode with `boardSlackForMode`.

A related parameter (transferSlack) also helps avoid missed connections when there are minor schedule
variations.


<h3 id="rd_drivingDirection">drivingDirection</h3>

**Since version:** `2.2` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"right"`   
**Path:** /routingDefaults   
**Enum values:** `right` | `left`

The driving direction to use in the intersection traversal calculation

<h3 id="rd_intersectionTraversalModel">intersectionTraversalModel</h3>

**Since version:** `2.2` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"simple"`   
**Path:** /routingDefaults   
**Enum values:** `simple` | `constant`

The model that computes the costs of turns.

<h3 id="rd_maxDirectStreetDuration">maxDirectStreetDuration</h3>

**Since version:** `2.1` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT4H"`   
**Path:** /routingDefaults 

This is the maximum duration for a direct street search for each mode.

This is a performance limit and should therefore be set high. Results close to the limit are not
guaranteed to be optimal. Use itinerary-filters to limit what is presented to the client. The
duration can be set per mode(`maxDirectStreetDurationForMode`), because some street modes searches
are much more resource intensive than others. A default value is applied if the mode specific value
does not exist."


<h3 id="rd_maxJourneyDuration">maxJourneyDuration</h3>

**Since version:** `2.1` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT24H"`   
**Path:** /routingDefaults 

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


<h3 id="rd_otherThanPreferredRoutesPenalty">otherThanPreferredRoutesPenalty</h3>

**Since version:** `2.0` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `300`   
**Path:** /routingDefaults 

Penalty added for using every route that is not preferred if user set any route as preferred.

We return number of seconds that we are willing to wait for preferred route.

<h3 id="rd_relaxTransitGroupPriority">relaxTransitGroupPriority</h3>

**Since version:** `2.5` ∙ **Type:** `string` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"0s + 1.00 t"`   
**Path:** /routingDefaults 

The relax function for transit-group-priority

A path is considered optimal if the generalized-cost is less than the generalized-cost of
another path. If this parameter is set, the comparison is relaxed further if they belong
to different transit groups.


<h3 id="rd_relaxTransitSearchGeneralizedCostAtDestination">relaxTransitSearchGeneralizedCostAtDestination</h3>

**Since version:** `2.3` ∙ **Type:** `double` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults 

Whether non-optimal transit paths at the destination should be returned

Let c be the existing minimum pareto optimal generalized cost to beat. Then a trip
with cost c' is accepted if the following is true:
`c' < Math.round(c * relaxRaptorCostCriteria)`.

The parameter is optional. If not set a normal comparison is performed.

Values equals or less than zero is not allowed. Values greater than 2.0 are not
supported, due to performance reasons.


<h3 id="rd_searchWindow">searchWindow</h3>

**Since version:** `2.0` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults 

The duration of the search-window.

This is the time/duration in seconds from the earliest-departure-time(EDT) to the
latest-departure-time(LDT). In case of a reverse search it will be the time from earliest to
latest arrival time (LAT - EAT).

All optimal travels that depart within the search window is guaranteed to be found.

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


<h3 id="rd_streetRoutingTimeout">streetRoutingTimeout</h3>

**Since version:** `2.2` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT5S"`   
**Path:** /routingDefaults 

The maximum time a street routing request is allowed to take before returning the results.

The street search(AStar) aborts after this duration and any paths found are returned to the client.
The street part of the routing may take a long time if searching very long distances. You can set
the street routing timeout to avoid tying up server resources on pointless searches and ensure that
your users receive a timely response. You can also limit the max duration. There are is also a
'apiProcessingTimeout'. Make sure the street timeout is less than the 'apiProcessingTimeout'.


<h3 id="rd_transferPenalty">transferPenalty</h3>

**Since version:** `2.0` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0`   
**Path:** /routingDefaults 

An additional penalty added to boardings after the first.

The value is in OTP's internal weight units, which are roughly equivalent to seconds.
Set this to a high value to discourage transfers. Of course, transfers that save
significant time or walking will still be taken.


<h3 id="rd_transferSlack">transferSlack</h3>

**Since version:** `2.0` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT2M"`   
**Path:** /routingDefaults 

The extra time needed to make a safe transfer.

The extra buffer time/safety margin added to transfers to make sure the connection is safe, time
wise. We recommend allowing the end-user to set this, and use `board-/alight-slack` to enforce
agency policies. This time is in addition to how long it might take to walk, board and alight.

It is useful for passengers on long distance travel, and people with mobility issues, but can be set
close to zero for everyday commuters and short distance searches in high-frequency transit areas.


<h3 id="rd_unpreferredCost">unpreferredCost</h3>

**Since version:** `2.2` ∙ **Type:** `cost-linear-function` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"0s + 1.00 t"`   
**Path:** /routingDefaults 

A cost function used to calculate penalty for an unpreferred route.

Function should return number of seconds that we are willing to wait for preferred route
or for an unpreferred agency's departure. For example: `5m + 2.0 t`


<h3 id="rd_accessEgress_maxDuration">maxDuration</h3>

**Since version:** `2.1` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT45M"`   
**Path:** /routingDefaults/accessEgress 

This is the maximum duration for access/egress for street searches.

This is a performance limit and should therefore be set high. Results close to the limit are not
guaranteed to be optimal. Use itinerary-filters to limit what is presented to the client. The
duration can be set per mode(`maxDurationForMode`), because some street modes searches
are much more resource intensive than others. A default value is applied if the mode specific value
does not exist.


<h3 id="rd_accessEgress_maxStopCount">maxStopCount</h3>

**Since version:** `2.4` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `500`   
**Path:** /routingDefaults/accessEgress 

Maximal number of stops collected in access/egress routing

Safety limit to prevent access to and egress from too many stops.


<h3 id="rd_accessEgress_maxDurationForMode">maxDurationForMode</h3>

**Since version:** `2.1` ∙ **Type:** `enum map of duration` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/accessEgress   
**Enum keys:** `not-set` | `walk` | `bike` | `bike-to-park` | `bike-rental` | `scooter-rental` | `car` | `car-to-park` | `car-pickup` | `car-rental` | `car-hailing` | `flexible`

Limit access/egress per street mode.

Override the settings in `maxDuration` for specific street modes. This is
done because some street modes searches are much more resource intensive than others.


<h3 id="rd_accessEgress_maxStopCountForMode">maxStopCountForMode</h3>

**Since version:** `2.7` ∙ **Type:** `enum map of integer` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/accessEgress   
**Enum keys:** `not-set` | `walk` | `bike` | `bike-to-park` | `bike-rental` | `scooter-rental` | `car` | `car-to-park` | `car-pickup` | `car-rental` | `car-hailing` | `flexible`

Maximal number of stops collected in access/egress routing for the given mode

Safety limit to prevent access to and egress from too many stops.
Mode-specific version of `maxStopCount`.


<h3 id="rd_accessEgress_penalty">penalty</h3>

**Since version:** `2.4` ∙ **Type:** `enum map of object` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/accessEgress   
**Enum keys:** `not-set` | `walk` | `bike` | `bike-to-park` | `bike-rental` | `scooter-rental` | `car` | `car-to-park` | `car-pickup` | `car-rental` | `car-hailing` | `flexible`

Penalty for access/egress by street mode.

Use this to add a time and cost penalty to an access/egress legs for a given street
mode. This will favour other street-modes and transit. This has a performance penalty,
since the search-window is increased with the same amount as the maximum penalty for
the access legs used. In other cases where the access(CAR) is faster than transit the
performance will be better.

The default values are

- `car-to-park` = (timePenalty: 20m + 2.0 t, costFactor: 1.50)
- `car-pickup` = (timePenalty: 20m + 2.0 t, costFactor: 1.50)
- `car-rental` = (timePenalty: 20m + 2.0 t, costFactor: 1.50)
- `car-hailing` = (timePenalty: 20m + 2.0 t, costFactor: 1.50)
- `flexible` = (timePenalty: 10m + 1.30 t, costFactor: 1.30)

Example: `"car-to-park" : { "timePenalty": "10m + 1.5t", "costFactor": 2.5 }`

**Time penalty**

The `timePenalty` is used to add a penalty to the access/egress duration/time. The
time including the penalty is used in the algorithm when comparing paths, but the
actual duration is used when presented to the end user.

**Cost factor**

The `costFactor` is used to add an additional cost to the leg´s  generalized-cost. The
time-penalty is multiplied with the cost-factor. A cost-factor of zero, gives no
extra cost, while 1.0 will add the same amount to both time and cost.


<h3 id="rd_alightSlackForMode">alightSlackForMode</h3>

**Since version:** `2.0` ∙ **Type:** `enum map of duration` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults   
**Enum keys:** `rail` | `coach` | `subway` | `bus` | `tram` | `ferry` | `airplane` | `cable-car` | `gondola` | `funicular` | `trolleybus` | `monorail` | `carpool` | `taxi`

How much extra time should be given when alighting a vehicle for each given mode.

Sometimes there is a need to configure a longer alighting times for specific modes, such as airplanes or ferries.

<h3 id="rd_bicycle_boardCost">boardCost</h3>

**Since version:** `2.0` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `600`   
**Path:** /routingDefaults/bicycle 

Prevents unnecessary transfers by adding a cost for boarding a transit vehicle.

This is the cost that is used when boarding while cycling. This is usually higher that walkBoardCost.

<h3 id="rd_bicycle_optimization">optimization</h3>

**Since version:** `2.0` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"safe-streets"`   
**Path:** /routingDefaults/bicycle   
**Enum values:** `shortest-duration` | `safe-streets` | `flat-streets` | `safest-streets` | `triangle`

The set of characteristics that the user wants to optimize for.

If the triangle optimization is used, it's enough to just define the triangle parameters

<h3 id="rd_bicycle_parking_unpreferredVehicleParkingTagCost">unpreferredVehicleParkingTagCost</h3>

**Since version:** `2.3` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `300`   
**Path:** /routingDefaults/bicycle/parking 

What cost to add if a parking facility doesn't contain a preferred tag.

See `preferredVehicleParkingTags`.

<h3 id="rd_bicycle_parking_bannedVehicleParkingTags">bannedVehicleParkingTags</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/bicycle/parking 

Tags with which a vehicle parking will not be used. If empty, no tags are banned.

Vehicle parking tags can originate from different places depending on the origin of the parking(OSM or RT feed).


<h3 id="rd_bicycle_parking_preferredVehicleParkingTags">preferredVehicleParkingTags</h3>

**Since version:** `2.3` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/bicycle/parking 

Vehicle parking facilities that don't have one of these tags will receive an extra cost and will therefore be penalised.

Vehicle parking tags can originate from different places depending on the origin of the parking(OSM or RT feed).


<h3 id="rd_bicycle_parking_requiredVehicleParkingTags">requiredVehicleParkingTags</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/bicycle/parking 

Tags without which a vehicle parking will not be used. If empty, no tags are required.

Vehicle parking tags can originate from different places depending on the origin of the parking(OSM or RT feed).


<h3 id="rd_bicycle_rental_allowedNetworks">allowedNetworks</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/bicycle/rental 

The vehicle rental networks which may be used. If empty all networks may be used.

<h3 id="rd_bicycle_rental_bannedNetworks">bannedNetworks</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/bicycle/rental 

The vehicle rental networks which may not be used. If empty, no networks are banned.

<h3 id="rd_bicycle_triangle">triangle</h3>

**Since version:** `2.5` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/bicycle 

Triangle optimization criteria.

Optimization type doesn't need to be defined if these values are defined.

<h3 id="rd_bicycle_triangle_safety">safety</h3>

**Since version:** `2.0` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.0`   
**Path:** /routingDefaults/bicycle/triangle 

Relative importance of safety (range 0-1).

This factor can also include other concerns such as convenience and general cyclist
preferences by taking into account road surface etc.


<h3 id="rd_bicycle_walk_mountDismountCost">mountDismountCost</h3>

**Since version:** `2.0` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0`   
**Path:** /routingDefaults/bicycle/walk 

The cost of hopping on or off a vehicle.

There are different parameters for the cost of renting or parking a vehicle and this is
not meant for controlling the cost of those events.


<h3 id="rd_bicycle_walk_mountDismountTime">mountDismountTime</h3>

**Since version:** `2.0` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT0S"`   
**Path:** /routingDefaults/bicycle/walk 

The time it takes the user to hop on or off a vehicle.

Time it takes to rent or park a vehicle have their own parameters and this is not meant
for controlling the duration of those events.


<h3 id="rd_boardSlackForMode">boardSlackForMode</h3>

**Since version:** `2.0` ∙ **Type:** `enum map of duration` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults   
**Enum keys:** `rail` | `coach` | `subway` | `bus` | `tram` | `ferry` | `airplane` | `cable-car` | `gondola` | `funicular` | `trolleybus` | `monorail` | `carpool` | `taxi`

How much extra time should be given when boarding a vehicle for each given mode.

Sometimes there is a need to configure a board times for specific modes, such as airplanes or
ferries, where the check-in process needs to be done in good time before ride.


<h3 id="rd_car_boardCost">boardCost</h3>

**Since version:** `2.7` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `600`   
**Path:** /routingDefaults/car 

Prevents unnecessary transfers by adding a cost for boarding a transit vehicle.

This is the cost that is used when boarding while driving. This can be different compared to the boardCost while walking or cycling.

<h3 id="rd_car_parking_unpreferredVehicleParkingTagCost">unpreferredVehicleParkingTagCost</h3>

**Since version:** `2.3` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `300`   
**Path:** /routingDefaults/car/parking 

What cost to add if a parking facility doesn't contain a preferred tag.

See `preferredVehicleParkingTags`.

<h3 id="rd_car_parking_bannedVehicleParkingTags">bannedVehicleParkingTags</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/car/parking 

Tags with which a vehicle parking will not be used. If empty, no tags are banned.

Vehicle parking tags can originate from different places depending on the origin of the parking(OSM or RT feed).


<h3 id="rd_car_parking_preferredVehicleParkingTags">preferredVehicleParkingTags</h3>

**Since version:** `2.3` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/car/parking 

Vehicle parking facilities that don't have one of these tags will receive an extra cost and will therefore be penalised.

Vehicle parking tags can originate from different places depending on the origin of the parking(OSM or RT feed).


<h3 id="rd_car_parking_requiredVehicleParkingTags">requiredVehicleParkingTags</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/car/parking 

Tags without which a vehicle parking will not be used. If empty, no tags are required.

Vehicle parking tags can originate from different places depending on the origin of the parking(OSM or RT feed).


<h3 id="rd_car_rental_allowedNetworks">allowedNetworks</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/car/rental 

The vehicle rental networks which may be used. If empty all networks may be used.

<h3 id="rd_car_rental_bannedNetworks">bannedNetworks</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/car/rental 

The vehicle rental networks which may not be used. If empty, no networks are banned.

<h3 id="rd_itineraryFilters">itineraryFilters</h3>

**Since version:** `2.0` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults 

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


<h3 id="rd_if_accessibilityScore">accessibilityScore</h3>

**Since version:** `2.2` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /routingDefaults/itineraryFilters 

An experimental feature contributed by IBI which adds a sandbox accessibility *score* between 0 and 1 for each leg and itinerary.

This can be used by frontend developers to implement a simple traffic light UI.

<h3 id="rd_if_bikeRentalDistanceRatio">bikeRentalDistanceRatio</h3>

**Since version:** `2.1` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.0`   
**Path:** /routingDefaults/itineraryFilters 

Filter routes that consist of bike-rental and walking by the minimum fraction of the bike-rental leg using _distance_.

This filters out results that consist of a long walk plus a relatively short bike rental leg. A
value of `0.3` means that a minimum of 30% of the total distance must be spent on the bike in order
for the result to be included.


<h3 id="rd_if_debug">debug</h3>

**Since version:** `2.0` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"off"`   
**Path:** /routingDefaults/itineraryFilters   
**Enum values:** `off` | `list-all` | `limit-to-search-window` | `limit-to-num-of-itineraries`

Enable this to attach a system notice to itineraries instead of removing them. This is very
convenient when tuning the itinerary-filter-chain.

 - `off` By default, the debug itinerary filters is turned off.
 - `list-all` List all itineraries, including all deleted itineraries.
 - `limit-to-search-window` Return all itineraries, including deleted ones, inside the actual search-window used
   (the requested search-window may differ).
 - `limit-to-num-of-itineraries` Only return the requested number of itineraries, counting both actual and deleted ones.
   The top `numItineraries` using the request sort order is returned. This does not work
   with paging, itineraries after the limit, but inside the search-window are skipped when
   moving to the next page.


<h3 id="rd_if_filterDirectFlexBySearchWindow">filterDirectFlexBySearchWindow</h3>

**Since version:** `2.7` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `true`   
**Path:** /routingDefaults/itineraryFilters 

Filter direct flex results by the search window. The search-window is not used
during flex routing, but we use one end to align it with transit results.

When direct flex is mixed with a transit search in the same request, then the direct
flex results are filtered by the search window of the transit results.

Depart-at searches are filtered by latest-arrival-time and arrive-by searches are
filtered by earliest-departure-time.

Use this configuration to turn this feature off.


<h3 id="rd_if_filterItinerariesWithSameFirstOrLastTrip">filterItinerariesWithSameFirstOrLastTrip</h3>

**Since version:** `2.2` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /routingDefaults/itineraryFilters 

If more than one itinerary begins or ends with same trip, filter out one of those itineraries so that only one remains.

Trips are considered equal if they have same id and same service day. Non-transit legs are skipped
during comparison. Before filtering, trips are sorted by their generalized cost. The algorithm loops
through the list from top to bottom. If an itinerary matches from any other itinerary from above, it is
removed from list.


<h3 id="rd_if_groupedOtherThanSameLegsMaxCostMultiplier">groupedOtherThanSameLegsMaxCostMultiplier</h3>

**Since version:** `2.1` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `2.0`   
**Path:** /routingDefaults/itineraryFilters 

Filter grouped itineraries, where the non-grouped legs are more expensive than in the lowest cost one.

Of the itineraries grouped to maximum of three itineraries, how much worse can the non-grouped legs
be compared to the lowest cost. 2.0 means that they can be double the cost, and any itineraries
having a higher cost will be filtered.


<h3 id="rd_if_minBikeParkingDistance">minBikeParkingDistance</h3>

**Since version:** `2.3` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.0`   
**Path:** /routingDefaults/itineraryFilters 

Filter out bike park+ride results that have fewer meters of cycling than this value.

Useful if you want to exclude those routes which have only a few meters of cycling before parking the bike and taking public transport.

<h3 id="rd_if_nonTransitGeneralizedCostLimit">nonTransitGeneralizedCostLimit</h3>

**Since version:** `2.1` ∙ **Type:** `cost-linear-function` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"1h + 2.0 t"`   
**Path:** /routingDefaults/itineraryFilters 

The function define a max-limit for generalized-cost for non-transit itineraries.

The max-limit is applied to itineraries with *no transit legs*, however *all* itineraries
(including those with transit legs) are considered when calculating the minimum cost. The smallest
generalized-cost value is used as input to the function. The function is used to calculate a
*max-limit*. The max-limit is then used to filter *non-transit* itineraries by
*generalized-cost*. Itineraries with a cost higher than the max-limit are dropped from the result
set.

For example if the function is `f(x) = 30m + 2.0 x` and the smallest cost is `30m = 1800s`, then
all non-transit itineraries with a cost larger than `1800 + 2 * 5000 = 11 800` are dropped.


<h3 id="rd_if_parkAndRideDurationRatio">parkAndRideDurationRatio</h3>

**Since version:** `2.1` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.0`   
**Path:** /routingDefaults/itineraryFilters 

Filter P+R routes that consist of driving and walking by the minimum fraction of the driving using of _time_.

This filters out results that consist of driving plus a very long walk leg at the end. A value of
`0.3` means that a minimum of 30% of the total time must be spent in the car in order for the
result to be included. However, if there is only a single result, it is never filtered.


<h3 id="rd_if_removeItinerariesWithSameRoutesAndStops">removeItinerariesWithSameRoutesAndStops</h3>

**Since version:** `2.2` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /routingDefaults/itineraryFilters 

Set to true if you want to list only the first itinerary  which goes through the same stops and routes.

Itineraries visiting the same set of stops and riding the exact same routes, departing later are removed from the result.

<h3 id="rd_if_removeTransitWithHigherCostThanBestOnStreetOnly">removeTransitWithHigherCostThanBestOnStreetOnly</h3>

**Since version:** `2.4` ∙ **Type:** `cost-linear-function` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"1m + 1.30 t"`   
**Path:** /routingDefaults/itineraryFilters 

Limit function for generalized-cost computed from street-only itineries applied to transit itineraries.

The max-limit is applied to itineraries with transit *legs*, and only itineraries
without transit legs are considered when calculating the minimum cost. The smallest
generalized-cost value is used as input to the function. The function is used to calculate a
*max-limit*. The max-limit is then used to filter *transit* itineraries by
*generalized-cost*. Itineraries with a cost higher than the max-limit are dropped from the result
set. Walking is handled with a different logic: if a transit itinerary has higher cost than
a plain walk itinerary, it will be removed even if the cost limit function would keep it.


<h3 id="rd_if_transitGeneralizedCostLimit">transitGeneralizedCostLimit</h3>

**Since version:** `2.1` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/itineraryFilters 

A relative limit for the generalized-cost for transit itineraries.

The filter compares all itineraries against every other itinerary. If the generalized-cost plus a
`transitGeneralizedCostLimit` is higher than the other generalized-cost, then the itinerary is
dropped. The `transitGeneralizedCostLimit` is calculated using the `costLimitFunction` plus a
*relative cost* for the distance in time between the itineraries. The *relative cost* is the
`intervalRelaxFactor` multiplied with the interval in seconds. To set the `costLimitFunction` to be
_1 hour plus 2 times cost_ use: `3600 + 2.0 x`. To set an absolute value(3000s) use: `3000 + 0x`


<h3 id="rd_if_transitGeneralizedCostLimit_costLimitFunction">costLimitFunction</h3>

**Since version:** `2.2` ∙ **Type:** `cost-linear-function` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"15m + 1.50 t"`   
**Path:** /routingDefaults/itineraryFilters/transitGeneralizedCostLimit 

The base function used by the filter.

This function calculates the threshold for the filter, when the itineraries have exactly the same arrival and departure times.

<h3 id="rd_if_transitGeneralizedCostLimit_intervalRelaxFactor">intervalRelaxFactor</h3>

**Since version:** `2.2` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.4`   
**Path:** /routingDefaults/itineraryFilters/transitGeneralizedCostLimit 

How much the filter should be relaxed for itineraries that do not overlap in time.

This value is used to increase the filter threshold for itineraries further away in
time, compared to those, that have exactly the same arrival and departure times.

The unit is cost unit per second of time difference.

<h3 id="rd_maxDirectStreetDurationForMode">maxDirectStreetDurationForMode</h3>

**Since version:** `2.2` ∙ **Type:** `enum map of duration` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults   
**Enum keys:** `not-set` | `walk` | `bike` | `bike-to-park` | `bike-rental` | `scooter-rental` | `car` | `car-to-park` | `car-pickup` | `car-rental` | `car-hailing` | `flexible`

Limit direct route duration per street mode.

Override the settings in `maxDirectStreetDuration` for specific street modes. This is
done because some street modes searches are much more resource intensive than others.


<h3 id="rd_scooter_optimization">optimization</h3>

**Since version:** `2.0` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"safe-streets"`   
**Path:** /routingDefaults/scooter   
**Enum values:** `shortest-duration` | `safe-streets` | `flat-streets` | `safest-streets` | `triangle`

The set of characteristics that the user wants to optimize for.

If the triangle optimization is used, it's enough to just define the triangle parameters

<h3 id="rd_scooter_rental_allowedNetworks">allowedNetworks</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/scooter/rental 

The vehicle rental networks which may be used. If empty all networks may be used.

<h3 id="rd_scooter_rental_bannedNetworks">bannedNetworks</h3>

**Since version:** `2.1` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/scooter/rental 

The vehicle rental networks which may not be used. If empty, no networks are banned.

<h3 id="rd_scooter_triangle">triangle</h3>

**Since version:** `2.5` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/scooter 

Triangle optimization criteria.

Optimization type doesn't need to be defined if these values are defined.

<h3 id="rd_scooter_triangle_safety">safety</h3>

**Since version:** `2.0` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.0`   
**Path:** /routingDefaults/scooter/triangle 

Relative importance of safety (range 0-1).

This factor can also include other concerns such as convenience and general cyclist
preferences by taking into account road surface etc.


<h3 id="rd_transferOptimization">transferOptimization</h3>

**Since version:** `2.1` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults 

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


<h3 id="rd_to_backTravelWaitTimeFactor">backTravelWaitTimeFactor</h3>

**Since version:** `2.1` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `1.0`   
**Path:** /routingDefaults/transferOptimization 

To reduce back-travel we favor waiting, this reduces the cost of waiting.

The wait time is used to prevent *back-travel*, the `backTravelWaitTimeFactor` is multiplied with the wait-time and subtracted from the optimized-transfer-cost.

<h3 id="rd_to_extraStopBoardAlightCostsFactor">extraStopBoardAlightCostsFactor</h3>

**Since version:** `2.1` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.0`   
**Path:** /routingDefaults/transferOptimization 

Add an extra board- and alight-cost for prioritized stops.

A stopBoardAlightTransferCosts is added to the generalized-cost during routing. But this cost
cannot be too high, because that would add extra cost to the transfer, and favor other
alternative paths. But, when optimizing transfers, we do not have to take other paths
into consideration and can *boost* the stop-priority-cost to allow transfers to
take place at a preferred stop. The cost added during routing is already added to the
generalized-cost used as a base in the optimized transfer calculation. By setting this
parameter to 0, no extra cost is added, by setting it to `1.0` the stop-cost is
doubled. Stop priority is only supported by the NeTEx import, not GTFS.


<h3 id="rd_to_minSafeWaitTimeFactor">minSafeWaitTimeFactor</h3>

**Since version:** `2.1` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `5.0`   
**Path:** /routingDefaults/transferOptimization 

Used to set a maximum wait-time cost, base on min-safe-transfer-time.

This defines the maximum cost for the logarithmic function relative to the min-safe-transfer-time (t0) when wait time goes towards zero(0). f(0) = n * t0

<h3 id="rd_to_optimizeTransferWaitTime">optimizeTransferWaitTime</h3>

**Since version:** `2.1` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `true`   
**Path:** /routingDefaults/transferOptimization 

This enables the transfer wait time optimization.

If not enabled generalizedCost function is used to pick the optimal transfer point.

<h3 id="rd_transitGroupPriority">transitGroupPriority</h3>

**Since version:** `2.5` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults 

Group transit patterns and give each group a mutual advantage in the Raptor search.

Use this to separate transit patterns into groups. Each group will be given a group-id. A
path (multiple legs) will then have a set of group-ids based on the group-id from each leg.
Hence, two paths with a different set of group-ids will BOTH be optimal unless the cost is
worse than the relaxation specified in the `relaxTransitGroupPriority` parameter. This is
only available in the TransmodelAPI for now.

Unmatched patterns are put in the BASE priority-group.


**THIS IS STILL AN EXPERIMENTAL FEATURE - IT MAY CHANGE WITHOUT ANY NOTICE!**

<h3 id="rd_transitReluctanceForMode">transitReluctanceForMode</h3>

**Since version:** `2.1` ∙ **Type:** `enum map of double` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults   
**Enum keys:** `rail` | `coach` | `subway` | `bus` | `tram` | `ferry` | `airplane` | `cable-car` | `gondola` | `funicular` | `trolleybus` | `monorail` | `carpool` | `taxi`

Transit reluctance for a given transport mode

<h3 id="rd_unpreferred">unpreferred</h3>

**Since version:** `2.2` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults 

Parameters listing authorities or lines that preferably should not be used in trip patters.

A cost is applied to boarding nonpreferred authorities or routes.

The routing engine will add extra penalty - on the *unpreferred* routes and/or agencies using a
cost function. The cost function (`unpreferredCost`) is defined as a linear function of the form
`A + B x`, where `A` is a fixed cost (in seconds) and `B` is reluctance multiplier for transit leg
travel time `x` (in seconds).


<h3 id="rd_unpreferred_agencies">agencies</h3>

**Since version:** `2.2` ∙ **Type:** `feed-scoped-id[]` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/unpreferred 

The ids of the agencies that incur an extra cost when being used. Format: `FeedId:AgencyId`

How much cost is added is configured in `unpreferredCost`.

<h3 id="rd_unpreferred_routes">routes</h3>

**Since version:** `2.2` ∙ **Type:** `feed-scoped-id[]` ∙ **Cardinality:** `Optional`   
**Path:** /routingDefaults/unpreferred 

The ids of the routes that incur an extra cost when being used. Format: `FeedId:RouteId`

How much cost is added is configured in `unpreferredCost`.

<h3 id="rd_walk_reluctance">reluctance</h3>

**Since version:** `2.0` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `2.0`   
**Path:** /routingDefaults/walk 

A multiplier for how bad walking is, compared to being in transit for equal lengths of time.

Empirically, values between 2 and 4 seem to correspond well to the concept of not wanting to walk
too much without asking for totally ridiculous itineraries, but this observation should in no way
be taken as scientific or definitive. Your mileage may vary.
See https://github.com/opentripplanner/OpenTripPlanner/issues/4090 for impact on performance with
high values.


<h3 id="rd_walk_safetyFactor">safetyFactor</h3>

**Since version:** `2.2` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `1.0`   
**Path:** /routingDefaults/walk 

Factor for how much the walk safety is considered in routing.

Value should be between 0 and 1. If the value is set to be 0, safety is ignored.

<h3 id="rd_walk_stairsTimeFactor">stairsTimeFactor</h3>

**Since version:** `2.1` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `3.0`   
**Path:** /routingDefaults/walk 

How much more time does it take to walk a flight of stairs compared to walking a similar horizontal length.

Default value is based on: Fujiyama, T., & Tyler, N. (2010). Predicting the walking
speed of pedestrians on stairs. Transportation Planning and Technology, 33(2), 177–202.


<h3 id="rd_walk_escalator_speed">speed</h3>

**Since version:** `2.7` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.45`   
**Path:** /routingDefaults/walk/escalator 

How fast does an escalator move horizontally?

Horizontal speed of escalator in m/s.

<h3 id="rd_wheelchairAccessibility_maxSlope">maxSlope</h3>

**Since version:** `2.0` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.083`   
**Path:** /routingDefaults/wheelchairAccessibility 

The maximum slope as a fraction of 1.

9 percent would be `0.09`

<h3 id="rd_wheelchairAccessibility_slopeExceededReluctance">slopeExceededReluctance</h3>

**Since version:** `2.2` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `1.0`   
**Path:** /routingDefaults/wheelchairAccessibility 

How much streets with high slope should be avoided.

What factor should be given to street edges, which are over the
max slope. The penalty is not static but scales with how much you
exceed the maximum slope. Set to negative to disable routing on
too steep edges.


<h3 id="rd_wheelchairAccessibility_stairsReluctance">stairsReluctance</h3>

**Since version:** `2.2` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `100.0`   
**Path:** /routingDefaults/wheelchairAccessibility 

How much stairs should be avoided.

Stairs are not completely excluded for wheelchair users but
severely punished. This value determines how much they are
punished. This should be a very high value as you want to only
include stairs as a last result.


<!-- PARAMETERS-DETAILS END -->

## Config Example

<!-- JSON-EXAMPLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

```JSON
// router-config.json
{
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
  }
}
```

<!-- JSON-EXAMPLE END -->
