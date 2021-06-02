# Changelog

## 2.1.0 (in progress)
- Fix NullPointerException when a RealTime update do not match an existing TripPattern [#3284](https://github.com/opentripplanner/OpenTripPlanner/issues/3284)
- Support for versioning the configuration files [#3282](https://github.com/opentripplanner/OpenTripPlanner/issues/3282)
- Support for versioning the configuration files [#3282](https://github.com/opentripplanner/OpenTripPlanner/issues/3282)
- Prioritize "direct" routes over transfers in group-filters [#3309](https://github.com/opentripplanner/OpenTripPlanner/issues/3309)
- The itinerary filter configuration is moved from the `RoutingRequest` into its own JSON node `itineraryFilters`.  
- Remove poor transit results for short trips, when walking is better [#3331](https://github.com/opentripplanner/OpenTripPlanner/issues/3331)
- A pathway's `traversal_time` is used when calculating the duration of transfers [#3357](https://github.com/opentripplanner/OpenTripPlanner/issues/3357).
- GTFS Trips will by default not allow bikes if no explicit value is set [#3359](https://github.com/opentripplanner/OpenTripPlanner/issues/3359).
- Improve the dynamic search window calculation. The configuration parameters `minTransitTimeCoefficient` and `minWaitTimeCoefficient` replace the old `minTripTimeCoefficient` parameter. [#3366](https://github.com/opentripplanner/OpenTripPlanner/issues/3366)   
- Allow loops caused by turn restriction in street routes [#3399](https://github.com/opentripplanner/OpenTripPlanner/pull/3399)
- Apply turn restrictions when splitting edges. [#3414](https://github.com/opentripplanner/OpenTripPlanner/pull/3414)
- Add separate no-thru handling for bicycles [#3410](https://github.com/opentripplanner/OpenTripPlanner/pull/3410)
- Add German way property set [#3359](https://github.com/opentripplanner/OpenTripPlanner/pull/3405)
- Process OSM bicycle routes correctly [#3359](https://github.com/opentripplanner/OpenTripPlanner/pull/3405)
- Avoid turns across traffic on bicycles [#3359](https://github.com/opentripplanner/OpenTripPlanner/pull/3405)
- Remove request parameter `driveOnRight` and derive information from way property set [#3359](https://github.com/opentripplanner/OpenTripPlanner/pull/3405)
- Add basic support for routing using floating bikes [#3370](https://github.com/opentripplanner/OpenTripPlanner/pull/3370)
- Add no thru traffic debug layer [#3443](https://github.com/opentripplanner/OpenTripPlanner/issues/3443)
- Optimize witch stops are used for transfers, using generalized-cost, wait-time and transfer priority. Partially implements [#2788](https://github.com/opentripplanner/OpenTripPlanner/issues/2788)
- Support for stay-seated and guaranteed transfers [#3193](https://github.com/opentripplanner/OpenTripPlanner/issues/3193)
- Fix reading of cached elevation files [#3455](https://github.com/opentripplanner/OpenTripPlanner/pull/3455)
- Added BikeRentalWithMostlyWalking filter [#3446](https://github.com/opentripplanner/OpenTripPlanner/pull/3446)
- Import GTFS-Flex v2 Flexible trips [#3453](https://github.com/opentripplanner/OpenTripPlanner/pull/3453)
- Add support for arriving at the destination with rented bicycles [#3459](https://github.com/opentripplanner/OpenTripPlanner/issues/3459)
- Allow IntersectionTraversalCostModel to be specified in the WayPropertySet [#3472](https://github.com/opentripplanner/OpenTripPlanner/pull/3472)
- ClassCastException when doing flex access search [#3448](https://github.com/opentripplanner/OpenTripPlanner/issues/3448)
- Fix for traveling back in time when optimize transfers [#3491](https://github.com/opentripplanner/OpenTripPlanner/pull/3491)
- Transit reluctance per transit mode [#3440](https://github.com/opentripplanner/OpenTripPlanner/issues/3440)
- Allow the removal of P+R results consisting only of driving of walking [#3515](https://github.com/opentripplanner/OpenTripPlanner/pull/3515)

## 2.0.0 (2020-11-27)

See the [OTP2 Migration Guide](OTP2-MigrationGuide.md) on changes to the REST API.

- Sandbox for experimental features [#2745](https://github.com/opentripplanner/OpenTripPlanner/issues/2745)
- Bugfix for Missing platforms for stops in GTFS import causes a NPE [#2804](https://github.com/opentripplanner/OpenTripPlanner/issues/2804)
- Remove extra Djikstra implementations
- Remove redundant LineStrings in order to save memory [#2795](https://github.com/opentripplanner/OpenTripPlanner/issues/2795)
- NeTEx import support [#2769](https://github.com/opentripplanner/OpenTripPlanner/issues/2769)
- New Transit search algorithm, Raptor, replaces the AStar for all transit searches. 
- Added NeTEx notices [#2824](https://github.com/opentripplanner/OpenTripPlanner/issues/2824)
- Make transfers and access/egress use effectiveWalkDistance to take slopes into account [#2857](https://github.com/opentripplanner/OpenTripPlanner/issues/2857)
- Add MultiModalStation and GroupOfStations to OTP model and added these to the NeTEx import [#2813](https://github.com/opentripplanner/OpenTripPlanner/issues/2813)
- Combined OSM loaders, removing several rarely used ones [#2878](https://github.com/opentripplanner/OpenTripPlanner/issues/2878)
- New Java Code Style (part of [#2755](https://github.com/opentripplanner/OpenTripPlanner/issues/2755))
- Cleanup and rename Graph Builder Annotations, now Data Import Issues [#2871](https://github.com/opentripplanner/OpenTripPlanner/issues/2871)
- Bugfix for graph building crashing on unsupported modes [#2899](https://github.com/opentripplanner/OpenTripPlanner/issues/2899)
- Add command line parameter for building partial graphs [#2583](https://github.com/opentripplanner/OpenTripPlanner/issues/2583)
- Refactor GenericLocation/AStar/RoutingContext to allow multiple start vertices [#2887](https://github.com/opentripplanner/OpenTripPlanner/issues/2887) 
- New Transit search algorithm, Raptor, replaces the AStar for all transit searches. 
- Update only the relevant parts of the TransitLayer each time an update is applied [#2918](https://github.com/opentripplanner/OpenTripPlanner/issues/2918)
- Ability to switch off the fare service[#2912](https://github.com/opentripplanner/OpenTripPlanner/issues/2912).
- Limit the transit service period[#2925](https://github.com/opentripplanner/OpenTripPlanner/issues/2925).
- Removed unwanted cost added for wait time between access and transit with RangeRaptor [#2927](https://github.com/opentripplanner/OpenTripPlanner/issues/2927)
- Dynamic search parameters, calculate raptor search-window when needed. [#2931](https://github.com/opentripplanner/OpenTripPlanner/issues/2931)
- Support for next/previous paging trip search results [#2941](https://github.com/opentripplanner/OpenTripPlanner/issues/2941)
- Fix mismatch in duration for walk legs, resulting in negative wait times [#2955](https://github.com/opentripplanner/OpenTripPlanner/issues/2955)
- NeTEx import now supports ServiceLinks [#2951](https://github.com/opentripplanner/OpenTripPlanner/issues/2951)
- Also check TripPatterns added by realtime when showing stoptimes for stop [#2954](https://github.com/opentripplanner/OpenTripPlanner/issues/2954)
- Copy geometries from previous TripPattern when realtime updates result in a TripPattern being replaced [#2987](https://github.com/opentripplanner/OpenTripPlanner/issues/2987)
- Support for the Norwegian language.
- Update pathways support to official GTFS specification [#2923](https://github.com/opentripplanner/OpenTripPlanner/issues/2923)
- Support for XML (de-)serialization is REMOVED from the REST API [#3031](https://github.com/opentripplanner/OpenTripPlanner/issues/3031)
- Refactor how to specify access/egress/direct/transit modes in the internal model and the Transmodel API [#3011](https://github.com/opentripplanner/OpenTripPlanner/issues/3011)
- Make agency id feed scoped [#3035](https://github.com/opentripplanner/OpenTripPlanner/issues/3035)
- Refactor kiss and ride to a more general car pickup mode [#3063](https://github.com/opentripplanner/OpenTripPlanner/issues/3063)
- Map NeTEx publicCode to OTP tripShortName and NeTEx private code to OTP internalPlanningCode [#3088](https://github.com/opentripplanner/OpenTripPlanner/issues/3088)
- Add MQTT transport for the GTFS-RT trip update updater [#3094](https://github.com/opentripplanner/OpenTripPlanner/issues/3094)
- Add FinlandWayPropertySetSource [#3096](https://github.com/opentripplanner/OpenTripPlanner/issues/3096)
- Map NeTEx publicCode to OTP tripShortName and NeTEx private code to OTP internalPlanningCode [#3088](https://github.com/opentripplanner/OpenTripPlanner/issues/3088)
- Reading and writing files(CONFIG, GRAPH, DEM, OSM, GTFS, NETEX, DATA_IMPORT_ISSUES) is changed. All files, except configuration files, are read from a data source. We support Google Cloud Storage and the local file system data sources for now, but plan to add at least support for AWS S3 [#2891](https://github.com/opentripplanner/OpenTripPlanner/issues/2891)
- Remove AlertPatcher [#3134](https://github.com/opentripplanner/OpenTripPlanner/issues/3134)
- Update DebugOutput to match new routing phases of OTP2 [#3109](https://github.com/opentripplanner/OpenTripPlanner/issues/3109)
- Filter transit itineraries with relative high cost [#3157](https://github.com/opentripplanner/OpenTripPlanner/issues/3157)
- Fix issue with colliding TripPattern ids after modifications form real-time updaters [#3202](https://github.com/opentripplanner/OpenTripPlanner/issues/3202)
- Fix: The updater config type is unknown: gtfs-http [#3195](https://github.com/opentripplanner/OpenTripPlanner/issues/3195)
- Fix: Problem building and loading the GTFS file in San Fransisco Bay Area [#3195](https://github.com/opentripplanner/OpenTripPlanner/issues/3195)
- Fix: The `BusRouteStreetMatcher` and `TransitToTaggedStopsModule` graph builder modules are not run if the graph is build in two steps, and add progress tracker to BusRouteStreetMatcher. [#3195](https://github.com/opentripplanner/OpenTripPlanner/issues/3195)
- Improvement: Insert project information like Maven version number into configuration files. [#3254](https://github.com/opentripplanner/OpenTripPlanner/pull/3254)   
- Added pathway FeedScopedId as the route text to trip plan responses. [#3287](https://github.com/opentripplanner/OpenTripPlanner/issues/3287)


## Ported over from 1.4 and 1.5

- Add application/x-protobuf to accepted protobuf content-types [#2839](https://github.com/opentripplanner/OpenTripPlanner/issues/2839)
- Make OTP run on Java 11 [#2812](https://github.com/opentripplanner/OpenTripPlanner/issues/2812)
- Fixes surefire test failure during build [#2816](https://github.com/opentripplanner/OpenTripPlanner/issues/2816)
- Disable linking from already linked stops [#2372](https://github.com/opentripplanner/OpenTripPlanner/issues/2372)
- Add Way Property Set for the UK [#2818](https://github.com/opentripplanner/OpenTripPlanner/issues/2818)
- Remove Open Traffic prototype code [#2698](https://github.com/opentripplanner/OpenTripPlanner/issues/2698)
- Docs: improve configuration documentation
- Update onebusaway-gtfs to latest version from OBA project [#2636](https://github.com/opentripplanner/OpenTripPlanner/issues/2636)
- Remove the coupling to OneBusAway GTFS within OTP's internal model by creating new classes replacing the external classes [#2494](https://github.com/opentripplanner/OpenTripPlanner/issues/2494)
- Allow itineraries in response to be sorted by duration [#2593](https://github.com/opentripplanner/OpenTripPlanner/issues/2593)
- Fix reverse optimization bug #2653, #2411
- increase GTFS-realtime feeds size limit from 64MB to 2G [#2738](https://github.com/opentripplanner/OpenTripPlanner/issues/2738)
- Fix XML response serialization [#2685](https://github.com/opentripplanner/OpenTripPlanner/issues/2685)
- Refactor InterleavedBidirectionalHeuristic [#2671](https://github.com/opentripplanner/OpenTripPlanner/issues/2671)
- Add "Accept" headers to GTFS-RT HTTP requests [#2796](https://github.com/opentripplanner/OpenTripPlanner/issues/2796)
- Fix minor test failure against BANO geocoder [#2798](https://github.com/opentripplanner/OpenTripPlanner/issues/2798)
- Fix frequency bounds checking [#2540](https://github.com/opentripplanner/OpenTripPlanner/issues/2540)
- Remove dependency on Conveyal jackson2-geojson
- Changed calculation of slope costs [#2579](https://github.com/opentripplanner/OpenTripPlanner/issues/2579)
- Replace Java built in serialization with faster Kryo [#2681](https://github.com/opentripplanner/OpenTripPlanner/issues/2681)
- Support OSM highway=razed tag [#2660](https://github.com/opentripplanner/OpenTripPlanner/issues/2660)
- Add bicimad bike rental updater [#2503](https://github.com/opentripplanner/OpenTripPlanner/issues/2503)
- Add Smoove citybikes updater [#2515](https://github.com/opentripplanner/OpenTripPlanner/issues/2515)
- Allow big GTFS-realtime feeds by increasing protobuf size limit to 2G [#2739](https://github.com/opentripplanner/OpenTripPlanner/issues/2739)
- Cannot transfer between stops at exactly the same location [#2371](https://github.com/opentripplanner/OpenTripPlanner/issues/2371)
- Improve documentation for `mode` routing parameter [#2809](https://github.com/opentripplanner/OpenTripPlanner/issues/2809)
- Switched to single license file, removing all OTP and OBA file license headers


## 1.3 (2018-08-03)

- Fix stop linking to only one edge of platform [#2472](https://github.com/opentripplanner/OpenTripPlanner/issues/2472)
- Log and allow changing number of HTTP handler threads
- Update Dutch base fare from 89 to 90 cents [#2608](https://github.com/opentripplanner/OpenTripPlanner/issues/2608)
- Add Dutch fare service [#2571](https://github.com/opentripplanner/OpenTripPlanner/issues/2571)
- Revise unit tests to use less memory
- Run all graph updater setup methods sequentially [#2545](https://github.com/opentripplanner/OpenTripPlanner/issues/2545)
- Allow vehicle rental systems with cars (stopgap parameter on bike rental)
- Bump R5 version to get newer gtfs-lib and FST serialization
- Move stopClusterMode parameter from routing config to build config [#2558](https://github.com/opentripplanner/OpenTripPlanner/issues/2558)
- Update encrypted Maven artifact signing key (it expired)
- Clean up logging
- Remove/update deprecated HTTPClient, add missing SSL ciphers [#2451](https://github.com/opentripplanner/OpenTripPlanner/issues/2451)
- Make maxTransfer options configurable through scripting API [#2507](https://github.com/opentripplanner/OpenTripPlanner/issues/2507)
- Fix scripts when entity IDs contain colons [#2474](https://github.com/opentripplanner/OpenTripPlanner/issues/2474)
- Add HTML report for stops more than 20m from linked road [#2460](https://github.com/opentripplanner/OpenTripPlanner/issues/2460)
- Update fares in NycFareServiceImpl [#2466](https://github.com/opentripplanner/OpenTripPlanner/issues/2466)
- Compact legs NPE fix [#2449](https://github.com/opentripplanner/OpenTripPlanner/issues/2449) [#2490](https://github.com/opentripplanner/OpenTripPlanner/issues/2490)
- Docs: elevation data configuration, USGS DEM files
- Docs: Update list of deployments
- Docs: API, list of deployments, usage stats and tutorials
- Docs: Update leadership committee listing following Boston Summit
- Docs: Update OTP logo (Thanks Kate Chanba!)

## 1.2 (2017-09-18)

- Add support for consuming GBFS bike-rental availability feeds. [#2458](https://github.com/opentripplanner/OpenTripPlanner/issues/2458)
- Add GBFS configuration example
- Add flag for including requested start/end time in maxHours in planner API. [#2457](https://github.com/opentripplanner/OpenTripPlanner/issues/2457)
- Add maxTransferDistance graph builder parameter
- Add option for filtering non-pickup stops in TransitIndex stop times functions. [#2377](https://github.com/opentripplanner/OpenTripPlanner/issues/2377)
- Support foot/bicycle=discouraged OSM tag. [#2415](https://github.com/opentripplanner/OpenTripPlanner/issues/2415)
- Improve linking of transit platforms to connecting access ways. [#2422](https://github.com/opentripplanner/OpenTripPlanner/issues/2422) / [#2428](https://github.com/opentripplanner/OpenTripPlanner/issues/2428)
- Fix bug when building graph with parent station transfers. [#2404](https://github.com/opentripplanner/OpenTripPlanner/issues/2404) / [#2410](https://github.com/opentripplanner/OpenTripPlanner/issues/2410)
- Fix bugs in park and ride search. [#2424](https://github.com/opentripplanner/OpenTripPlanner/issues/2424)
- Support different stop ID formats in field trip module
- Update URL in BANO geocoding module. [#2438](https://github.com/opentripplanner/OpenTripPlanner/issues/2438) / [#2439](https://github.com/opentripplanner/OpenTripPlanner/issues/2439)
- Add more debug information related to trips matching using GTFS-RT feed. [#2432](https://github.com/opentripplanner/OpenTripPlanner/issues/2432)
- Update default PATH_NOT_FOUND message to new wording developed w/ TriMet. [#2355](https://github.com/opentripplanner/OpenTripPlanner/issues/2355)
- Update Travis build configuration to not attempt GPG operations. [#2441](https://github.com/opentripplanner/OpenTripPlanner/issues/2441)
- Fix javadoc URL in scripting documentation. [#2437](https://github.com/opentripplanner/OpenTripPlanner/issues/2437)
- Automatically link to GitHub issues in Changelog. [#2426](https://github.com/opentripplanner/OpenTripPlanner/issues/2426)
- Expose FeedInfo objects in the Index API [#2456](https://github.com/opentripplanner/OpenTripPlanner/issues/2456)
- Changes to Puget Sound region fare calculation [#2484](https://github.com/opentripplanner/OpenTripPlanner/issues/2484)
- Fix coordinatates when clustering by parent station [#2447](https://github.com/opentripplanner/OpenTripPlanner/issues/2447)
- Allow setting OSM Way Properties from build-config.json [#2389](https://github.com/opentripplanner/OpenTripPlanner/issues/2389)
- Optionally compact ("reverse-optimize") results with complete reverse search [#2449](https://github.com/opentripplanner/OpenTripPlanner/issues/2449)
- Add updater for urbaninfrastructure city bikes [#2448](https://github.com/opentripplanner/OpenTripPlanner/issues/2448)
- Miscellaneous documentation updates

## 1.1 (2017-03-16)

- Deploy to Sonatype OSSRH and Maven Central
- Documentation updates including repo links
- New router-config stopClusterMode: clustering by parent station or geography [#2364](https://github.com/opentripplanner/OpenTripPlanner/issues/2364)
- Spanish and Portuguese UI Translations
- In TimeSurface API, serialize travel times to every point when detail=true
- Make OSM highway=corridor pedestrian routable
- Fix GraphIndex.stopTimesForStop to search on the request day rather than now
- Update GraphQL to latest version and improve support for complex arguments [#2367](https://github.com/opentripplanner/OpenTripPlanner/issues/2367)
- Add support for operationName to the graphql endpoint
- Fix findClosestStopsByWalking, properly set RoutingContext
- Fixed major routing problem where dead-end SimpleTransfers blocked walking paths [#2414](https://github.com/opentripplanner/OpenTripPlanner/issues/2414)
- Created Github issue template
- Avoid negative elevation figures: Compute ellipsoid-geoid offset and optionally apply to elevation calculations [#2301](https://github.com/opentripplanner/OpenTripPlanner/issues/2301)
- Fix VCub bike share updater using new API variable names.
- Fix spurious different-day warning [#2399](https://github.com/opentripplanner/OpenTripPlanner/issues/2399)
- Shutdown hook to gracefully shut down Grizzly [#2384](https://github.com/opentripplanner/OpenTripPlanner/issues/2384)
- Added headsign attribute for stoptimes in GraphQL [#2224](https://github.com/opentripplanner/OpenTripPlanner/issues/2224)
- Allow Cars on highway=*;bicycle=designated [#2374](https://github.com/opentripplanner/OpenTripPlanner/issues/2374)
- Expose PruneFloatingIslands parameters in build-config.json
- Lazy initialization of stop clusters where needed
- Include Agency/Route branding in responses
- Include turn-by-turn walking directions for transfer legs [#1707](https://github.com/opentripplanner/OpenTripPlanner/issues/1707)
- Output error when edge lengths are negative, and set them to 1mm
- Add disableAlertFiltering API flag [#2351](https://github.com/opentripplanner/OpenTripPlanner/issues/2351)
- Do not show arrival times at terminal stops in stop time viewer [#2357](https://github.com/opentripplanner/OpenTripPlanner/issues/2357)
- Index API now returns stop information URL, enabling hyperlinks in trip viewer [#2352](https://github.com/opentripplanner/OpenTripPlanner/issues/2352)
- Remove all unused model classes for index API [#1301](https://github.com/opentripplanner/OpenTripPlanner/issues/1301)
- Apply an interlining fix from 0.10 branch
- Allow quoted search phrases in the Lucene search [#2279](https://github.com/opentripplanner/OpenTripPlanner/issues/2279)
- Re-implement maxHours filter [#2332](https://github.com/opentripplanner/OpenTripPlanner/issues/2332)
- Properly set wheelchairAccessible on area edges
- Fixed file URL in test [#2339](https://github.com/opentripplanner/OpenTripPlanner/issues/2339)
- Add details field to fares, listing which legs each fare applies to [#1699](https://github.com/opentripplanner/OpenTripPlanner/issues/1699)

## 1.0 (2016-09-09)

- Fix problem with missing embedded router-configs.
- Check whether trips have been banned when applying in-seat transfers (interlining).
- Load embedded config for existing graphs on disk.
- Apply max walk distance to transfers, not just initial and final walk.
- Remove Conveyal tiles from client (which was getting expensive), add free Carto/MapZen tiles.
- Fixed headsigns: in itineraries, headsign for a leg used to always be the last stop.
- Updated default map tile sets in the client because Mapquest is no longer gratis.
- Fix problem with empty list ??? [#1873](https://github.com/opentripplanner/OpenTripPlanner/issues/1873)
- Rewrite of intermediate places handling in GraphPathFinder. Original request is cloned for each intermediate path.
- Routes in GraphQL API Change "type" to "mode" and add "type" as route type to Route for GraphQL
- Add effective end date to alerts (from HSL).
- Rutebanken Citybike bike share.
- Correct TPEG transport modes TPEG 401 and 402 to be "subway".
- Ignore exceptions caused by errors in OSM linear rings.
- Updated to version 2.18 of Jersey to fix hanging threads in Grizzly.
- Removed confusing "Busish" and "Trainish" pseudo-modes.
- FareService for Seattle: allow specifying fares in GTFS instead of hard-coding them in Java. Senior/youth fare prices are given in an extra column in fare attributes. Per-trip fares are taken into consideration when calculating fares in this region.
- Update new linker to link to transitStops if no streets are found.
- Show the name supplied in the request for the origin/destination points in the response.
- Throw a trivialPath exception if start/end point are on the same edge.
- Switch to only use the new SimpleStreetLinker, even for search start and end points. Completely removed old linker classes. Changes for proper handling of wheelchairs and bicycles at start and end points.
- Properly handle null timetableSnapshots when there is no real-time data.


## 0.20 (2016-06-10)

- Re-enabled Enunciate, which works properly with OTP now. This means we have auto-generated API docs.
- Make headsign and block ID visible in the Stop Viewer.
- NYC fare service: filter out non-NYC agencies.
- Optionally log all requests to a file.
- Make max distance for in-seat transfers (interlining) configurable. Previously it was hardcoded at 200m.
- Polish translation for web client.
- Introduced bikeShareId in trip plans (separate from stopIds).
- Support for ShareBike bike rental system in Oslo, Drammen, Trondheim, Milan, Barcelona and Mexico City among others.
- Changed default waitAtBeginningFactor and timeouts.
- Show alert in client when itinerary departure date differs from search date.
- Exposed realtimeState in GraphQL responses.
- Fixed a routerConfig NullPointerException.
- Support for San Francisco bike share from leoromanovsky.
- GraphQL API for most transit data from hannesj.
- Disallow shortcuts through multiple StationStopEdges.
- Add support for airplanes (from HSL)
- Major simplification and correction of the longDistance heuristic, removed obsolete runState.options.heuristicWeight.
- Return default OSM level for ways that are not found.
- Profile routing: use earliest arrival objective function on-street, properly handle TrivialPathExceptions.
- Fixed ID matching when applying AlertPatches.
- Fixed banning of agencies in multi agency feeds.
- More coherent handling of feed IDs as scope for GTFS IDs.
- Added transit service start and end timestamps to BuildInfo.
- Handle embeded router configuration for POSTed graphs and zips for building.
- Simplified router-config handling.
- Properly lazy-initialize profile routing stopClusters. Added stop clusters to the Index API.
- Completely removed the ill-advised path parser system, which was too clever for its own good.    
- Sort itineraries by total travel time rather than in-transit time.
- Rental bikes: allow loading generic KML.
- Removed the experimental TransportNetwork classes, which shared no code with the rest of OTP and were duplicated in the R5 project. There are still some elements that can be cleaned out when only R5 is used by Conveyal's analysis system. The broker code in OTP is now able to start up R5 workers for Analyst.
- Use the Conveyal fork of the OBA GTFS loader, so that we can add our own extensions to GTFS.
- Updated docs to offer Conveyal Maven repo as a place to get prebuilt OTP.

## 0.19.0 (2016-05-25)

- TODO

## 0.18.0 (2015-05-29)

- Ability to load elevation from projected GeoTIFF
- Clarified axis order for unprojected GeoTIFFs
- Stop viewer and car distance fixed in client
- Server-side localization improvements
- Proper names for intersections
- JSON config for loading bikeshare and park and ride lots from OSM
- More ways to fetch isochrones
- Fixed frequency-based routing in repeated RAPTOR
- Calculate graph envelope at build time not runtime
- Fixed slow excessive HashGrid search
- Readthedocs documentation updates

## 0.17.0 (2015-05-14)

- Allow fetching arrivals/departures over a particular time window
- Completely new spatial analysis implementation: repeated RAPTOR search at every minute in a departure time window
- More reproducible spatial analysis results across similar graphs, thanks to more consistent splitting of streets etc.
- Sigmoidal accessibility metric rolloff (rather than hard-edged cutoff)
- Correction of equirectangular projection used in spatial analysis
- Improved, simplified, deterministic linking of stops into the street network

## 0.16.0 (2015-05-07)

- Several improvements to OSM tag based traversal permissions
- Scripting documentation
- Accept TIFF files whose names end in .tiff not .tif
- Store distances (not times) in Analyst Samples to allow variable walk speed
- Fixed bug in graph auto-scanning
- Fixed client-side bug in first and last itinerary buttons
- OTP startup scripts no longer use wildcards
- Transit, bike rental, and parking linking done in one module
- Elevation tiles for the US can be fetched from Amazon S3
- Bumped language level to Java 8 (lambda functions, method references, collection streams)

## 0.15.0 (2015-04-14)

- Fare module for Seattle
- JSON fare module and OSM street naming configuration
- Significant improvements to speed and result quality of Profile Routing
- Support for added and modified GTFS-RT trips (thanks Jaap Koelewijn of DAT Mobility and Plannerstack)
- Detailed edge lists in profile routing responses (for Transitive.js)
- Support for multiple access modes including bike rental in profile routing
- Fixes to graph reloading via web API
- Improved comments in code and documentation of PointSets
- Pulled MapDB GTFS loader out into a separate repo
- Working artifact version was 0.15.0-SNAPSHOT instead of 1.0.0-SNAPSHOT (anticipating frequent point releases)

## 0.14.0 (2015-03-28)

- JSON configuration of graph building and routers
- Began moving documentation (including this changelog) into the OTP repo and rewriting it page by page. It is built statically from Markdown using mkdocs and published on readthedocs.
- Street edge lists and bike rental station IDs in profile routing results (allows better rendering)
- Improved correctness of profile routing
- Qualified modes including rented bikes work in profile routing
- Simplification of qualified mode sets
- Elevation models are loaded from TIFFs in graph directory
- Tiles for differences between TimeSurfaces
- Restructured relationship between Routers and Graphs
- Various changes enabling use of Analyst features in a cluster computing environment.
- Removed several single-implementation interfaces, factories, services and other superfluous abstractions
- Various client fixes related to the transit index API
- Revised nearby stops logic and transfer generation to eliminate useless transfer edges
- New Index API endpoints for geometries, transfers etc.
- Isochrone generation fixes
- Default mode of operation is now “long distance mode”
- Process for finding alternative routes is now based on banning trips and retrying, while reusing the heuristic
- Optimization objective functions are swappable, and have been simplified and corrected
- All client Javascript librariess are now pulled from a CDN
- Dutch BAG and French BANO geocoders
- Bus to street matching improvements
- Complete MapDB based GTFS and OSM loader libraries (will become separate projects, not yet connected to OTP graph builder)
- API documentation generation working again
- Disable some time consuming graph building steps by default
- Finnish and Swedish translations
- Subway-specific JSON configuration options (street to platform time)
- Realtime fetch / streaming configurable via JSON
- Stairs reluctance is much higher when carrying a bike
- Graph visualizer routing progress animates when a search is triggered via the web API
- Assume WGS84 (spherical distance calculations) everywhere
- Removed custom motor vehicle (which was unmaintained and not documented)
- Ability to poll for bike rental locations only once at startup
- Stoptimes are fetched for a specific service day in index API
- Bicycle triangle support in profile routing
- Proper handling of multiple access modes with different speeds in profile routing
- Command line option to output OTP's version

## 0.13.0 (2014-12-05)
- Detect apparent errors in GTFS interlining
- Long distance mode: use a pure weight-based state comparison, and use trip-banning retrying logic to get multiple paths. This compromises correctness somewhat but brings search times back within reason for large regional graphs. Also, we create significantly less SimpleTransfers.
- Progress on GTFS reading and writing library (not yet used by OTP).
- Bug fixes for tiny street edges, time zones.
- Deployment of artifacts to maven.conveyal.com via S3.
- Handle park and ride lots that have roads running through them, but don't share nodes with those roads.

## 0.12.1 (2014-11-17)
- Fixed threading problem caused by graph visualization instrumentation [#1611](https://github.com/opentripplanner/OpenTripPlanner/issues/1611)
- Fixed 'unconnected areas' infinite loop [#1605](https://github.com/opentripplanner/OpenTripPlanner/issues/1605)

## 0.12.0 (2014-11-11)
- Graph building from zipball of data sent over the wire
- OTP-specific GTFS loader library with error checking and recovery
- Bike and car park and ride improvements
- Stable hash codes for stop patterns and trips
- Bicycle safety and wheelchair access tile generators
- Newer versions of Grizzly, Jackson, and Enunciate (documentation generation now works)
- Redesigned HashGrid spatial index
- Significant reduction in graph size in memory and on disk
- Improved internationalization
- Ability to pause and step search in graph visualizer
- Additional graph visualizer modes for spotting overbranching
- Movement toward 1.0 web services API
- Kiss and Ride
- Complete removal of Spring
- Complete removal of Lombok
- CORS replaces JSONP
- Pointset classes for dealing with one-to-many calculations and accessibility calculations
- Experimental "Profile routing" which enumerates reasonable route combinations over a time range rather than exact itineraries
- Single-module Maven build (complete elimination of submodules)
- Alternate Gradle build script
- full internationalization of the map-based web client
- basic Lucene-based built-in geocoder

## 0.11.0 (2014-03-24)
- Built-in HTTP server layer, making it possible to distribute OTP as a standalone JAR
- "Long-distance" mode for large graphs, including bidirectional goal direction heuristic.
- Simplified Maven project structure with less submodules
- GTFS-RT trip update support, including streaming incremental data, which directly affects route optimization

## 0.10.0 (2014-03-18)
This release was made to consolidate all the development that had occurred with a 0.9.x-SNAPSHOT Maven version. The changes were very significant and it was not appropriate to tag them as a minor bugfix release after the 0.9 tag. Though this release was performed at the same time as 0.11.0, it represents a much earlier stage in the development of OTP.

## 0.7.0 (2012-04-29)
- Bike rental support (thanks Laurent Grégoire)
- Realtime bike rental availability feed support
- Updated to new version of One Bus Away GTFS/CSV, fixing timezone and string interning issues (thanks Brian Ferris)
- Bugfixes in area routing, OSM loading, nonexistant NED tiles, route short names
- Dutch and French language updates
- Catch negative edge weights due to broken GTFS
- Significant (10-20%) speedup by moving a field into StateData (thanks Laurent Grégoire)

## 0.6.0 (2012-04-25)
- area routing
- more lenient parsing of times
- new directions icon set with SVG sources (thanks Laurent G)

## 0.5.4 (2012-04-06)
- catch 0 divisors in NED builder, preventing NaN propagation to edge lengths
- avoid repeated insertion of edges into edge lists, which are now threadsafe edge sets
- identity equality for edges
- bounding box check in UnifiedCoverage (speed up NED loading)
- Dutch API messages
- elevation override fix
- less verbose graph builder (be sure to check graphbuilder annotation summary)
- replacement streets given names
- geocoder bug fix (thanks Laurent Gregoire)
- git commit IDs included in MavenVersion, allowing clearer OTP/Graph version mismatch warnings
- fix problems with immediate reboarding and unexpected edges in itinerary builder
- favicon (thanks Joel Haasnoot)
- Legs in API response have TripId (for realtime information)
- Polish locale (thanks Łukasz Witkowski)
- transfers.txt can define station paths, entry costs for stations
- allow loading a base graph into graphbuilder instead of starting from scratch

## 0.5.3 (2012-03-23)
- GTFS loader now loads feeds one-at-a-time, allowing per-feed configuration
- half-written graph files are now deleted on graph build error
- DST issue OTP-side fixes, tests adjusted to use timezones
- updated French translation
- fixed problem with loop ways in OSM
- graph coherency checking
- improved OSM floor number handling
- handle units in ele tags
- ferry icons (thanks Joel Haasnoot)
- mapbox streets tile layer is now the default
- complete Dutch translation

## 0.5.2 (2012-03-20)
- hop speed/distance checks, duplicate shape point filtering, etc.

## 0.5.1 (2012-03-16)
- more transit index features
- default agencyIDs now determined on a per-feed basis
- fixed fare overflow problem
- fixed bug in loop road turn conversion
- additional graphbuilder warnings and annotations
- fixed a batch of bugs found by fixbugs  

## 0.5.0 (2012-03-09)
- stop codes, zones, and agency names in planner responses
- encapsulation of edge list modifications
- expanded edge and vertex type hierarchy
- use mapquest OSM server by default
- Turkish locale (thanks Hasan Tayyar Beşik)
- German and Italian locales (thanks Gerardo Carrieri)
- bookmarkable trip URLs (thanks Matt Conway)
- elevator and OSM level support (thanks Matt Conway)
- BART/Muni fare service
- release and javadoc/apidoc publishing automation
- graph versioning based on Maven artifact version
- API for browsing graph internals
- improved stop linking
- optional island removal graphbuilder step
- and of course, lots of bugfixes

## 0.4.4 (2012-02-06)
Release in anticipation of upcoming merges.
