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
 
 A lot of the parameters in the REST API is ignored/deprecated look at the `RoutingRequest` class 
 for documentation.
 
 In OTP1 most client provided a way to page the results by looking at the trips returned and passing 
 in something like the `last-depature-time` + 1 minute to the next request, to get trips to add to 
 the already fetched results. In OTP2 the recommended way to do this is to use the new `TripPlan` 
 `metadata` returned by the rout call.
 
 ### RoutingRequest changes
 See JavaDoc on the RoutingRequest for full documentation of deprecated fields and doc on new fields. 
 Her is a short list of new fields:
 
 - `searchWindow` Limit the departure window or arrival window for the routing search.
 - `boardSlackByMode` How much time boarding a vehicle takes for each given mode.
 - `alightSlackByMode` How much time alighting a vehicle takes for each given mode.
  
 ### Response changes

- `metadata` is added to `TripPlan`. The `TripSearchMetadata` has three fields:
  - `searchWindowUsed`
  - `nextDateTime`
  - `prevDateTime`

### Changes to the Index API
- Error handling is improved, this is now consistently applied and uses build in framework support. 
  - The HTTP 400 and 404 response now contains an error message in plain text in addition to the
    header "FOUR HUNDRED" and "FOUR ZERO FOUR".   
- `Route`
  - Deprecated 'routeBikesAllowed' field removed.
  - `sortOrder` will be empty (missing) when empty, NOT -999 as before.
- To access or references `TripPattern` the `tripPatternId` is used, not `code`. In OTP1 the
  `code` was used. The code was the same as the id without the feedId prefix. The `code`
  is removed from OTP2. Clients may not be affected by this change, unless they toke advantage 
  of the semantics in the old `code`.
- `Trip`
  - The deprecated `tripBikesAllowed` is removed.
  - The `routeId` replace `route`. The route is no longer part of the trip.
    To obtain the Route object call the Index API with the routeId.
