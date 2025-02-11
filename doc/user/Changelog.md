# Changelog

The changelog lists most feature changes between each release. The list is automatically created
based on merged pull requests. Search GitHub issues and pull requests for smaller issues.

## 2.7.0-SNAPSHOT (under development)

- Extra leg when transferring at the same stop [#5984](https://github.com/opentripplanner/OpenTripPlanner/pull/5984)
- Filter vector tiles stops by current service week [#6003](https://github.com/opentripplanner/OpenTripPlanner/pull/6003)
- Add a matcher API for filters in the transit service used for datedServiceJourneyQuery [#5713](https://github.com/opentripplanner/OpenTripPlanner/pull/5713)
- Refetch transit leg with a leg query of GTFS GraphQL API [#6045](https://github.com/opentripplanner/OpenTripPlanner/pull/6045)
- Remove deprecated support for GTFS flex stop areas [#6074](https://github.com/opentripplanner/OpenTripPlanner/pull/6074)
- Don't use elevation data directly for ways with cutting=*, location=underground or indoor=yes tags in the default mapper [#6093](https://github.com/opentripplanner/OpenTripPlanner/pull/6093)
- Un-deprecate GTFS API's `planConnection`, deprecate `plan` [#6110](https://github.com/opentripplanner/OpenTripPlanner/pull/6110)
- Support for routing to Station centroid instead of child stops [#6047](https://github.com/opentripplanner/OpenTripPlanner/pull/6047)
- Add via to the Transmodel trip query and make a proper Raptor implementation for it [#6084](https://github.com/opentripplanner/OpenTripPlanner/pull/6084)
- Fix GTFS-Flex duration offset and factor parsing when only one of them is set [#6138](https://github.com/opentripplanner/OpenTripPlanner/pull/6138)
- Fix arrive by filtering for on-street/flex itineraries [#6050](https://github.com/opentripplanner/OpenTripPlanner/pull/6050)
- Rename TransitModel to TransitRepository [#6148](https://github.com/opentripplanner/OpenTripPlanner/pull/6148)
- Clear added patterns in TimetableSnapshot [#6141](https://github.com/opentripplanner/OpenTripPlanner/pull/6141)
- Rename StopModel to SiteRepository [#6165](https://github.com/opentripplanner/OpenTripPlanner/pull/6165)
- Allow bike walking through bicycle no thru traffic areas [#6179](https://github.com/opentripplanner/OpenTripPlanner/pull/6179)
- update the description of  mode to cable tram. [#6173](https://github.com/opentripplanner/OpenTripPlanner/pull/6173)
- Change GraphQL tooltip for searchWindowUsed to say minutes, instead of seconds [#6185](https://github.com/opentripplanner/OpenTripPlanner/pull/6185)
- Interpret GTFS extended route types 801-899 also as trolleybus service [#6170](https://github.com/opentripplanner/OpenTripPlanner/pull/6170)
- Disable protocol upgrades for the HTTP client by default. [#6194](https://github.com/opentripplanner/OpenTripPlanner/pull/6194)
- Fix max search-window when paging [#6189](https://github.com/opentripplanner/OpenTripPlanner/pull/6189)
- Add car ferry functionality [#5966](https://github.com/opentripplanner/OpenTripPlanner/pull/5966)
- Make indoor=area and indoor=corridor routable for UK OSM tag mapper [#6119](https://github.com/opentripplanner/OpenTripPlanner/pull/6119)
- Revert  [#6214](https://github.com/opentripplanner/OpenTripPlanner/pull/6214)
- Remove reading agency and route brandingUrl from GTFS data [#6183](https://github.com/opentripplanner/OpenTripPlanner/pull/6183)
- Fix NullPointerException when searching backwards with a frequency-based trip [#6211](https://github.com/opentripplanner/OpenTripPlanner/pull/6211)
- Combine two multi-criteria searches in Raptor [#6182](https://github.com/opentripplanner/OpenTripPlanner/pull/6182)
- Implement alert node query in GTFS GraphQL API [#6225](https://github.com/opentripplanner/OpenTripPlanner/pull/6225)
- Fix hop geometries when one pattern is replaced by another with different number of stops [#6136](https://github.com/opentripplanner/OpenTripPlanner/pull/6136)
- Distinct coach from bus when reading in GTFS data and in GTFS GraphQL API [#6171](https://github.com/opentripplanner/OpenTripPlanner/pull/6171)
- Add provider of updates as a dimension to metrics. [#6199](https://github.com/opentripplanner/OpenTripPlanner/pull/6199)
- Return empty list if there is no siriUrls in situations/infoLinks [#6232](https://github.com/opentripplanner/OpenTripPlanner/pull/6232)
- OSM area processing obeys tag mapping  [#6164](https://github.com/opentripplanner/OpenTripPlanner/pull/6164)
- Add `via` to GTFS GraphQL API [#5958](https://github.com/opentripplanner/OpenTripPlanner/pull/5958)
- Deprecate old alert translations in the GTFS API and add language param to a few alert fields [#6216](https://github.com/opentripplanner/OpenTripPlanner/pull/6216)
- add stopPositionInPattern in Stoptime in GTFS GraphQL API [#6204](https://github.com/opentripplanner/OpenTripPlanner/pull/6204)
- Fix parsing of wheelchair accessible parking, add wheelchair debug layer [#6229](https://github.com/opentripplanner/OpenTripPlanner/pull/6229)
- Add previousLegs into GTFS GraphQL API [#6142](https://github.com/opentripplanner/OpenTripPlanner/pull/6142)
- Fix stop index filtering on ServiceJourney Transmodel GraphQL API [#6251](https://github.com/opentripplanner/OpenTripPlanner/pull/6251)
- Fix rental searches when destination is in a no-drop-off zone [#6233](https://github.com/opentripplanner/OpenTripPlanner/pull/6233)
- Include empty rail stops in transfers [#6208](https://github.com/opentripplanner/OpenTripPlanner/pull/6208)
- Relax rejection of GTFS flex trips that also contain continuous stopping [#6231](https://github.com/opentripplanner/OpenTripPlanner/pull/6231)
- Remove legacy bike access mapping [#6248](https://github.com/opentripplanner/OpenTripPlanner/pull/6248)
- Filter import of rental data by pickup type [#6240](https://github.com/opentripplanner/OpenTripPlanner/pull/6240)
- Apply stricter motor vehicle nothrough traffic rules in Finland [#6254](https://github.com/opentripplanner/OpenTripPlanner/pull/6254)
- Add `vehicleRentalsByBbox` query to GTFS GraphQL API [#6186](https://github.com/opentripplanner/OpenTripPlanner/pull/6186)
- Improve performance of speculative rental vehicle use in reverse search [#6260](https://github.com/opentripplanner/OpenTripPlanner/pull/6260)
- Fix problem with relaxed-generalized-cost-at-destination [#6255](https://github.com/opentripplanner/OpenTripPlanner/pull/6255)
- Reject SIRI-ET updates with empty StopPointRefs [#6266](https://github.com/opentripplanner/OpenTripPlanner/pull/6266)
- Allow GTFS fuzzy trip matching even when trip descriptor has an id [#6250](https://github.com/opentripplanner/OpenTripPlanner/pull/6250)
- Make `motorroad=yes` car-only [#6288](https://github.com/opentripplanner/OpenTripPlanner/pull/6288)
- Add decision record for analysis and design documentation [#6281](https://github.com/opentripplanner/OpenTripPlanner/pull/6281)
- Switch GTFS flex `safe_duration_offset` back to seconds [#6298](https://github.com/opentripplanner/OpenTripPlanner/pull/6298)
- Remove unused GtfsGraphQlApiRentalStationFuzzyMatching feature [#6282](https://github.com/opentripplanner/OpenTripPlanner/pull/6282)
- Make debug UI background layers configurable with new file `debug-ui-config.json` [#6295](https://github.com/opentripplanner/OpenTripPlanner/pull/6295)
- Better escalator duration control: specific duration from OSM duration tag, default speed from build-config.json [#6268](https://github.com/opentripplanner/OpenTripPlanner/pull/6268)
- Detect JSON array in addition to JSON objects when including a file in the config. [#6307](https://github.com/opentripplanner/OpenTripPlanner/pull/6307)
- Use WCAG recommendation to fill in GTFS route text color if it is missing [#6308](https://github.com/opentripplanner/OpenTripPlanner/pull/6308)
- Rename `otp-shaded-jar` artifact and fix deployment to Maven Central [#6331](https://github.com/opentripplanner/OpenTripPlanner/pull/6331)
- Add query for cancelled trips to GTFS GraphQL API [#5393](https://github.com/opentripplanner/OpenTripPlanner/pull/5393)
- Enable mode-specific transfers by storing mode information in transfers [#6293](https://github.com/opentripplanner/OpenTripPlanner/pull/6293)
- Add default penalty to all car API modes [#6302](https://github.com/opentripplanner/OpenTripPlanner/pull/6302)
- Make flex linking work together with boarding locations [#6311](https://github.com/opentripplanner/OpenTripPlanner/pull/6311)
- Add fallback name for corridors [#6303](https://github.com/opentripplanner/OpenTripPlanner/pull/6303)
- Implement SIRI Lite [#6284](https://github.com/opentripplanner/OpenTripPlanner/pull/6284)
- Add a matcher API for filters in the transit service used for regularStop lookup [#6234](https://github.com/opentripplanner/OpenTripPlanner/pull/6234)
- Make all polling updaters wait for graph update finish [#6262](https://github.com/opentripplanner/OpenTripPlanner/pull/6262)
- When using ScheduledTransitLeg's copy builder, also copy alerts [#6368](https://github.com/opentripplanner/OpenTripPlanner/pull/6368)
- Process boarding location for OSM ways (linear platforms) [#6247](https://github.com/opentripplanner/OpenTripPlanner/pull/6247)
- Fix `bookWhen` field is `null` in the Transmodel API [#6385](https://github.com/opentripplanner/OpenTripPlanner/pull/6385)
- Make it possible to add custom API documentation based on the deployment location [#6355](https://github.com/opentripplanner/OpenTripPlanner/pull/6355)
- If configured, add subway station entrances from OSM to walk steps [#6343](https://github.com/opentripplanner/OpenTripPlanner/pull/6343)
- Revert allow multiple states during transfer edge traversals [#6357](https://github.com/opentripplanner/OpenTripPlanner/pull/6357)
- Generate Raptor transfer cache in parallel [#6326](https://github.com/opentripplanner/OpenTripPlanner/pull/6326)
- Add 'transferParametersForMode' build config field [#6215](https://github.com/opentripplanner/OpenTripPlanner/pull/6215)
- Add 'maxStopCountForMode' to the router config [#6383](https://github.com/opentripplanner/OpenTripPlanner/pull/6383)
- Add all routing parameters to debug UI [#6370](https://github.com/opentripplanner/OpenTripPlanner/pull/6370)
- Add currentFuelPercent and currentRangeMeters to RentalVehichle in the GTFS GraphQL API [#6272](https://github.com/opentripplanner/OpenTripPlanner/pull/6272)
- Add a matcher API for filters in the transit service used for route lookup [#6378](https://github.com/opentripplanner/OpenTripPlanner/pull/6378)
- Use SIRI-ET and GTFS-RT TripUpdates at the same time [#6363](https://github.com/opentripplanner/OpenTripPlanner/pull/6363)
- Add 'boardCost' parameter for cars [#6413](https://github.com/opentripplanner/OpenTripPlanner/pull/6413)
- Fix processing of one-way boarding locations [#6410](https://github.com/opentripplanner/OpenTripPlanner/pull/6410)
- Allow SIRI's StopPointRef to refer to a NeTEx scheduled stop point [#6397](https://github.com/opentripplanner/OpenTripPlanner/pull/6397)
- Cleanup DefaultServerRequestContext [#6416](https://github.com/opentripplanner/OpenTripPlanner/pull/6416)
- Add support for alert metrics [#6415](https://github.com/opentripplanner/OpenTripPlanner/pull/6415)
- Replacing protobuf-format in SIRI-Updater with standard xml [#6409](https://github.com/opentripplanner/OpenTripPlanner/pull/6409)
- Add walk and bicycle safety debug layer, remove raster ones [#6434](https://github.com/opentripplanner/OpenTripPlanner/pull/6434)
- Add maximum slope debug layer [#6447](https://github.com/opentripplanner/OpenTripPlanner/pull/6447)
[](AUTOMATIC_CHANGELOG_PLACEHOLDER_DO_NOT_REMOVE)

## 2.6.0 (2024-09-18)

### Notable Changes

- ISO-8601 date time for GTFS GraphQL API itinerary responses [#5660](https://github.com/opentripplanner/OpenTripPlanner/pull/5660)
- Add support for query parameter to enforce booking time in trip search for flexible services [#5606](https://github.com/opentripplanner/OpenTripPlanner/pull/5606)
- New GTFS GraphQL plan query [#5185](https://github.com/opentripplanner/OpenTripPlanner/pull/5185)
- Make new debug client the default, move old one to `classic-debug` [#5924](https://github.com/opentripplanner/OpenTripPlanner/pull/5924) [#5962](https://github.com/opentripplanner/OpenTripPlanner/pull/5962) [#6012](https://github.com/opentripplanner/OpenTripPlanner/pull/6012) [#6053](https://github.com/opentripplanner/OpenTripPlanner/pull/6053)
- Disable Legacy REST API by default [#5948](https://github.com/opentripplanner/OpenTripPlanner/pull/5948)

### Detailed changes by Pull Request

- SIRI real time improvements and bug fixes [#5867](https://github.com/opentripplanner/OpenTripPlanner/pull/5867) [#5931](https://github.com/opentripplanner/OpenTripPlanner/pull/5931) [#5865](https://github.com/opentripplanner/OpenTripPlanner/pull/5865)
- Real time improvements and bug fixes [#5726](https://github.com/opentripplanner/OpenTripPlanner/pull/5726) [#5941](https://github.com/opentripplanner/OpenTripPlanner/pull/5941) [#6007](https://github.com/opentripplanner/OpenTripPlanner/pull/6007)
- Fix street routing on roundabout [#5732](https://github.com/opentripplanner/OpenTripPlanner/pull/5732)
- Expose route sort order in GTFS API [#5764](https://github.com/opentripplanner/OpenTripPlanner/pull/5764)
- Fix issue with cancellations on trip patterns that run after midnight [#5719](https://github.com/opentripplanner/OpenTripPlanner/pull/5719)
- Discourage instead of ban cycling on use_sidepath ways and do the same for walking on foot=use_sidepath [#5790](https://github.com/opentripplanner/OpenTripPlanner/pull/5790)
- Prune islands with mode-less stop vertices [#5782](https://github.com/opentripplanner/OpenTripPlanner/pull/5782)
- Overwrite default WALK directMode when it is not set in the request, but modes is set [#5779](https://github.com/opentripplanner/OpenTripPlanner/pull/5779)
- Fix trip duplication in Graph Builder DSJ mapping [#5794](https://github.com/opentripplanner/OpenTripPlanner/pull/5794)
- Fix bug in heuristics cost calculation for egress legs [#5783](https://github.com/opentripplanner/OpenTripPlanner/pull/5783)
- Fix handling of implicit access and egress mode parameters [#5821](https://github.com/opentripplanner/OpenTripPlanner/pull/5821)
- Namer for applying street names to nearby sidewalks [#5774](https://github.com/opentripplanner/OpenTripPlanner/pull/5774)
- Implement GTFS Flex safe duration spec draft [#5796](https://github.com/opentripplanner/OpenTripPlanner/pull/5796)
- Add OTP request timeout GraphQL instrumentation [#5881](https://github.com/opentripplanner/OpenTripPlanner/pull/5881)
- Add feed publisher name and url to GTFS GraphQL API [#5835](https://github.com/opentripplanner/OpenTripPlanner/pull/5835)
- Fix parsing of GBFS feeds [#5891](https://github.com/opentripplanner/OpenTripPlanner/pull/5891)
- Limit result size and execution time in Transmodel GraphQL API [#5883](https://github.com/opentripplanner/OpenTripPlanner/pull/5883)
- Remove TravelTime API [#5890](https://github.com/opentripplanner/OpenTripPlanner/pull/5890)
- Improve cancellation of large response in Transmodel API [#5908](https://github.com/opentripplanner/OpenTripPlanner/pull/5908)
- Require valid polygons for AreaStop [#5915](https://github.com/opentripplanner/OpenTripPlanner/pull/5915)
- Fix NullPointerException in stop transfer priority cost vector generation [#5943](https://github.com/opentripplanner/OpenTripPlanner/pull/5943)
- Convert transferSlack configuration to duration [#5897](https://github.com/opentripplanner/OpenTripPlanner/pull/5897)
- Expose stop transfer priority in Transmodel API [#5942](https://github.com/opentripplanner/OpenTripPlanner/pull/5942)
- Add rental system to GTFS GraphQL API [#5909](https://github.com/opentripplanner/OpenTripPlanner/pull/5909)
- Generate documentation for OSM tag mappers [#5929](https://github.com/opentripplanner/OpenTripPlanner/pull/5929)
- Enforce non-null coordinates on multimodal station [#5971](https://github.com/opentripplanner/OpenTripPlanner/pull/5971)
- Add car rental to Transmodel street mode options [#5977](https://github.com/opentripplanner/OpenTripPlanner/pull/5977)
- Handle NeTEx `any` version [#5983](https://github.com/opentripplanner/OpenTripPlanner/pull/5983)
- Keep at least one result for min-transfers and each transit-group in itinerary-group-filter [#5919](https://github.com/opentripplanner/OpenTripPlanner/pull/5919)
- Extract parking lots from NeTEx feeds [#5946](https://github.com/opentripplanner/OpenTripPlanner/pull/5946)
- Filter routes and patterns by service date in GTFS GraphQL API [#5869](https://github.com/opentripplanner/OpenTripPlanner/pull/5869)
- SIRI-FM vehicle parking updates [#5979](https://github.com/opentripplanner/OpenTripPlanner/pull/5979)
- Take realtime patterns into account when storing realtime vehicles [#5994](https://github.com/opentripplanner/OpenTripPlanner/pull/5994)
- Developer Decision Records [#5932](https://github.com/opentripplanner/OpenTripPlanner/pull/5932)
- Allow NeTEx ServiceJourneyPatterns with stopUse=passthrough [#6037](https://github.com/opentripplanner/OpenTripPlanner/pull/6037)
- Use correct HEAD request when fetching HTTP headers [#6063](https://github.com/opentripplanner/OpenTripPlanner/pull/6063)
- Fix incorrect mapping of flex safe duration offset [#6059](https://github.com/opentripplanner/OpenTripPlanner/pull/6059)

## 2.5.0 (2024-03-13)

### Notable Changes

- Make GTFS GraphQL API an official API [#5339](https://github.com/opentripplanner/OpenTripPlanner/pull/5339)
- Make Transmodel GraphQl API an official API [#5573](https://github.com/opentripplanner/OpenTripPlanner/pull/5573), [#5637](https://github.com/opentripplanner/OpenTripPlanner/pull/5637)
- Deprecate REST API [#5580](https://github.com/opentripplanner/OpenTripPlanner/pull/5580)
- Transit group priority [#4999](https://github.com/opentripplanner/OpenTripPlanner/pull/4999), [#5583](https://github.com/opentripplanner/OpenTripPlanner/pull/5583), [#5638](https://github.com/opentripplanner/OpenTripPlanner/pull/5638)
- Transmodel GraphQL API for pass-through searches [#5320](https://github.com/opentripplanner/OpenTripPlanner/pull/5320)
- Migrate to Java 21 [#5421](https://github.com/opentripplanner/OpenTripPlanner/pull/5421)
- New debug client [#5334](https://github.com/opentripplanner/OpenTripPlanner/pull/5334)
- Update to latest GTFS Flex spec draft [#5564](https://github.com/opentripplanner/OpenTripPlanner/pull/5564), [#5655](https://github.com/opentripplanner/OpenTripPlanner/pull/5655)
- Restructure walk/bicycle/car preferences in router-config.json [#5582](https://github.com/opentripplanner/OpenTripPlanner/pull/5582)

### Detailed changes by Pull Request

- Gracefully handle nullable fields in TransitAlert [#5349](https://github.com/opentripplanner/OpenTripPlanner/pull/5349)
- Remove transit with higher cost than best on-street itinerary filter [#5222](https://github.com/opentripplanner/OpenTripPlanner/pull/5222)
- Remove `banDiscouragedCycling` and `banDiscouragedWalking` [#5341](https://github.com/opentripplanner/OpenTripPlanner/pull/5341)
- Fix rental scooter access [#5361](https://github.com/opentripplanner/OpenTripPlanner/pull/5361)
- De-duplicate stops returned by `stopsByRadius` [#5366](https://github.com/opentripplanner/OpenTripPlanner/pull/5366)
- Fix value mapping for `bikesAllowed` in GTFS GraphQL API [#5368](https://github.com/opentripplanner/OpenTripPlanner/pull/5368)
- Apply correct traversal permissions to barrier vertex [#5369](https://github.com/opentripplanner/OpenTripPlanner/pull/5369)
- Fix check for OSM relation members not being present in the extract [#5379](https://github.com/opentripplanner/OpenTripPlanner/pull/5379)
- Add a configurable limit for the search window [#5293](https://github.com/opentripplanner/OpenTripPlanner/pull/5293)
- Fix board slack list mapping in Transmodel API [#5420](https://github.com/opentripplanner/OpenTripPlanner/pull/5420)
- Fix flexible quay querying in Transmodel API [#5417](https://github.com/opentripplanner/OpenTripPlanner/pull/5417)
- Add validation for missing calls in SIRI update [#5403](https://github.com/opentripplanner/OpenTripPlanner/pull/5403)
- Import Occupancy Status from GTFS-RT Vehicle Positions [#5372](https://github.com/opentripplanner/OpenTripPlanner/pull/5372)
- Allow multiple zones in an unscheduled flex trip [#5376](https://github.com/opentripplanner/OpenTripPlanner/pull/5376)
- Filter out null, empty and blank elements when mapping feed-scoped ids [#5428](https://github.com/opentripplanner/OpenTripPlanner/pull/5428)
- Validate stop id in Transit leg reference [#5440](https://github.com/opentripplanner/OpenTripPlanner/pull/5440)
- Add available types and spaces to `VehicleRentalStation` [#5425](https://github.com/opentripplanner/OpenTripPlanner/pull/5425)
- Make vehicleRentalStation query optionally accept id without feed [#5411](https://github.com/opentripplanner/OpenTripPlanner/pull/5411)
- Add stricter validation for flex areas [#5457](https://github.com/opentripplanner/OpenTripPlanner/pull/5457)
- Remove HTTPS handling and its documentation [#5439](https://github.com/opentripplanner/OpenTripPlanner/pull/5439)
- Add support for DSJ in transit leg reference [#5455](https://github.com/opentripplanner/OpenTripPlanner/pull/5455)
- Fix sort order bug in optimized transfers [#5446](https://github.com/opentripplanner/OpenTripPlanner/pull/5446)
- SIRI file loader [#5460](https://github.com/opentripplanner/OpenTripPlanner/pull/5460)
- Calculate CO₂ emissions of itineraries [#5278](https://github.com/opentripplanner/OpenTripPlanner/pull/5278)
- Interpolate increasing stop times for GTFS-RT cancelled trips [#5348](https://github.com/opentripplanner/OpenTripPlanner/pull/5348)
- Remove itineraries outside the search window in arriveBy search [#5433](https://github.com/opentripplanner/OpenTripPlanner/pull/5433)
- Add back walk reluctance in Transmodel API [#5471](https://github.com/opentripplanner/OpenTripPlanner/pull/5471)
- Improve linking of fixed stops used by flex trips [#5503](https://github.com/opentripplanner/OpenTripPlanner/pull/5503)
- Keep min transfer filter is not local to group-by-filters [#5436](https://github.com/opentripplanner/OpenTripPlanner/pull/5436)
- Add paging deduplication when cropping [#5458](https://github.com/opentripplanner/OpenTripPlanner/pull/5458)
- Check transport mode when mapping GroupStops [#5518](https://github.com/opentripplanner/OpenTripPlanner/pull/5518)
- Transfer cost limit [#5516](https://github.com/opentripplanner/OpenTripPlanner/pull/5516)
- Fix missed trip when arrive-by search-window is off by one minute [#5520](https://github.com/opentripplanner/OpenTripPlanner/pull/5520)
- Remove `matchBusRoutesToStreets` [#5523](https://github.com/opentripplanner/OpenTripPlanner/pull/5523)
- Add same submode in alternative legs filter [#5548](https://github.com/opentripplanner/OpenTripPlanner/pull/5548)
- Fix issue where stop points are sometimes added twice to index [#5552](https://github.com/opentripplanner/OpenTripPlanner/pull/5552)
- Improve shutdown logic [#5514](https://github.com/opentripplanner/OpenTripPlanner/pull/5514)
- Create TripOnServiceDate for new SIRI real-time servicejourneys [#5542](https://github.com/opentripplanner/OpenTripPlanner/pull/5542)
- Improve paging - avoid duplicates and missed itineraries when paging [#5551](https://github.com/opentripplanner/OpenTripPlanner/pull/5551)
- Add option to include stations in `nearest` search [#5390](https://github.com/opentripplanner/OpenTripPlanner/pull/5390)
- Report NO_TRANSIT_CONNECTION when search-window is set [#5570](https://github.com/opentripplanner/OpenTripPlanner/pull/5570)
- Fix preference cost comparisons [#5586](https://github.com/opentripplanner/OpenTripPlanner/pull/5586)
- Consider escalator edges in island pruning [#5591](https://github.com/opentripplanner/OpenTripPlanner/pull/5591)
- Adding situation-version to TransmodelGraphQL API [#5592](https://github.com/opentripplanner/OpenTripPlanner/pull/5592)
- Fix high walk reluctance leading to zero egress results for rental searches [#5605](https://github.com/opentripplanner/OpenTripPlanner/pull/5605)
- Remove GTFS-RT websocket updater [#5604](https://github.com/opentripplanner/OpenTripPlanner/pull/5604)
- Use fallback timezone if no transit data is loaded [#4652](https://github.com/opentripplanner/OpenTripPlanner/pull/4652)
- Add new path for GTFS GraphQL API, remove batch feature [#5581](https://github.com/opentripplanner/OpenTripPlanner/pull/5581)
- Remove `FareComponent` [#5613](https://github.com/opentripplanner/OpenTripPlanner/pull/5613)
- Add flexibleArea to GroupStop Quays [#5625](https://github.com/opentripplanner/OpenTripPlanner/pull/5625)
- Introduce `generalizedCostPlusPenalty` to make cost comparison fairer [#5483](https://github.com/opentripplanner/OpenTripPlanner/pull/5483)
- Separate walk time from non-transit time [#5648](https://github.com/opentripplanner/OpenTripPlanner/pull/5648)
- Remove "fare" [#5645](https://github.com/opentripplanner/OpenTripPlanner/pull/5645)
- Remove `VehicleToStopHeuristics` [#5381](https://github.com/opentripplanner/OpenTripPlanner/pull/5381)
- Set defaults of the modes WALK, even if one and not the others are set [#5675](https://github.com/opentripplanner/OpenTripPlanner/pull/5675)
- Reduce flex default access/egress penalty [#5674](https://github.com/opentripplanner/OpenTripPlanner/pull/5674)
- Add scooter preferences [#5632](https://github.com/opentripplanner/OpenTripPlanner/pull/5632)
- Use NeTEx authority short name if name is not present [#5698](https://github.com/opentripplanner/OpenTripPlanner/pull/5698)
- Add Hamburg OSM mapper [#5701](https://github.com/opentripplanner/OpenTripPlanner/pull/5701)
- Remove configurable car speed and determine it in graph build [#5657](https://github.com/opentripplanner/OpenTripPlanner/pull/5657)
- Avoid cumulative real-time updates [#5705](https://github.com/opentripplanner/OpenTripPlanner/pull/5705)
- Fix time penalty [#5715](https://github.com/opentripplanner/OpenTripPlanner/pull/5715)
- Fix world envelope builder when crossing Greenwich meridian [#5731](https://github.com/opentripplanner/OpenTripPlanner/pull/5731)

## 2.4.0 (2023-09-13)

### Notable Changes

- Improved support for Fares V2 [#4917](https://github.com/opentripplanner/OpenTripPlanner/pull/4917) [#5227](https://github.com/opentripplanner/OpenTripPlanner/pull/5227) 
- Improved error and timeout handling [#5047](https://github.com/opentripplanner/OpenTripPlanner/pull/5047) [#5092](https://github.com/opentripplanner/OpenTripPlanner/pull/5092) [#5121](https://github.com/opentripplanner/OpenTripPlanner/pull/5121) [#5114](https://github.com/opentripplanner/OpenTripPlanner/pull/5114) [#5133](https://github.com/opentripplanner/OpenTripPlanner/pull/5133) [#5130](https://github.com/opentripplanner/OpenTripPlanner/pull/5130) [#5192](https://github.com/opentripplanner/OpenTripPlanner/pull/5192)
- Enable GTFS GraphQL API by default, remove the word "legacy" from its name and documentation [#5202](https://github.com/opentripplanner/OpenTripPlanner/pull/5202)
- Access and egress penalty on time and cost [#5180](https://github.com/opentripplanner/OpenTripPlanner/pull/5180)
- Improve support for GBFS geofencing zones [#5201](https://github.com/opentripplanner/OpenTripPlanner/pull/5201)
- Reduce memory consumption by 5-8% [#5223](https://github.com/opentripplanner/OpenTripPlanner/pull/5223)
- Stop count limit for access/egress routing and new accessEgress configuration object [#5214](https://github.com/opentripplanner/OpenTripPlanner/pull/5214)

### Detailed changes by Pull Request

- Generate static documentation for GTFS GraphQL API [#5069](https://github.com/opentripplanner/OpenTripPlanner/pull/5069)
- Create a valid area even when it has too many vertices [#5019](https://github.com/opentripplanner/OpenTripPlanner/pull/5019)
- Constant speed street routing [#5057](https://github.com/opentripplanner/OpenTripPlanner/pull/5057)
- Remove dead build configuration parameter (extraEdgesStopPlatformLink) [#5080](https://github.com/opentripplanner/OpenTripPlanner/pull/5080)
- Configure HTTP WebServer Transaction timeouts [#5047](https://github.com/opentripplanner/OpenTripPlanner/pull/5047)
- Improve Graph updaters shutdown [#5092](https://github.com/opentripplanner/OpenTripPlanner/pull/5092)
- Fix flex timeshift [#5063](https://github.com/opentripplanner/OpenTripPlanner/pull/5063)
- Merge norway traversal calculator into default [#5106](https://github.com/opentripplanner/OpenTripPlanner/pull/5106)
- Use correct GTFS sequence number in vehicle position matcher [#5090](https://github.com/opentripplanner/OpenTripPlanner/pull/5090)
- OSM: Break out of processing a malformed level map [#5096](https://github.com/opentripplanner/OpenTripPlanner/pull/5096)
- Handle JsonParseException [#5121](https://github.com/opentripplanner/OpenTripPlanner/pull/5121)
- Do not enforce API processing timeout for parallel routing [#5114](https://github.com/opentripplanner/OpenTripPlanner/pull/5114)
- Rename bikeRental options to vehicleRental [#5089](https://github.com/opentripplanner/OpenTripPlanner/pull/5089)
- Fare sandbox cleanup, remove MultipleFareService [#5100](https://github.com/opentripplanner/OpenTripPlanner/pull/5100)
- Add validation of NeTEx timetabled passing times [#5081](https://github.com/opentripplanner/OpenTripPlanner/pull/5081)
- Return WALKING_BETTER_THAN_TRANSIT only on a fully walking leg [#5091](https://github.com/opentripplanner/OpenTripPlanner/pull/5091)
- Handle CoercingParseValueException [#5133](https://github.com/opentripplanner/OpenTripPlanner/pull/5133)
- Remove broken Jersey tracing configuration [#5142](https://github.com/opentripplanner/OpenTripPlanner/pull/5142)
- Graceful timeout error handling [#5130](https://github.com/opentripplanner/OpenTripPlanner/pull/5130)
- Log http request headers - like correlationId [#5131](https://github.com/opentripplanner/OpenTripPlanner/pull/5131)
- Fix vertex removal race condition [#5141](https://github.com/opentripplanner/OpenTripPlanner/pull/5141)
- Comment out replacing DSJ-ID from planned data with ID from real-time-data [#5140](https://github.com/opentripplanner/OpenTripPlanner/pull/5140)
- Remove San Francisco and vehicle rental fare calculators [#5145](https://github.com/opentripplanner/OpenTripPlanner/pull/5145)
- Remove batch query from Transmodel API [#5147](https://github.com/opentripplanner/OpenTripPlanner/pull/5147)
- Fix nullable absolute direction in GTFS GraphQL API [#5159](https://github.com/opentripplanner/OpenTripPlanner/pull/5159)
- Fix error in flex validation [#5161](https://github.com/opentripplanner/OpenTripPlanner/pull/5161)
- Check service dates instead of service ids for block id transfers [#5162](https://github.com/opentripplanner/OpenTripPlanner/pull/5162)
- Add support for mapping NeTEx operating day in operating period [#5167](https://github.com/opentripplanner/OpenTripPlanner/pull/5167)
- Validate to/from in routing request [#5164](https://github.com/opentripplanner/OpenTripPlanner/pull/5164)
- Changing default value for earlyStartSec [#5165](https://github.com/opentripplanner/OpenTripPlanner/pull/5165)
- Add GTFS stop sequence to GTFS GraphQL API [#5153](https://github.com/opentripplanner/OpenTripPlanner/pull/5153)
- Remove walk leg in a stay seated transfer [#5135](https://github.com/opentripplanner/OpenTripPlanner/pull/5135)
- Validate distinct from/to temporary vertices [#5181](https://github.com/opentripplanner/OpenTripPlanner/pull/5181)
- Add support for NeTEx taxi mode [#5183](https://github.com/opentripplanner/OpenTripPlanner/pull/5183)
- Fix bike triangle in Transmodel API [#5179](https://github.com/opentripplanner/OpenTripPlanner/pull/5179)
- Bug fixes in stop area relation processing [#5166](https://github.com/opentripplanner/OpenTripPlanner/pull/5166)
- Allow underscores in GTFS feed IDs [#5191](https://github.com/opentripplanner/OpenTripPlanner/pull/5191)
- Area vertex linking improvements [#5209](https://github.com/opentripplanner/OpenTripPlanner/pull/5209)
- Allow multiple FlexibleAreas in a FlexibleStopPlace [#4922](https://github.com/opentripplanner/OpenTripPlanner/pull/4922)
- Prevent NPE in vehicle position matching [#5212](https://github.com/opentripplanner/OpenTripPlanner/pull/5212)
- Empty stop_headsign will fall back to trip_headsign [#5205](https://github.com/opentripplanner/OpenTripPlanner/pull/5205)
- Treat escalator differently from stairs, add escalator reluctance [#5046](https://github.com/opentripplanner/OpenTripPlanner/pull/5046)
- Flex build time and memory optimization for large zones [#5233](https://github.com/opentripplanner/OpenTripPlanner/pull/5233)
- Fix pathway traversal time calculation when none is supplied [#5242](https://github.com/opentripplanner/OpenTripPlanner/pull/5242)
- Add check for null value of serviceCodesRunning in TripPatternForDateMapper [#5239](https://github.com/opentripplanner/OpenTripPlanner/pull/5239)
- Improve error handling in TransmodelGraph [#5192](https://github.com/opentripplanner/OpenTripPlanner/pull/5192)
- Fix filtering by submode [#5261](https://github.com/opentripplanner/OpenTripPlanner/pull/5261)
- Add leg.headsign to GTFS GraphQL API [#5290](https://github.com/opentripplanner/OpenTripPlanner/pull/5290)
- Return client error for invalid Transmodel query JSON format [#5277](https://github.com/opentripplanner/OpenTripPlanner/pull/5277)
- Validate missing intermediate location in via requests [#5253](https://github.com/opentripplanner/OpenTripPlanner/pull/5253)
- Support Fares v2 FareMedium and update spec implementation [#5227](https://github.com/opentripplanner/OpenTripPlanner/pull/5227)
- Improve walk step narrative for entering/exiting stations and signposted pathways [#5285](https://github.com/opentripplanner/OpenTripPlanner/pull/5285)
- Fix walk board cost comparison and add escalatorReluctance to hash [#5310](https://github.com/opentripplanner/OpenTripPlanner/pull/5310)
- Remove pathway id from REST API [#5303](https://github.com/opentripplanner/OpenTripPlanner/pull/5303)
- Remove Winkki street note updater [#5305](https://github.com/opentripplanner/OpenTripPlanner/pull/5305)
- Extend stop area relation linking to include bus stop and platform nodes [#5319](https://github.com/opentripplanner/OpenTripPlanner/pull/5319)
- Add access/egress penalty transmodel api [#5268](https://github.com/opentripplanner/OpenTripPlanner/pull/5268)


## 2.3.0 (2023-04-24)

### Notable Changes

- The performance for trip search of large networks (> 100k stops) is improved
- Run only a single heuristics request (instead of two) [#4537](https://github.com/opentripplanner/OpenTripPlanner/pull/4537)
- Parse NeTEx fare zones from a FareFrame [#4563](https://github.com/opentripplanner/OpenTripPlanner/pull/4563)
- Initial implementation of via search [#4554](https://github.com/opentripplanner/OpenTripPlanner/pull/4554)
- Relaunched documentation theme [#4575](https://github.com/opentripplanner/OpenTripPlanner/pull/4575)
- Add carpool and taxi modes [#4641](https://github.com/opentripplanner/OpenTripPlanner/pull/4641)
- New filter API [#4657](https://github.com/opentripplanner/OpenTripPlanner/pull/4657)
- Adaptive street graph island pruning [#4688](https://github.com/opentripplanner/OpenTripPlanner/pull/4688)
- Experimental support for GBFS geofencing zones [#4741](https://github.com/opentripplanner/OpenTripPlanner/pull/4741)
- Update GBFS support to v2.3 [#4809](https://github.com/opentripplanner/OpenTripPlanner/pull/4809)
- Improve support for EPIP NeTex profile [#4863](https://github.com/opentripplanner/OpenTripPlanner/pull/4863)
- Uber ride hailing [#4979](https://github.com/opentripplanner/OpenTripPlanner/pull/4979)

### Detailed changes by Pull Request

- Refactoring SIRI StopConditions from TransitAlert to EntitySelector [#4196](https://github.com/opentripplanner/OpenTripPlanner/pull/4196)
- Add cost of egress leg to the cost of the last transit leg if transit leg arrives at destination [#4547](https://github.com/opentripplanner/OpenTripPlanner/pull/4547)
- Add fallback for missing operator name [#4588](https://github.com/opentripplanner/OpenTripPlanner/pull/4588)
- Add performance test for Switzerland [#4576](https://github.com/opentripplanner/OpenTripPlanner/pull/4576)
- Fix interchange generation when identical consecutive stops are filtered [#4586](https://github.com/opentripplanner/OpenTripPlanner/pull/4586)
- Add alerts to leg when reading in a leg reference [#4595](https://github.com/opentripplanner/OpenTripPlanner/pull/4595)
- Remove KML bike parking updater [#4602](https://github.com/opentripplanner/OpenTripPlanner/pull/4602)
- Add stationTransferPreference option for GTFS feeds [#4599](https://github.com/opentripplanner/OpenTripPlanner/pull/4599)
- Fix osmDefaults so they are used if something is not set in source [#4635](https://github.com/opentripplanner/OpenTripPlanner/pull/4635)
- Add demDefaults [#4637](https://github.com/opentripplanner/OpenTripPlanner/pull/4637)
- Add vector tile layer in debug client for AreaStops [#4565](https://github.com/opentripplanner/OpenTripPlanner/pull/4565)
- Prevent bicycles from using stairs [#4614](https://github.com/opentripplanner/OpenTripPlanner/pull/4614)
- Bugfix for interchanges that start and end from the same stop [#4597](https://github.com/opentripplanner/OpenTripPlanner/pull/4597)
- Add option to define gtfsDefaults and move some parameters from root to gtfs config [#4624](https://github.com/opentripplanner/OpenTripPlanner/pull/4624)
- Fix: forBoarding and forAlighting on cancelled calls in transmodel api [#4639](https://github.com/opentripplanner/OpenTripPlanner/pull/4639)
- Make maxFlexTripDuration configurable [#4642](https://github.com/opentripplanner/OpenTripPlanner/pull/4642)
- Extract separate conditions for "and", "greater than", "less than" during OSM tag mapping, add Portland tag mapping [#4593](https://github.com/opentripplanner/OpenTripPlanner/pull/4593)
- Better fallback in shape geometry processing [#4617](https://github.com/opentripplanner/OpenTripPlanner/pull/4617)
- Fix batch queries in HSL graphql API [#4663](https://github.com/opentripplanner/OpenTripPlanner/pull/4663)
- Add filter for minimum bike parking distance [#4626](https://github.com/opentripplanner/OpenTripPlanner/pull/4626)
- Add limits for flex access/egress walking [#4655](https://github.com/opentripplanner/OpenTripPlanner/pull/4655)
- Make Prometheus endpoint take Accept header into account [#4674](https://github.com/opentripplanner/OpenTripPlanner/pull/4674)
- Add entrances to parking lots that are unconnected in OSM [#4666](https://github.com/opentripplanner/OpenTripPlanner/pull/4666)
- Include crossing=traffic_signals and use it for walking/cycling penalty on crossings [#4574](https://github.com/opentripplanner/OpenTripPlanner/pull/4574)
- Replace surface=cobblestone:flattened with surface=set as a mixin spec in default OsmTagMapper [#4683](https://github.com/opentripplanner/OpenTripPlanner/pull/4683)
- Remove NYC fare service [#4694](https://github.com/opentripplanner/OpenTripPlanner/pull/4694)
- Upgrade dependencies to take new spelling of GTFS Flex's 'drop off' into account [#4693](https://github.com/opentripplanner/OpenTripPlanner/pull/4693)
- Configuration of additional HTTP headers for GTFS-RT updaters [#4684](https://github.com/opentripplanner/OpenTripPlanner/pull/4684)
- Automatic Java dependency upgrades [#4704](https://github.com/opentripplanner/OpenTripPlanner/pull/4704)
- GTFS-RT extension to add completely new routes [#4667](https://github.com/opentripplanner/OpenTripPlanner/pull/4667)
- Add validation on NeTEx flexible area import [#4765](https://github.com/opentripplanner/OpenTripPlanner/pull/4765)
- Do not return a leg from a leg reference, if trip does not exist on date [#4758](https://github.com/opentripplanner/OpenTripPlanner/pull/4758)
- Fix direct transfer analyzer [#4767](https://github.com/opentripplanner/OpenTripPlanner/pull/4767)
- Refactor data import issue reporting [#4777](https://github.com/opentripplanner/OpenTripPlanner/pull/4777)
- Add support for deleted trips & including real-time cancellations in trip search [#4759](https://github.com/opentripplanner/OpenTripPlanner/pull/4759)
- Fix TPoint generation for real-time generated patterns [#4787](https://github.com/opentripplanner/OpenTripPlanner/pull/4787)
- Improve resiliency of the Transmodel API [#4806](https://github.com/opentripplanner/OpenTripPlanner/pull/4806)
- Use TripPatterns for filtering [#4820](https://github.com/opentripplanner/OpenTripPlanner/pull/4820)
- Remove all edges from stop vertex in island pruning [#4846](https://github.com/opentripplanner/OpenTripPlanner/pull/4846)
- Filter functionality for GroupOfLines/GroupOfRoutes in TransmodelAPI [#4812](https://github.com/opentripplanner/OpenTripPlanner/pull/4812)
- Mapping for maxAccessEgressDurationPerMode in Transmodel API [#4829](https://github.com/opentripplanner/OpenTripPlanner/pull/4829)
- Use headsign from the original pattern in a real-time added pattern if the stop sequence is unchanged [#4845](https://github.com/opentripplanner/OpenTripPlanner/pull/4845)
- Remove RouteMatcher [#4821](https://github.com/opentripplanner/OpenTripPlanner/pull/4821)
- Improve boarding location linking on platforms [#4852](https://github.com/opentripplanner/OpenTripPlanner/pull/4852)
- Always check allowed modes in VehicleRentalEdge [#4810](https://github.com/opentripplanner/OpenTripPlanner/pull/4810)
- Include cancelled stops when including cancelled trips in trip search [#4851](https://github.com/opentripplanner/OpenTripPlanner/pull/4851)
- Unpreferred vehicle parking tags [#4873](https://github.com/opentripplanner/OpenTripPlanner/pull/4873)
- Ignore UnrestrictedPublicTransportAreas that do not contain regular stops [#4919](https://github.com/opentripplanner/OpenTripPlanner/pull/4919)
- Validate missing transit data before building transit graph [#4930](https://github.com/opentripplanner/OpenTripPlanner/pull/4930)
- Use durations for Raptor search window configuration [#4926](https://github.com/opentripplanner/OpenTripPlanner/pull/4926)
- Add graph build issue statistics to metrics endpoint [#4869](https://github.com/opentripplanner/OpenTripPlanner/pull/4869)
- Add documentation about system requirements and suggestions [#4937](https://github.com/opentripplanner/OpenTripPlanner/pull/4937)
- Fix initial A* state of car rental [#4934](https://github.com/opentripplanner/OpenTripPlanner/pull/4934)
- Remove check for (now optional) ENV-variable for GCP Authentication [#4966](https://github.com/opentripplanner/OpenTripPlanner/pull/4966)
- Improved walking routes when walking starts or ends in an area [#4936](https://github.com/opentripplanner/OpenTripPlanner/pull/4936)
- Remove unconnected walking areas from graph [#4981](https://github.com/opentripplanner/OpenTripPlanner/pull/4981)
- Filter out duplicate-like legs in alternative legs [#4868](https://github.com/opentripplanner/OpenTripPlanner/pull/4868)
- Fix timeshifting of Flex ~ Walk ~ Flex paths in Raptor [#4952](https://github.com/opentripplanner/OpenTripPlanner/pull/4952)
- Validate the number of service links mapped to a JourneyPattern [#4963](https://github.com/opentripplanner/OpenTripPlanner/pull/4963)
- Make sure the default streetRoutingTimeout is used [#4998](https://github.com/opentripplanner/OpenTripPlanner/pull/4998)
- Validate service date for SCHEDULED updates [#4861](https://github.com/opentripplanner/OpenTripPlanner/pull/4861)
- Fix quay duplicates overwriting stop index [#4964](https://github.com/opentripplanner/OpenTripPlanner/pull/4964)
- Allow OTP to start when unexpected enum values are encountered in config [#4983](https://github.com/opentripplanner/OpenTripPlanner/pull/4983)
- Add filename to graph report [#4984](https://github.com/opentripplanner/OpenTripPlanner/pull/4984)
- Support for second criteria in McRaptor [#4996](https://github.com/opentripplanner/OpenTripPlanner/pull/4996)
- Initialize RAPTOR stop-to-stop transfers on server startup [#4977](https://github.com/opentripplanner/OpenTripPlanner/pull/4977)
- Fix bugs related to DST and service date [#5004](https://github.com/opentripplanner/OpenTripPlanner/pull/5004)
- Fix validation of running period for NeTEx flexible lines [#5007](https://github.com/opentripplanner/OpenTripPlanner/pull/5007)
- JSON logging [#5023](https://github.com/opentripplanner/OpenTripPlanner/pull/5023)
- Add allowOverloading parameter to GBFS updaters and fix use of it in routing [#5024](https://github.com/opentripplanner/OpenTripPlanner/pull/5024)
- Fix precedence rules for NeTEx flexible line booking information [#5021](https://github.com/opentripplanner/OpenTripPlanner/pull/5021)
- Fix transferCacheRequests when custom streetRoutingTimeout is set [#5039](https://github.com/opentripplanner/OpenTripPlanner/pull/5039)


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
- Metrics for real-time trip updaters [#4471](https://github.com/opentripplanner/OpenTripPlanner/pull/4471)
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
- Improve SIRI real-time performance by reducing stopPattern duplicates [#4038](https://github.com/opentripplanner/OpenTripPlanner/pull/4038)
- SIRI updaters for Azure ServiceBus [#4106](https://github.com/opentripplanner/OpenTripPlanner/pull/4106)
- Fallback to recorded/expected arrival/departure time if other one is missing in SIRI-ET [#4055](https://github.com/opentripplanner/OpenTripPlanner/pull/4055)
- Allow overriding GBFS system_id with configuration [#4147](https://github.com/opentripplanner/OpenTripPlanner/pull/4147)
- Fix error with transfer-slack and GTFS minTransferTime [#4120](https://github.com/opentripplanner/OpenTripPlanner/pull/4120)
- Use actual distance for walk distance in StreetEdge [#4125](https://github.com/opentripplanner/OpenTripPlanner/pull/4125)
- Don't indicate stop has been updated when NO_DATA is defined [#3962](https://github.com/opentripplanner/OpenTripPlanner/pull/3962)
- Implement nearby searches for car and bicycle parking [#4165](https://github.com/opentripplanner/OpenTripPlanner/pull/4165)
- Do not link cars to stop vertices in routing [#4166](https://github.com/opentripplanner/OpenTripPlanner/pull/4166)
- Add SIRI real-time occupancy info [#4180](https://github.com/opentripplanner/OpenTripPlanner/pull/4180)
- Add gtfs stop description translations [#4158](https://github.com/opentripplanner/OpenTripPlanner/pull/4158)
- Add option to discard min transfer times [#4195](https://github.com/opentripplanner/OpenTripPlanner/pull/4195)
- Use negative delay from first stop in a GTFS RT update in previous stop times when required [#4035](https://github.com/opentripplanner/OpenTripPlanner/pull/4035)
- OTP2 no longer crashes on invalid GTFS stop time sequences [#4205](https://github.com/opentripplanner/OpenTripPlanner/pull/4205)
- Cost-based wheelchair accessibility routing for streets [#4163](https://github.com/opentripplanner/OpenTripPlanner/pull/4163)
- Expose SIRI ET PredictionInaccurate in Transmodel-API [#4217](https://github.com/opentripplanner/OpenTripPlanner/pull/4217)
- Do not apply walkable area processing to open platform geometries [#4225](https://github.com/opentripplanner/OpenTripPlanner/pull/4225)
- Add field 'routingErrors' to GTFS GraphQLAPI [#4253](https://github.com/opentripplanner/OpenTripPlanner/pull/4253)
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
- Return typed errors from real-time updates, prepare for real-time statistics [#4424](https://github.com/opentripplanner/OpenTripPlanner/pull/4424)
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

- Fix NullPointerException when a Real-Time update do not match an existing TripPattern [#3284](https://github.com/opentripplanner/OpenTripPlanner/issues/3284)
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
- Add support for sandboxed real-time vehicle parking updaters [#3796](https://github.com/opentripplanner/OpenTripPlanner/pull/3796)
- Add reading and exposing of Netex submodes [#3793](https://github.com/opentripplanner/OpenTripPlanner/pull/3793)
- Fix: Account for wait-time in no-wait Raptor strategy  [#3798](https://github.com/opentripplanner/OpenTripPlanner/pull/3798)
- Read in flex window from Netex feeds [#3800](https://github.com/opentripplanner/OpenTripPlanner/pull/3800)
- Fix NPE when routing on a graph without transit data. [#3804](https://github.com/opentripplanner/OpenTripPlanner/pull/3804)
- Read leg mode from trip, instead of route [#3819](https://github.com/opentripplanner/OpenTripPlanner/pull/3819)
- Use API mapping in snapshot tests [#3823](https://github.com/opentripplanner/OpenTripPlanner/pull/3823)
- Store stop indices in leg and use them to simplify logic in TripTimeShortHelper [#3820](https://github.com/opentripplanner/OpenTripPlanner/pull/3820)
- Include all trips in `stopTimesForStop` [#3817](https://github.com/opentripplanner/OpenTripPlanner/pull/3817)
- Store all alerts and add support for route_type and direction_id selectors [#3780](https://github.com/opentripplanner/OpenTripPlanner/pull/3780)
- Remove outdated real-time-update from TimetableSnapshot [#3770](https://github.com/opentripplanner/OpenTripPlanner/pull/3770)
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
- Remove ET real-time override code [#3912](https://github.com/opentripplanner/OpenTripPlanner/pull/3912)
- Allow traversal of pathways without traversal time, distance or steps [#3910](https://github.com/opentripplanner/OpenTripPlanner/pull/3910)


## 2.0.0 (2020-11-27)

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
- Also check TripPatterns added by real-time when showing stoptimes for stop [#2954](https://github.com/opentripplanner/OpenTripPlanner/issues/2954)
- Copy geometries from previous TripPattern when real-time updates result in a TripPattern being replaced [#2987](https://github.com/opentripplanner/OpenTripPlanner/issues/2987)
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
- increase GTFS-real-time feeds size limit from 64MB to 2G [#2738](https://github.com/opentripplanner/OpenTripPlanner/issues/2738)
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
- Allow big GTFS-real-time feeds by increasing protobuf size limit to 2G [#2739](https://github.com/opentripplanner/OpenTripPlanner/issues/2739)
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
- Real-time fetch / streaming configurable via JSON
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
- Real-time bike rental availability feed support
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
- Legs in API response have TripId (for real-time information)
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
