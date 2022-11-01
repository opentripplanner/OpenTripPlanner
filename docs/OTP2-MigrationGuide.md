# How to migrate from OTP1 to OTP2

## Command Line

The OTP2 command line parameters are different than in OTP1. Use the `--help` option to get the
current documentation, and look at [Basic Tutorial - Starting OTP](Basic-Tutorial.md#starting-otp)
for examples. The possibility to build the graph in 2 steps (streets then transit) is new in OTP2.
OTP2 does not support routing on more than one separate transportation network with a single
server (referred to as multiple "routers" in OTP1).

## File Loading

OTP1 reads and writes all files on the local filesystem, and no other _data-source_ is supported. In
OTP2 we support accessing cloud storage. So far support for Google Cloud Storage has been added and
we plan to add support for AWS S3 as well. Config files (_otp-config.json, build-config.json,
router-config.json_) must be read from the local file system, while other files can be read/written
from either the local filesystem or cloud storage. OTP2 supports mixing any supported data sources.

OTP1 loads input data files (_DEM, OSM, GTFS, NeTEx_) based on the suffix (file extension). But for
GTFS files OTP1 also opens the zip-file and looks for _stops.txt_. OTP2 identifies GTFS files by the
name only: it will detect any zip-file or directory that contains "gtfs" as part of the name. All
file types in OTP2 are resolved by matching the name with a regexp pattern. You can configure the
patterns in the _build-config.json_ if the defaults do not suit you.

OTP2 does not support multiple routers (separate named networks to route on), but you can load as
many GTFS and/or NeTEx feeds as you want into a single routable network in a single instance of
OTP2.

## Build Config

OTP will log all unrecognized parameters when starting up. Make sure to investigate all log events
of this type:

```
16:18:46.911 WARN (NodeAdapter.java:413) Unexpected config parameter: 'fetchElevationUS:false' in 'build-config.json'. Is the spelling correct?
```

#### New parameters

- `configVersion` Optional parameter which can be used to version the build config file. Since v2.1
- `dataOverlay` Config for the DataOverlay Sandbox module. Since v2.1
- `maxAreaNodes` Visibility calculations will not be done for areas with more nodes than this limit.
  Since v2.1
- `maxJourneyDuration` This limits the patterns we consider in the transit search.
  See [RoutingRequest](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.1/src/main/java/org/opentripplanner/routing/api/request/RoutingRequest.java)
  . Since v2.1
- `maxStopToShapeSnapDistance` Used for mapping route geometry shapes. Since v2.1
- `transferRequests` Pre-calculate transfers. Since v2.1
- `transitServiceStart` Limit the import of transit services to the given *start* date.
  Default: `-P1Y`. Since v2.0
- `transitServiceEnd` Limit the import of transit services to the given *end* date. *Inclusive*.
  Default: `P3Y`. Since v2.0
- Since v2.2, data feeds can be configured individually by using the `transitFeeds`, `osm` and `dem` 
  nodes.

#### Parameters whose names were changed

- `alightTimes` to `routingDefaults.alightSlackForMode`. Since v2.0
- `boardTimes` to `routingDefaults.boardSlackForMode`. Since v2.0
- `htmlAnnotations` to `dataImportReport`. Since v2.0
- `maxHtmlAnnotationsPerFile` to `maxDataImportIssuesPerFile`. Since v2.0
- `maxTransferDistance` to `maxTransferDurationSeconds`. Since v2.1

#### These parameters are no longer supported

- `fetchElevationUS`. Since v2.1
- `parentStopLinking`. Since v2.0
- `staticBikeRental`. Since v2.1
- `stationTransfers`. Since v2.0
- `stopClusterMode`. Since v2.0
- `useTransfersTxt`. Since v2.1


Since v2.2, `osmWayPropertySet` was renamed `osmTagMapping` and is part of the individual osm 
source. The driving direction and intersection cost model were decoupled for the tag mapping and can
be configured using `drivingDirection` and `intersectionTraversalModel` inside `routingDefaults`.

OTP2 records the "parentStation" relationship between stops and stations in its internal transit
model, based on the GTFS and/or NeTEx input. This enables OTP to search from all stop in a station
_without_ walking/waiting when the request from/to input field is a station id. There is no way to
automatically infer this parent station relationship based on geographic proximity in OTP2.

Transfers in OTP2 are generated based on the stop location and the OSM data or GTFS Pathways. In
future versions of OTP2 we also want to support generating simple transfers based on
"line-of-sight" if no pathways or OSM data exist. See issue
[#3204](https://github.com/opentripplanner/OpenTripPlanner/issues/3204).

Cleaning and patching input data is NOT a core feature of OTP, but anyone is welcome to implement a
sandbox plugin to patch data. So, if any of the features above are needed they can be ported from
OTP1 into an OTP2 sandbox feature.

## Router Config

See the [Router Configuration](RouterConfiguration) for a description of the new and existing
routing parameters.

#### New parameters

- `flex` Add configuration for flex services (sandbox feature). Since v2.1
- `configVersion` Optional parameter which can be used to version the build config file. Since v2.1
- `streetRoutingTimeout` Maximum time limit for street route queries. Replace the old `timeout`.
  Since v2.0
- `transit` A set of parameters to tune the Raptor transit router. Since v2.0, changed in v2.1
- `itineraryFilters` Configure itinerary filters that may modify itineraries, sort them, and filter
  away less preferable results. Since v2.0, changed in v2.1
- `transferOptimization` Configure the new transfer optimization feature. Since 2.1

#### These parameters are no longer supported

- `timeout` Replaced by `streetRoutingTimeout`. Since v2.0
- `timeouts` OTP1 searches the graph many times. OTP2 finds multiple results in a single search so
  there is no longer a need for this parameter. Since v2.0
- `boardTimes` is replaced by `request` parameter `boardSlack` and `boardSlackForMode`. Since v2.0
- `alightTimes` is replaced by `request` parameter `alightSlack` and `alightSlackForMode`. Since
  v2.0
- `disableAlertFiltering` Not implemented in OTP2. Since v2.0

## REST API

### Trip Planning

Support for XML as a request/response format is removed. The only supported format is JSON. Some of
these parameters may only be available as `defaultRequest` configuration parameters.

#### Query parameter changes

A lot of the query parameters in the REST API are ignored/deprecated, see
the [RoutingRequest](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.1/src/main/java/org/opentripplanner/routing/api/request/RouteRequest.java)
and
the [RoutingResource](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.1/src/main/java/org/opentripplanner/api/common/RouteResource.java)
class for the documentation on what is now supported in OTP2.

#### Parameters missing in OTP2 but intended to be reintroduced

- `startingTransitTripId` - ability to plan a trip from on board a vehicle
- `intermediatePlaces` - ability to specify intermediate destinations along the route. It is not
  certain when this will be implemented.
- `nonpreferredTransferCost`, `(un)preferredRoutes`, `(un)preferredAgencies` - these help diversify
  or customize the trips and operators visible in results. Due to the new transit routing algorithm,
  Entur plans to completely rewrite these features, accounting for market-neutrality requirements
  and showing relevant trips and operators in local vs. intercity trips.

Some features in OTP1 will not be present upon launch in OTP2, and they are proposed to be removed
permanently from OTP2, but may require some development to support valid important cases:

- `maxWalkDistance`, `maxTransferWalkDistance`, & `maxWait` - these parameters impose hard limits
  and are no longer the preferred way to reduce the amount of walking or waiting in returned
  itineraries. In OTP2 the goal is to control this with `walkReluctance` and `waitReluctance`.
  Internally some limits on walking and waiting do still exist, but they are set quite high so trips
  with long walking or waiting times are still considered. Note that unlike in OTP1, if you do set
  your own max walk or wait time on an API request, it will apply to both transit searches and
  non-transit searches.
- `maxHours` & `useRequestedDateTimeInMaxHours` - This is replaced by `searchWindow`, which limits
  the arrival or departure window of the trip
- `worstTime` - This factor returns the “worst” trip in a depart after/arrive by search, i.e. the
  latest or earliest trip available. It is not a priority for current OTP2 users but could be added
  as a filter.
- `waitAtBeginningFactor` - No longer necessary to weight the initial wait differently based on the
  the Range Raptor search algorithm, which no longer prefers a departure at one valid time over
  another. Filtering could be implemented on top of Raptor to show certain departure times before
  others. Removed in v2.2.
- `pathComparator` - The ability to set a sort order based on departure or arrival should be the
  domain of the API rather than the search.
- `startingTransitStopId` - this is redundant, as the same thing can be achieved with fromPlace
- `onlyTransitTrips` - it is now possible to specify access, egress, transit and direct modes
  separately, making this parameter unnecessary.

#### Parameters that have changed

- `numItineraries` The parameter is no longer used to terminate the request when the numItineraries
  is found, instead the new `searchWindow` parameter should be used to limit the search. In OTP2 it
  crops the list of itineraries AFTER the search is complete. This parameter is a post search filter
  function. The best option is to configure this on the server side and not use it as a client side
  input parameter. A side effect from reducing the result is that OTP2 cannot guarantee to find all
  pareto-optimal itineraries when paging. Also, a large search-window and a small `numItineraries`
  waste computer CPU calculation time. Consider tuning the `searchWindow` instead of setting this to
  a small value. Since 2.0
- `modes` The REST API is unchanged, but is mapped into a new structure in the RouteRequest. This
  means not all combinations of non-transit modes that was available in OTP1 is available in OTP2.
  Since 2.0
- `preferredAgencies`, `unpreferredAgencies`, `bannedAgencies` and `whiteListedAgencies` use
  feed-scoped ids. If you are using the ids directly from the Index API, no changes are needed.
  Since 2.0
- `maxTransferDistance`, replaced by `maxTransferDurationSeconds` Since 2.1
- `bannedTrips` no longer allows specifying stop indices, but only allows banning complete trips.
  Since 2.2

#### New parameters in OTP2

- `alightSlackForMode` How much time alighting a vehicle takes for each given mode. Since 2.0
- `allowedVehicleRentalNetworks` and `bannedVehicleRentalNetworks`. Since 2.1
- `bikeReluctance`, `bikeWalkingReluctance`, `bikeWalkingSpeed`, `carReluctance`, and `walkingBike`
  Add explicit bike / bike-walking / car / walk reluctance. Since 2.1
- `boardSlackForMode` How much time ride a vehicle takes for each given mode. Since 2.0
- `carPickupCost` and `carPickupTime`. Add a cost/time for CarPickup changes when a pickup or drop
  off takes place. Since 2.1
- `maxAccessEgressDurationForMode` Limit access/egress per street mode. Since 2.2
  - This was called `maxAccessEgressDurationSecondsForMode` between 2.0 and 2.2
- `maxDirectStreetDurationForMode` Limit direct route duration per street mode. Since 2.2
- `parkAndRideDurationRatio` Filter for park and ride with long walk. Since 2.1
- `requiredVehicleParkingTags` and `bannedVehicleParkingTags`. Since 2.1
- `searchWindow` Limit the departure window or arrival window for the routing search. Since 2.0
- `stairsTimeFactor` Add a penalty to the time it takes to walk up and down stairs. Since 2.1

#### These parameters are no longer supported

- `maxHours` Since 2.1
- `maxPreTransitTime` Since 2.1
- `maxWeight` Since 2.1
- `driveOnRight` You can specify the driving direction in your way property set. Since 2.1
- `bannedTrips` Not supported in 2.0 and 2.1
- `waitAtBeginningFactor` Since 2.2 

#### Paging

In OTP1 most clients provided a way to break results into pages by looking at the trips returned and
issuing another request, supplying something like the `last-depature-time` + 1 minute to the next
request. This yields another batch of trips to show to the user. In OTP2 the recommended way to do
this is to use the new `TripPlan metadata` returned by the router call.

In OTP 2.0 the server returned a set of parameters(`searchWindowUsed`, `nextDateTime`,
and `prevDateTime`), but in OTP 2.1 we have switched to a token-based approach to paging. In the
response there is a next/previous cursor. Duplicate the request and set the new `pageCursor` to go
the next/previous page.

#### Response changes

- `agencyId` in the `leg` is now feed-scoped and similarly to other ids, is prefixed
  with `<FEED_ID>:`
- `debugOutput` in `TripPlan` has changed due to the different algorithms used in OTP version 1.x
  and 2.x.
- The `totalTime` is left as is, `directStreetRouterTime`, `transitRouterTime`, `filteringTime`
  and `renderingTime` are new fields.
- `effectiveEndDate` is added to the `Alert`s

### Changes to the Index API

- Error handling is improved, this is now consistently applied and uses build in framework support.
- The HTTP 400 and 404 response now contains a detailed error message in plain text targeted
  developers to help understanding why the 400 or 404 was returned.
- `Route`
- Deprecated 'routeBikesAllowed' field removed.
- `sortOrder` will be empty (missing) when empty, NOT -999 as before.
- To access or references `TripPattern` use `tripPatternId`, not `code`. In OTP1 the
  `code` was used. The code was the same as the id without the feedId prefix. The `code`
  is removed from OTP2. Clients may not be affected by this change, unless they toke advantage of
  the semantics in the old `code`.
- The `mode` field is added to `Route`, it should probebly replace the `type`(unchanged). The
  `RouteShort` is not chencged - it has the `mode` field.
- `Pattern` (or `TripPattern`)
    - The semantics of the `id` should NOT be used to access other related entities like `Route`,
      the `routeId` is added to `TripPatternShort` to allow navigation to Route.
- `Trip`
    - The deprecated `tripBikesAllowed` is removed.
    - The `routeId` replace `route`. The route is no longer part of the trip. To obtain the Route
      object call the Index API with the routeId.
- `Stop`
    - The new `stationId` is a feed-scoped-id to the parent station. It should be used instead of
      the deprecated ~~parentStation~~.
- `StopShort`
    - The new `stationId` is a feed-scoped-id to the parent station. It should be used instead of
      the deprecated ~~cluster~~.
- `Agency`
    - The `id` is now feed-scoped and similarly to other ids, is prefixed with `<FEED_ID>:`
- `Alert`
    - `effectiveEndDate` is added to show the end time of the alert validity.

### ServerInfo

The returned data structure is changed and more info is available.

### AlertPatcher

The AlertPatcher, which was under the `/patch` path, is removed. In order to update alerts, please
use a GTFS-RT Service Alert updater instead. An example of a simple service for producing static
GTFS-RT Service Alert feed from JSON is [manual-gtfsrt](https://github.com/pailakka/manual-gtfsrt).

Querying for alerts has been moved under the index API, where `/alerts` can be appended to stop,
route, trip and pattern.

### Analyst

The analyst API endpoints have been removed.

### Scripting

The scripting API endpoint has been removed.

### Updaters

- Floating bikes have been disabled by default in the GbfsBikeRentalDataSource unless explicitly
  turned on via OTPFeature.
- Allow http headers to be configured for bike rental updaters
- The following bike updaters have been removed: *b-cycle*, *bicimad*, *bixi*, *city-bikes*, and *
  citi-bike-nyc*, *jcdecaux*, *keolis-rennes*, *kml*, *next-bike*, *ov-fiets*, *sf-bay-area*, *
  share-bike*, *smoove*, *uip-bike*, and *vcub*. Use the standard *gtfs* updater instead, or
  reintroduce your custom updater as a Sandbox module.
- The `logFrequency`, `maxSnapshotFrequency`, `purgeExpiredData` updater parameters are moved from 
  the individual updaters to `timetableUpdates`(root level in the router config).  

