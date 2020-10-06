# How to migrate from OTP1 to OTP2

## Command line
The command line parameters are changed. Use the `--help` option to get the current documentation,
and look at the [Basic Tutorial, Start up OPT](Basic-Tutorial.md#starting-otp) for examples. The 
possibility to build the graph in 2 steps (streets then transit) is new in OTP2. OTP2 does not support multiple routers.


## File loading
OTP1 access all files using the local file system, no other _data-source_ is supported. In OTP2 we 
support accessing cloud storage. So far Google Cloud Storage is added and we plan to add support 
for AWS S3 as well. Config files(_otp-config.json, build-config.json, router-config.json_) are read 
from the local file system, while other files can be read/written from the local file-system or the
cloud. OTP2 support mixing any data sources that is supported. 

OTP1 loads input data files(_DEM, OSM, GTFS, NeTEx_) based on the suffix(file extension). But for 
GTFS files OTP1 also open the zip-file and look for _stops.txt_. OTP2 identify GTFS files by the 
name only. OTP2 will read any zip-file or directory that contains "gtfs" as part of the name. All 
files-types in OTP2 is resolved by matching the name with a regexp pattern. You can configure the 
patterns in the _build-config.json_ if the defaults does not suites you. 

OTP2 do not support for multiple routers, but you can load as many GTFS and/or NeTEx feeds as 
you want into a single instance of OTP2.


## Build config

These properties changed names from:

 - `htmlAnnotations` to `dataImportReport`
 - `maxHtmlAnnotationsPerFile` to `maxDataImportIssuesPerFile`
 - `boardTimes` to `routingDefaults.boardSlackByMode`
 - `alightTimes` to `routingDefaults.alightSlackByMode`
 
These parameters is no longer supported:

 - `stopClusterMode` - TODO OTP2 Why? Old options: `proximity`, `parentStation`

 
## Router config

 - All updaters that require data sources now require you to specify a `sourceType`, even if that
   particular updater only has one possible data source.
 
   
## REST API
  
### Trip Planning 

Support for XML as a request/response format is removed. The only supported format is JSON.

#### Query parameter changes

A lot of the query parameters in the REST API are ignored/deprecated, see the [RoutingRequest](https://github.com/opentripplanner/OpenTripPlanner/blob/2.0-rc/src/main/java/org/opentripplanner/routing/api/request/RoutingRequest.java) 
 and the [RoutingResource](https://github.com/opentripplanner/OpenTripPlanner/blob/2.0-rc/src/main/java/org/opentripplanner/api/common/RoutingResource.java)
 class for documentation on what is currently supported - we are adding features one by one. The 
 plan is to go over the API documentation before the release - we do not prioritize keeping 
 the documentation up to date, except for this migration guide document.
 
 The following parameters are missing in OTP2 but will be added:
- `startingTransitTripId` The ability to plan a trip from on board a vehicle should be implemented by Q1 2021.
- `intermediatePlaces` - ability to specify intermediate destinations along the route. It is not certain when this will be implemented. 
- `nonpreferredTransferCost`, `(un)preferredRoutes`, `(un)preferredAgencies` - these help diversify or customize the trips and operators 
visible in results. Due to the new transit routing algorithm, Entur plans to completely rewrite these features, 
accounting for market-neutrality requirements and showing relevant trips and operators in local vs. intercity trips.
 
 Some features in OTP1 will not be present upon launch in OTP2, and they are proposed to be removed permanently from OTP2 but may require some development to support valid important cases:
 * maxWalkDistance, maxTransferWalkDistance, & maxWait - walkReluctance and waitReluctance should be utilized. If implemented, they should be filters on top of Raptor search rather than embedded in search. But this shouldn't be necessary: if the trip is just beyond max distance/wait, rider should still see it if it is the only option.  
 * maxHours & useRequestedDateTimeInMaxHours - This is replaced by searchWindow, which limits the arrival or departure window of the trip 
 * worstTime - This factor returns the “worst” trip in a depart after/arrive by search, i.e. the latest or earliest trip available. It is not a priority for current OTP2 users but could be added as a filter. 
 * waitAtBeginningFactor - No longer necessary to weight the initial wait differently based on the the Range Raptor search algorithm, which no longer prefers a departure at one valid time over another. Filtering could be implemented on top of Raptor to show certain departure times before others. 
 * pathComparator - The ability to set a sort order based on departure or arrival should be the domain of the API rather than the search.  
 * startingTransitStopId - duplicative with fromPlace 
 * onlyTransitTrips - the new feature for specifying access, egress, transit and direct mode replace the need for this parameter. 
 

#### Paging
 In OTP1 most client provided a way to page the results by looking at the trips returned and passing 
 in something like the `last-depature-time` + 1 minute to the next request, to get trips to add to 
 the already fetched results. In OTP2 the recommended way to do this is to use the new `TripPlan` 
 `metadata` returned by the router call.

#### New/changed query parameters in the plan request
- `numItineraries` The parameter is no longer used to terminate the request when the numItineraries is found, instead the new `searchWindow` parameter should be used to limit the search. In OTP2 it crops the list of itineraries AFTER the search is complete. This parameter is a post search filter function. The best option is to configure this on the server side and not use it as a client side input parameter. A side effect from reducing the result is that OTP2 cannot guarantee to find all pareto-optimal itineraries when paging. Also, a large search-window and a small `numItineraries` waste computer CPU calculation time. Consider tuning the `searchWindow` instead of setting this to a small value.
 - `searchWindow` Limit the departure window or arrival window for the routing search.
 - `boardSlackByMode` How much time ride a vehicle takes for each given mode.
 - `alightSlackByMode` How much time alighting a vehicle takes for each given mode.
 - `modes` The REST API is unchanged, but is mapped into a new structure in the RoutingRequest. This means not all combinations of non-transit modes that was available in OTP1 is available in OTP2.
 - `preferredAgencies`, `unpreferredAgencies`, `bannedAgencies` and `whiteListedAgencies` use feed-
 scoped ids. If you are using the ids directly from the Index API, no changes are needed.
  
#### Response changes
- `metadata` is added to `TripPlan`. The `TripSearchMetadata` has three fields:
  - `searchWindowUsed`
  - `nextDateTime`
  - `prevDateTime`
- `agencyId` in the `leg` is now feed-scoped and similarly to other ids, is prefixed with `<FEED_ID>:`
- `debugOutput` in `TripPlan` has changed due to the different algorithms used in OTP version 1.x and 2.x.
  - The `totalTime` is left as is, `directStreetRouterTime`, `transitRouterTime`, `filteringTime` and `renderingTime` are new fields.
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
  is removed from OTP2. Clients may not be affected by this change, unless they toke advantage 
  of the semantics in the old `code`.
  - The `mode` field is added to `Route`, it should probebly replace the `type`(unchanged). The 
    `RouteShort` is not chencged - it has the `mode` field.
- `Pattern` (or `TripPattern`)  
  - The semantics of the `id` should NOT be used to access other related entities like `Route`, 
    the `routeId` is added to `TripPatternShort` to allow navigation to Route. 
- `Trip`
  - The deprecated `tripBikesAllowed` is removed.
  - The `routeId` replace `route`. The route is no longer part of the trip. To obtain the Route object call the Index API with the routeId.
- `Stop`
  - The new `stationId` is a feed-scoped-id to the parent station. It should be used instead of the
    deprecated ~~parentStation~~.
- `StopShort`
  - The new `stationId` is a feed-scoped-id to the parent station. It should be used instead of the
    deprecated ~~cluster~~.
- `Agency`
  - The `id` is now feed-scoped and similarly to other ids, is prefixed with `<FEED_ID>:`
- 'Alert'
  - `effectiveEndDate` is added to show the end time of the alert validity.

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
