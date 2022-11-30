# Changelog

The changelog list most feature changes between each release. The list is automatically created
based on merged pull requests. Search GitHub issues and pull requests for smaller issues.

## 2.3.0 (in progress)

- Remove unused StreetClasses from StreetEdge [#4545](https://github.com/opentripplanner/OpenTripPlanner/pull/4545)
- Do not run forward heuristic to calculate LAT, it is calculated from data already [#4537](https://github.com/opentripplanner/OpenTripPlanner/pull/4537)
- Refactoring SIRI StopConditions from TransitAlert to EntitySelector [#4196](https://github.com/opentripplanner/OpenTripPlanner/pull/4196)
- Add cost of egress leg to the cost of the last transit leg if transit leg arrives at destination [#4547](https://github.com/opentripplanner/OpenTripPlanner/pull/4547)
- Setting explicit TTL on AzureSiriUpdater subscription messages [#4584](https://github.com/opentripplanner/OpenTripPlanner/pull/4584)
- Parse NeTEx fare zones from a FareFrame [#4563](https://github.com/opentripplanner/OpenTripPlanner/pull/4563)
- Add fallback for missing operator name [#4588](https://github.com/opentripplanner/OpenTripPlanner/pull/4588)
- Add performance test for switzerland [#4576](https://github.com/opentripplanner/OpenTripPlanner/pull/4576)
- Add Bikely vehicle parking updater [#4589](https://github.com/opentripplanner/OpenTripPlanner/pull/4589)
- Fix interchange generation when identical consecutive stops are filtered [#4586](https://github.com/opentripplanner/OpenTripPlanner/pull/4586)
- Add test for SpeedTestConfigs used in performance tests [#4596](https://github.com/opentripplanner/OpenTripPlanner/pull/4596)
- Add alerts to leg when reading in a leg reference [#4595](https://github.com/opentripplanner/OpenTripPlanner/pull/4595)
- Remove KML bike parking updater [#4602](https://github.com/opentripplanner/OpenTripPlanner/pull/4602)
- Initial implementation of via search [#4554](https://github.com/opentripplanner/OpenTripPlanner/pull/4554)
- Use material design theme for documentation, auto-deploy to Github Pages [#4575](https://github.com/opentripplanner/OpenTripPlanner/pull/4575)
- Add stationTransferPreference option for GTFS feeds [#4599](https://github.com/opentripplanner/OpenTripPlanner/pull/4599)
- Fix osmDefaults so they are used if something is not set in source [#4635](https://github.com/opentripplanner/OpenTripPlanner/pull/4635)
- Add demDefaults [#4637](https://github.com/opentripplanner/OpenTripPlanner/pull/4637)
[](AUTOMATIC_CHANGELOG_PLACEHOLDER_DO_NOT_REMOVE)


## 2.2.0 (2022-11-01)

### Notable Changes

- The performance for trip search is improved by ~20-30% 
- Support for GTFS-RT Vehicle Positions [#3981](https://github.com/opentripplanner/OpenTripPlanner/pull/3981)
- Support for NeTex DatedServiceJourneys [#3889](https://github.com/opentripplanner/OpenTripPlanner/pull/3889)
- Cost-based wheelchair-accessible routing [#4045](https://github.com/opentripplanner/OpenTripPlanner/pull/4045)
- Improve stop linking by taking OSM platform polygons into account [#4116](https://github.com/opentripplanner/OpenTripPlanner/pull/4116)
- Provide an OCI container image on DockerHub [#4168](https://github.com/opentripplanner/OpenTripPlanner/pull/4168)
- Re-add block-based interlining [#4264](https://github.com/opentripplanner/OpenTripPlanner/pull/4264)
- Add walk safety to street routing [#4328](https://github.com/opentripplanner/OpenTripPlanner/pull/4328)
- Experimental support for GTFS Fares V2 [#4338](https://github.com/opentripplanner/OpenTripPlanner/pull/4338)
- Document JVM configuration options [#4492](https://github.com/opentripplanner/OpenTripPlanner/pull/4492)
- Support for HTTPS datasource for Graph Building [#4482](https://github.com/opentripplanner/OpenTripPlanner/pull/4482)
- Metrics for realtime trip updaters [#4471](https://github.com/opentripplanner/OpenTripPlanner/pull/4471)
- Configuration Documentation generated programmatically [#4478](https://github.com/opentripplanner/OpenTripPlanner/pull/4478)


### Detailed changes by Pull Request

- NeTEx mapping for WheelChairBoarding [#3945](https://github.com/opentripplanner/OpenTripPlanner/pull/3945)
- OTP support for NeTEx GroupOfLines [#3928](https://github.com/opentripplanner/OpenTripPlanner/pull/3928)
- Implement trip banning [#3953](https://github.com/opentripplanner/OpenTripPlanner/pull/3953)
- Improve performance of Park+Ride, Bike+Ride and Bike+Transit [#3906](https://github.com/opentripplanner/OpenTripPlanner/pull/3906)
- Re-add GraphCoherencyCheckerModule [#3985](https://github.com/opentripplanner/OpenTripPlanner/pull/3985)
- Modify TripPattern logic for updating stop-to-stop geometry [#3988](https://github.com/opentripplanner/OpenTripPlanner/pull/3988)
- Read translations from GTFS and Netex sources for stop names [#3808](https://github.com/opentripplanner/OpenTripPlanner/pull/3808)
- Update to Java 17 and Kryo 5 [#3994](https://github.com/opentripplanner/OpenTripPlanner/pull/3994)
- Remove optimize=TRANSFERS [#4004](https://github.com/opentripplanner/OpenTripPlanner/pull/4004)
- Enable GBFS floating vehicles by default [#4012](https://github.com/opentripplanner/OpenTripPlanner/pull/4012)
- Don't try to repair trips with negative dwell or driving times, drop them instead [#4019](https://github.com/opentripplanner/OpenTripPlanner/pull/4019)
- Add support for SKIPPED stop time updates in SCHEDULED update handling [#3960](https://github.com/opentripplanner/OpenTripPlanner/pull/3960)
- Elevation handling improvements [#4033](https://github.com/opentripplanner/OpenTripPlanner/pull/4033)
- NeTEx mapping of WheelchairBoarding from ServiceJourney to Trip [#4043](https://github.com/opentripplanner/OpenTripPlanner/pull/4043)
- Add GBFS form factors to GraphQL API [#4062](https://github.com/opentripplanner/OpenTripPlanner/pull/4062)
- Add Geocoder API for debug client searches [#4068](https://github.com/opentripplanner/OpenTripPlanner/pull/4068)
- Consider Wheelchair boarding/alightning when routing from or to directly to a stop [#4046](https://github.com/opentripplanner/OpenTripPlanner/pull/4046)
- Add an implementation for fetching alternative legs [#4071](https://github.com/opentripplanner/OpenTripPlanner/pull/4071)
- Allow single SIRI-situations to fail without affecting others [#4083](https://github.com/opentripplanner/OpenTripPlanner/pull/4083)
- Fix problem with submode filter [#4093](https://github.com/opentripplanner/OpenTripPlanner/pull/4093)
- Make maxWheelchairSlope not a hard cut-off, but apply a cost instead [#4088](https://github.com/opentripplanner/OpenTripPlanner/pull/4088)
- Expose Leg.serviceDate in trip end-point [#4096](https://github.com/opentripplanner/OpenTripPlanner/pull/4096)
- Enable overriding maxDirectStreetDuration per mode [#4104](https://github.com/opentripplanner/OpenTripPlanner/pull/4104)
- Preserve language in SIRI/GTFS-RT alert messages [#4117](https://github.com/opentripplanner/OpenTripPlanner/pull/4117)
- Use board/alight cost only for transits [#4079](https://github.com/opentripplanner/OpenTripPlanner/pull/4079)
- Improve SIRI realtime performance by reducing stopPattern duplicates [#4038](https://github.com/opentripplanner/OpenTripPlanner/pull/4038)
- Siri updaters for Azure ServiceBus [#4106](https://github.com/opentripplanner/OpenTripPlanner/pull/4106)
- Fallback to recorded/expected arrival/departure time if other one is missing in SIRI-ET [#4055](https://github.com/opentripplanner/OpenTripPlanner/pull/4055)
- Allow overriding GBFS system_id with configuration [#4147](https://github.com/opentripplanner/OpenTripPlanner/pull/4147)
- Fix error with transfer-slack and GTFS minTransferTime [#4120](https://github.com/opentripplanner/OpenTripPlanner/pull/4120)
- Use actual distance for walk distance in StreetEdge [#4125](https://github.com/opentripplanner/OpenTripPlanner/pull/4125)
- Don't indicate stop has been updated when NO_DATA is defined [#3962](https://github.com/opentripplanner/OpenTripPlanner/pull/3962)
- Implement nearby searches for car and bicycle parking [#4165](https://github.com/opentripplanner/OpenTripPlanner/pull/4165)
- Do not link cars to stop vertices in routing [#4166](https://github.com/opentripplanner/OpenTripPlanner/pull/4166)
- Add Siri realtime occupancy info [#4180](https://github.com/opentripplanner/OpenTripPlanner/pull/4180)
- Add gtfs stop description translations [#4158](https://github.com/opentripplanner/OpenTripPlanner/pull/4158)
- Add option to discard min transfer times [#4195](https://github.com/opentripplanner/OpenTripPlanner/pull/4195)
- Use negative delay from first stop in a GTFS RT update in previous stop times when required [#4035](https://github.com/opentripplanner/OpenTripPlanner/pull/4035)
- OTP2 no longer crashes on invalid GTFS stop time sequences [#4205](https://github.com/opentripplanner/OpenTripPlanner/pull/4205)
- Cost-based wheelchair accessibility routing for streets [#4163](https://github.com/opentripplanner/OpenTripPlanner/pull/4163)
- Expose SIRI ET PredictionInaccurate in Transmodel-API [#4217](https://github.com/opentripplanner/OpenTripPlanner/pull/4217)
- Do not apply walkable area processing to open platform geometries [#4225](https://github.com/opentripplanner/OpenTripPlanner/pull/4225)
- Add field 'routingErrors' to LegacyGraphQLAPI [#4253](https://github.com/opentripplanner/OpenTripPlanner/pull/4253)
- Configure idempotent upload to Google Cloud Storage [#4269](https://github.com/opentripplanner/OpenTripPlanner/pull/4269)
- Add support to unprefer certain routes [#4238](https://github.com/opentripplanner/OpenTripPlanner/pull/4238)
- Fix inconsistent mapping of NeTEx quay publicCode [#4282](https://github.com/opentripplanner/OpenTripPlanner/pull/4282)
- Take street legs into account when applying the similar legs filter [#4280](https://github.com/opentripplanner/OpenTripPlanner/pull/4280)
- Make graph time zone configurable [#4281](https://github.com/opentripplanner/OpenTripPlanner/pull/4281)
- Support real-time updated wheelchair accessibility for trips [#4255](https://github.com/opentripplanner/OpenTripPlanner/pull/4255)
- Include waiting time at beginning and end in TransitGeneralizedCostFilter [#4188](https://github.com/opentripplanner/OpenTripPlanner/pull/4188)
- Allow filtering out of time-shifted itineraries with same routes and stops [#4298](https://github.com/opentripplanner/OpenTripPlanner/pull/4298)
- Remove unmaintained custom fare calculators for NYC, Seattle, SF Bay Area, Netherlands [#4273](https://github.com/opentripplanner/OpenTripPlanner/pull/4273)
- Return meaningful error if origin and destination are the same [#4364](https://github.com/opentripplanner/OpenTripPlanner/pull/4364)
- Allow configuring maxAccessEgressDuration, searchWindow, timetableView [#4377](https://github.com/opentripplanner/OpenTripPlanner/pull/4377)
- Add custom bike rental data updater (vilkku) as an extension [#4381](https://github.com/opentripplanner/OpenTripPlanner/pull/4381)
- Flex: when optimizing paths preserve egress transfers [#4378](https://github.com/opentripplanner/OpenTripPlanner/pull/4378)
- Skip opening hours if no time-zone configured [#4372](https://github.com/opentripplanner/OpenTripPlanner/pull/4372)
- Digitransit stop vector layer updates [#4404](https://github.com/opentripplanner/OpenTripPlanner/pull/4404)
- Re-enable copying of submode to trip from route [#4407](https://github.com/opentripplanner/OpenTripPlanner/pull/4407)
- Configure each transit feed properties individually  [#4399](https://github.com/opentripplanner/OpenTripPlanner/pull/4399)
- Account for boarding restrictions when calculating direct transfers [#4421](https://github.com/opentripplanner/OpenTripPlanner/pull/4421)
- Configure the import of OSM extracts individually [#4419](https://github.com/opentripplanner/OpenTripPlanner/pull/4419)
- Configure the import of elevation data individually [#4423](https://github.com/opentripplanner/OpenTripPlanner/pull/4423)
- Return typed errors from realtime updates, prepare for realtime statistics [#4424](https://github.com/opentripplanner/OpenTripPlanner/pull/4424)
- Add feature switch for matching ET messages on stops [#4347](https://github.com/opentripplanner/OpenTripPlanner/pull/4347)
- Make safety defaults customizable for walking and cycling [#4438](https://github.com/opentripplanner/OpenTripPlanner/pull/4438)
- Fix block-based interlining when importing several GTFS feeds [#4468](https://github.com/opentripplanner/OpenTripPlanner/pull/4468)
- Do not remove patterns with non-running DatedServiceJourneys [#4474](https://github.com/opentripplanner/OpenTripPlanner/pull/4474)
- Discard direct walking results if direct mode is flex [#4476](https://github.com/opentripplanner/OpenTripPlanner/pull/4476)
- Implement free transfers in GTFS Fares V2 [#4460](https://github.com/opentripplanner/OpenTripPlanner/pull/4460)
- Read and expose time zone in NeTEx/Transmodel API [#4480](https://github.com/opentripplanner/OpenTripPlanner/pull/4480)
- Remove filtering of flex patterns in NeTEX import [#4493](https://github.com/opentripplanner/OpenTripPlanner/pull/4493)
- Make walk and bicycle safety defaults adjustable based on speed limits on ways [#4484](https://github.com/opentripplanner/OpenTripPlanner/pull/4484)
- Enable fuzzyTripMatching in other SIRI-ET updaters [#4495](https://github.com/opentripplanner/OpenTripPlanner/pull/4495)
- Add support for importing zip datasets containing large entries [#4508](https://github.com/opentripplanner/OpenTripPlanner/pull/4508)
- Add feature toggle for turning off the debug client [#4512](https://github.com/opentripplanner/OpenTripPlanner/pull/4512)
- Add 'exact match' specifier for way property sets [#4505](https://github.com/opentripplanner/OpenTripPlanner/pull/4505)
- Remove unnecessary sourceType attribute from GTFS-RT configuration  [#4525](https://github.com/opentripplanner/OpenTripPlanner/pull/4525)
- Do not allow boarding/alighting at GroupStops in RAPTOR [#4534](https://github.com/opentripplanner/OpenTripPlanner/pull/4534)
- Read the feed id from the agency when storing agencies [#4536](https://github.com/opentripplanner/OpenTripPlanner/pull/4536)

## 2.1.0 (2022-03-17)


### Notable Changes

- GBFS 2.2 is supported including "form factors" (bike, scooter, car) and floating vehicles (with no fixed station)
- Constrained Transfers (Netex interchanges / GTFS `transfers.txt`)
- Transfers for bicycle and wheelchair users distinct from walking paths
- Support for GTFS-Flex v2
- Support for frequency-based trips (GTFS `frequencies.txt`, does not exist in Netex)
- Many 1.5 features not fully implemented in 2.0 have been reintroduced
- Improved result quality relative to both 1.5 and 2.0: filtering of itineraries and selection of transfer points between routes
- Car and bicycle parking has been combined into vehicle parking (enhanced with real-time details like remaining parking spaces)
- New system for paging of routing results via cursor token
- Response times should be roughly stable since 2.0. Performance much improved over OTP1 for long searches, may be somewhat slower for short searches. Extremely depdendent on data set used, so test on your specific data.
- System integration tests for ongoing performance measurement 


### Detailed changes by Pull Request

- Fix NullPointerException when a RealTime update do not match an existing TripPattern [#3284](https://github.com/opentripplanner/OpenTripPlanner/issues/3284)
- Support for versioning the configuration files [#3282](https://github.com/opentripplanner/OpenTripPlanner/issues/3282)
- Prioritize "direct" routes over transfers in group-filters [#3309](https://github.com/opentripplanner/OpenTripPlanner/issues/3309)
- Remove poor transit results for short trips, when walking is better [#3331](https://github.com/opentripplanner/OpenTripPlanner/issues/3331)
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
- Optimize which stops are used for transfers, using generalized-cost, wait-time and transfer priority. Partially implements [#2788](https://github.com/opentripplanner/OpenTripPlanner/issues/2788)
- Support for stay-seated and guaranteed transfers [#3193](https://github.com/opentripplanner/OpenTripPlanner/issues/3193)
- Fix reading of cached elevation files [#3455](https://github.com/opentripplanner/OpenTripPlanner/pull/3455)
- Added BikeRentalWithMostlyWalking filter [#3446](https://github.com/opentripplanner/OpenTripPlanner/pull/3446)
- Import GTFS-Flex v2 Flexible trips [#3453](https://github.com/opentripplanner/OpenTripPlanner/pull/3453)
- Add support for arriving at the destination with rented bicycles [#3459](https://github.com/opentripplanner/OpenTripPlanner/issues/3459)
- Allow IntersectionTraversalCostModel to be specified in the WayPropertySet [#3472](https://github.com/opentripplanner/OpenTripPlanner/pull/3472)
- Fix for traveling back in time when optimize transfers [#3491](https://github.com/opentripplanner/OpenTripPlanner/pull/3491)
- Transit reluctance per transit mode [#3440](https://github.com/opentripplanner/OpenTripPlanner/issues/3440)
- Allow the removal of P+R results consisting only of driving of walking [#3515](https://github.com/opentripplanner/OpenTripPlanner/pull/3515)
- Allow http headers to be specified for bike rental updaters [#3533](https://github.com/opentripplanner/OpenTripPlanner/pull/3533)
- Per-mode reluctance parameters are added so that itineraries with multiple modes may have varying reluctances. [#3501](https://github.com/opentripplanner/OpenTripPlanner/issues/3501)
- Add `maxAreaNodes` configuration parameter for changing an area visibility calculation limit (https://github.com/opentripplanner/OpenTripPlanner/issues/3534)
- Add maxAccessEgressDurationSecondsForMode to RoutingRequest [#3560](https://github.com/opentripplanner/OpenTripPlanner/issues/3560)
- Add bicycle safety report to report API [#3563](https://github.com/opentripplanner/OpenTripPlanner/issues/3563)
- Optimize Transfers performance issue [#3513](https://github.com/opentripplanner/OpenTripPlanner/issues/3513)
- Don't allow bicycle loops in A* [#3574](https://github.com/opentripplanner/OpenTripPlanner/pull/3574)
- Cancel individual stop on StopPattern instead of TripTimes [#3575](https://github.com/opentripplanner/OpenTripPlanner/issues/3575)
- Do not allow bicycle traversal on ways tagged with mtb:scale [#3578](https://github.com/opentripplanner/OpenTripPlanner/pull/3578)
- Changes to the StopTimes call [#3576](https://github.com/opentripplanner/OpenTripPlanner/issues/3576)
- Fix bug in optimize transfer service decorating path [#3587](https://github.com/opentripplanner/OpenTripPlanner/issues/3587)
- Remove non-GBFS bicycle rental updaters [#3562](https://github.com/opentripplanner/OpenTripPlanner/issues/3562)
- Remove possibility to import vehicle rental stations from OSM, make vehicle rental stations feed scoped [#3601](https://github.com/opentripplanner/OpenTripPlanner/pull/3601)
- When importing Netex, allow bicycles on ferries by default [#3596](https://github.com/opentripplanner/OpenTripPlanner/pull/3596)
- Safely catch some elevation interpolation exceptions [#3412](https://github.com/opentripplanner/OpenTripPlanner/pull/3412)
- Route not found in some conditions with boarding/alighting restrictions [#3621](https://github.com/opentripplanner/OpenTripPlanner/pull/3621)
- Load additional data from GBFS and expose it [#3610](https://github.com/opentripplanner/OpenTripPlanner/pull/3610)
- Allow transfers to use customizable request options [#3324](https://github.com/opentripplanner/OpenTripPlanner/issues/3324)
- Check boarding and alighting permissions in TransferGenerator [#3641](https://github.com/opentripplanner/OpenTripPlanner/pull/3641)
- Stoptimes should return tripId on the REST API. [#3589](https://github.com/opentripplanner/OpenTripPlanner/issues/3589)
- Handle non-symmetric transfers in RAPTOR [#3634](https://github.com/opentripplanner/OpenTripPlanner/issues/3634).
- Order RAPTOR input data so that plans are deterministic [#3580](https://github.com/opentripplanner/OpenTripPlanner/issues/3580)
- Cost on transfer in Raptor [#3617](https://github.com/opentripplanner/OpenTripPlanner/pull/3617)
- Allow for combined NeTEx and GTFS data sources [#3650](https://github.com/opentripplanner/OpenTripPlanner/issues/3650)
- Generalized graph connectivity pruning [#3426](https://github.com/opentripplanner/OpenTripPlanner/pull/3426)
- Stop linking to area/platform edges obeys area boundaries and traverse modes [#3201](https://github.com/opentripplanner/OpenTripPlanner/issues/3201)
- Add service day mapping to REST API [#3659](https://github.com/opentripplanner/OpenTripPlanner/pull/3659)
- Generalized cost on transfer in Raptor [#3629](https://github.com/opentripplanner/OpenTripPlanner/pull/3629)
- Add two new filters for use within grouping filter [#3638](https://github.com/opentripplanner/OpenTripPlanner/pull/3638)
- Correct usage of boardSlackForMode / alightSlackForMode [#3693](https://github.com/opentripplanner/OpenTripPlanner/pull/3693)
- Do not parse pass-through information in GBFS mappers [#3709](https://github.com/opentripplanner/OpenTripPlanner/pull/3709)
- Vehicle rental updates [#3632](https://github.com/opentripplanner/OpenTripPlanner/pull/3632)
- add trolleybus & monorail support [#3658](https://github.com/opentripplanner/OpenTripPlanner/pull/3658)
- Do not create zero length StreetEdges [#3716](https://github.com/opentripplanner/OpenTripPlanner/pull/3716)
- Add pickup and dropoff booking info to REST API [#3710](https://github.com/opentripplanner/OpenTripPlanner/pull/3710)
- Use the whole duration of the filtered transit data for the raptor heuristic search [#3664](https://github.com/opentripplanner/OpenTripPlanner/pull/3664)
- Performance improvement for flex access/egress searches [#3661](https://github.com/opentripplanner/OpenTripPlanner/pull/3661)
- Add new routing errors for cases where all itineraries were filtered by post-processing [#3628](https://github.com/opentripplanner/OpenTripPlanner/pull/3628)
- Fix combination of flex access and constrained transfer [#3726](https://github.com/opentripplanner/OpenTripPlanner/pull/3726)
- Merge B+R and P+R functionality into vehicle parking [#3480](https://github.com/opentripplanner/OpenTripPlanner/pull/3480)
- Add cost to maximize wait-time and avoid back-travel in optimize transfers [#3654](https://github.com/opentripplanner/OpenTripPlanner/pull/3654)
- Calculate fares from itineraries not Raptor paths, calculate flex fares [#3743](https://github.com/opentripplanner/OpenTripPlanner/pull/3743)
- Logging first time instance reports it is ready to use [#3733](https://github.com/opentripplanner/OpenTripPlanner/pull/3733)
- Allow limiting the used vehicle rentals and parkings [#3746](https://github.com/opentripplanner/OpenTripPlanner/pull/3746)
- Add support for car and scooter rental modes [#3706](https://github.com/opentripplanner/OpenTripPlanner/pull/3706)
- Extra stop priority cost in optimized transfer service [#3731](https://github.com/opentripplanner/OpenTripPlanner/pull/3731)
- Remove old visibility graph library from walkable area builder [#3753](https://github.com/opentripplanner/OpenTripPlanner/pull/3753)
- Update GtfsRealtime and include severity, effect and cause from GTFS RT [#3747](https://github.com/opentripplanner/OpenTripPlanner/pull/3747)
- Handle miscellaneous service as BUS instead of crashing build [#3755](https://github.com/opentripplanner/OpenTripPlanner/pull/3755)
- Update all timers to micrometer instances [#3744](https://github.com/opentripplanner/OpenTripPlanner/pull/3744)
- Bugfix: ClassCastException when planning flex routes [#3762](https://github.com/opentripplanner/OpenTripPlanner/pull/3762)
- Add mode from parent StopPlace for Quays in Netex mapper [#3751](https://github.com/opentripplanner/OpenTripPlanner/pull/3751)
- Minor performance improvements [#3767](https://github.com/opentripplanner/OpenTripPlanner/pull/3767)
- Parallelise computing of trip pattern geometries [#3766](https://github.com/opentripplanner/OpenTripPlanner/pull/3766)
- Add flex stop to TripTimes, return geometries in GraphQL API [#3757](https://github.com/opentripplanner/OpenTripPlanner/pull/3757)
- Fix checking allowed boarding/alighting for unscheduled flex trips [#3782](https://github.com/opentripplanner/OpenTripPlanner/pull/3782)
- Calculating number of days to use in StopTimes-request [#3742](https://github.com/opentripplanner/OpenTripPlanner/pull/3742)
- Walkable area builder improvements [#3765](https://github.com/opentripplanner/OpenTripPlanner/pull/3765)
- Remove hardcoded alighting/boarding on first/last stop [#3784](https://github.com/opentripplanner/OpenTripPlanner/pull/3784)
- Add support for include-file-directive in config files. [#3771](https://github.com/opentripplanner/OpenTripPlanner/pull/3771)
- Remove build parameter 'useTransfersTxt' [#3791](https://github.com/opentripplanner/OpenTripPlanner/pull/3791)
- Add cursor-based paging [#3759](https://github.com/opentripplanner/OpenTripPlanner/pull/3759)
- Data overlay sandbox feature [#3760](https://github.com/opentripplanner/OpenTripPlanner/pull/3760)
- Add support for sandboxed realtime vehicle parking updaters [#3796](https://github.com/opentripplanner/OpenTripPlanner/pull/3796)
- Add reading and exposing of Netex submodes [#3793](https://github.com/opentripplanner/OpenTripPlanner/pull/3793)
- Fix: Account for wait-time in no-wait Raptor strategy  [#3798](https://github.com/opentripplanner/OpenTripPlanner/pull/3798)
- Read in flex window from Netex feeds [#3800](https://github.com/opentripplanner/OpenTripPlanner/pull/3800)
- Fix NPE when routing on a graph without transit data. [#3804](https://github.com/opentripplanner/OpenTripPlanner/pull/3804)
- Read leg mode from trip, instead of route [#3819](https://github.com/opentripplanner/OpenTripPlanner/pull/3819)
- Use API mapping in snapshot tests [#3823](https://github.com/opentripplanner/OpenTripPlanner/pull/3823)
- Store stop indices in leg and use them to simplify logic in TripTimeShortHelper [#3820](https://github.com/opentripplanner/OpenTripPlanner/pull/3820)
- Include all trips in `stopTimesForStop` [#3817](https://github.com/opentripplanner/OpenTripPlanner/pull/3817)
- Store all alerts and add support for route_type and direction_id selectors [#3780](https://github.com/opentripplanner/OpenTripPlanner/pull/3780)
- Remove outdated realtime-update from TimetableSnapshot [#3770](https://github.com/opentripplanner/OpenTripPlanner/pull/3770)
- Contributing Guide [#3769](https://github.com/opentripplanner/OpenTripPlanner/pull/3769)
- OTP support for NeTEx branding [#3829](https://github.com/opentripplanner/OpenTripPlanner/pull/3829)
- Not allowed transfers and support for GTFS transfer points [#3792](https://github.com/opentripplanner/OpenTripPlanner/pull/3792)
- Simple implementation of horizontally moving elevators (Elevator way) [#3750](https://github.com/opentripplanner/OpenTripPlanner/pull/3750)
- fix: Avoid mixed path separators on Windows in Park API test [#3845](https://github.com/opentripplanner/OpenTripPlanner/pull/3845)
- Filter out elevator ways that are also implicit areas [#3850](https://github.com/opentripplanner/OpenTripPlanner/pull/3850)
- Dynamically compute additional search days [#3824](https://github.com/opentripplanner/OpenTripPlanner/pull/3824)
- Mode & submode filter for NeTEx Service Journeys [#3844](https://github.com/opentripplanner/OpenTripPlanner/pull/3844)
- Return correct heuristic values, when constrained transfers is turned on [#3841](https://github.com/opentripplanner/OpenTripPlanner/pull/3841)
- Adjust search window dynamically to fit the number of itineraries [#3828](https://github.com/opentripplanner/OpenTripPlanner/pull/3828)
- Implement minimum transfer time from GTFS transfers.txt [#3830](https://github.com/opentripplanner/OpenTripPlanner/pull/3830)
- Fix number format exception for elevator duration parsing OSM data [#3871](https://github.com/opentripplanner/OpenTripPlanner/pull/3871)
- Include generalizedCost in the optimized transfers wait-time cost [#3864](https://github.com/opentripplanner/OpenTripPlanner/pull/3864)
- Expose maxStopToShapeSnapDistance as build-config.json parameter [#3874](https://github.com/opentripplanner/OpenTripPlanner/pull/3874)
- Implement in-seat transfers per GTFS draft [#3831](https://github.com/opentripplanner/OpenTripPlanner/pull/3831)
- Add stairsTimeFactor to StreetEdge [#3832](https://github.com/opentripplanner/OpenTripPlanner/pull/3832)
- Make sure we keep the itinerary with the least number of transfers when grouping the itineraries [#3833](https://github.com/opentripplanner/OpenTripPlanner/pull/3833)
- Don't expect every pattern in a route to have the specified stop with constrained transfers [#3868](https://github.com/opentripplanner/OpenTripPlanner/pull/3868)
- Add support for creating constrained transfers from real-time generated patterns [#3878](https://github.com/opentripplanner/OpenTripPlanner/pull/3878)
- Account for stay seated transfers when calculating the number of transfers [#3888](https://github.com/opentripplanner/OpenTripPlanner/pull/3888)
- Add Hungarian translation [#3861](https://github.com/opentripplanner/OpenTripPlanner/pull/3861)
- Add support for "via" in NeTEx headsigns [#3883](https://github.com/opentripplanner/OpenTripPlanner/pull/3883)
- NeTEx mapping to StopTime.timepoint [#3898](https://github.com/opentripplanner/OpenTripPlanner/pull/3898)
- Optimize RAPTOR trip search by pre-calculating arrival/departure time arrays [#3919](https://github.com/opentripplanner/OpenTripPlanner/pull/3919)
- Make turn restrictions faster and thread-safe by moving them into StreetEdge [#3899](https://github.com/opentripplanner/OpenTripPlanner/pull/3899)
- Add routing using frequency trips [#3916](https://github.com/opentripplanner/OpenTripPlanner/pull/3916)
- Remove ET realtime override code [#3912](https://github.com/opentripplanner/OpenTripPlanner/pull/3912)
- Allow traversal of pathways without traversal time, distance or steps [#3910](https://github.com/opentripplanner/OpenTripPlanner/pull/3910)


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
- Removed the experimental TransportNetwork classes, which shared no code with the rest of OTP and 
  were duplicated in the R5 project. There are still some elements that can be cleaned out when only
  R5 is used by Conveyal's analysis system. The broker code in OTP is now able to start up R5 
  workers for Analyst.
- Use the Conveyal fork of the OBA GTFS loader, so that we can add our own extensions to GTFS.
- Updated docs to offer Conveyal Maven repo as a place to get prebuilt OTP.

## 0.19.0 (2016-05-25)

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
- Began moving documentation (including this changelog) into the OTP repo and rewriting it page by
  page. It is built statically from Markdown using mkdocs and published on readthedocs.
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
- Complete MapDB based GTFS and OSM loader libraries (will become separate projects, not yet
  connected to OTP graph builder)
- API documentation generation working again
- Disable some time consuming graph building steps by default
- Finnish and Swedish translations
- Subway-specific JSON configuration options (street to platform time)
- Realtime fetch / streaming configurable via JSON
- Stairs reluctance is much higher when carrying a bike
- Graph visualizer routing progress animates when a search is triggered via the web API
- Assume WGS84 (spherical distance calculations) everywhere
- Removed custom motor vehicle (which was unmaintained and not documented)
- Ability to poll for bike rental locations only once at startup
- Stoptimes are fetched for a specific service day in index API
- Bicycle triangle support in profile routing
- Proper handling of multiple access modes with different speeds in profile routing
- Command line option to output OTP's version

## 0.13.0 (2014-12-05)

- Detect apparent errors in GTFS interlining
- Long distance mode: use a pure weight-based state comparison, and use trip-banning retrying logic
  to get multiple paths. This compromises correctness somewhat but brings search times back within
  reason for large regional graphs. Also, we create significantly less SimpleTransfers.
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

This release was made to consolidate all the development that had occurred with a 0.9.x-SNAPSHOT
Maven version. The changes were very significant and it was not appropriate to tag them as a minor
bugfix release after the 0.9 tag. Though this release was performed at the same time as 0.11.0, it
represents a much earlier stage in the development of OTP.

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
