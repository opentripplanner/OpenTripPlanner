# How to migrate OTP version 1.x to 2.x

## Build config

These properties changed names from:
 - `htmlAnnotations` to `dataImportReport`
 - `maxHtmlAnnotationsPerFile` to `maxDataImportIssuesPerFile`
 - `boardTimes` to `routingDefaults.boardSlackByMode`
 - `alightTimes` to `routingDefaults.alightSlackByMode`
 
## Command line
 The command line parameters are changed. Use the `--help` option to get the current documentation,
  and look at the [Basic Tutorial, Start up OPT](Basic-Tutorial.md#start-up-otp) for examples. The 
  possibility to build the graph in 2 steps is new in OTP2.  
   
## REST API
  
### Trip Planning 

Support for XML as a request/response format is removed. The only supported format is JSON.

#### Query parameter changes

A lot of the query parameters in the REST API are ignored/deprecated, see the [RoutingRequest](https://github.com/opentripplanner/OpenTripPlanner/tree/dev-2.x/src/main/java/org/opentripplanner/routing/core/RoutingRequest.java) 
 and the [RoutingResource](https://github.com/opentripplanner/OpenTripPlanner/tree/dev-2.x/src/main/java/org/opentripplanner/api/common/RoutingResource.java)
 class for documentation on what is currently supported - we are adding features one by one. The 
 plan is to go over the API documentation before the release - we do not prioritize keeping 
 the documentation up to date, except for this migration guide document.
  
#### Paging
 In OTP1 most client provided a way to page the results by looking at the trips returned and passing 
 in something like the `last-depature-time` + 1 minute to the next request, to get trips to add to 
 the already fetched results. In OTP2 the recommended way to do this is to use the new `TripPlan` 
 `metadata` returned by the router call.

#### New query parameters in the plan request
 - `searchWindow` Limit the departure window or arrival window for the routing search.
 - `boardSlackByMode` How much time boarding a vehicle takes for each given mode.
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
