# Changelog

## 0.19.0-SNAPSHOT

- next release, work in progress on master branch

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
- Fixed threading problem caused by graph visualization instrumentation (#1611)
- Fixed 'unconnected areas' infinite loop (#1605) 

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