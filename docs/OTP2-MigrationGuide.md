# How to migrate OTP version 1.x to 2.x

## Build config

These properties changed names from:
 - `htmlAnnotations` to `dataImportReport`
 - `maxHtmlAnnotationsPerFile` to `maxDataImportIssuesPerFile`
 - `boardTimes` to `routingDefaults.boardSlackByMode`
 - `alightTimes` to `routingDefaults.alightSlackByMode`
 
 ## Command line
 TODO OTP2
 
 ## REST API
 
 A lot of the parameters in the REST API is ignored/deprecated look at the `RoutingRequest` class 
 for documentation.
 
 In OTP1 most client provided a way to page the results by looking at the trips returned and passing 
 in something like the `last-depature-time` + 1 minute to the next request, to get trips to add to 
 the already fetched results. In OTP2 the recommended way to do this is to use the new `TripPlan` 
 `metadata` returned by the rout call.
 
 ### RoutingRequest changes
 See JavaDoc on the RoutingRequest for full documentation of deprecated fields and doc on new fields. 
 Her is a short list of new filed:
 
 - `searchWindow` Limit the departure window or arrival window for the routing search.
 - `boardSlackByMode` How much time boarding a vehicle takes for each given mode.
 - `alightSlackByMode` How much time alighting a vehicle takes for each given mode.
  
 ### Response changes

 - `metadata` is added to `TripPlan`. The `TripSearchMetadata` has three fields:
    - `searchWindowUsed`
    - `nextDateTime`
    - `prevDateTime`

 