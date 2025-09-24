<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc/templates
   - Generated directory is: /doc/user 
-->

# Graph Build Configuration

This table lists all the JSON properties that can be defined in a `build-config.json` file. These
will be stored in the graph itself, and affect any server that subsequently loads that graph.
Sections follow that describe particular settings in more depth.


## Parameters Overview

<!-- PARAMETERS-TABLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                                                            |         Type         | Summary                                                                                                                                                        |  Req./Opt. | Default Value                     | Since |
|---------------------------------------------------------------------------------------------|:--------------------:|----------------------------------------------------------------------------------------------------------------------------------------------------------------|:----------:|-----------------------------------|:-----:|
| [areaVisibility](#areaVisibility)                                                           |       `boolean`      | Perform visibility calculations.                                                                                                                               | *Optional* | `false`                           |  1.5  |
| [buildReportDir](#buildReportDir)                                                           |         `uri`        | URI to the directory where the graph build report should be written to.                                                                                        | *Optional* |                                   |  2.0  |
| [configVersion](#configVersion)                                                             |       `string`       | Deployment version of the *build-config.json*.                                                                                                                 | *Optional* |                                   |  2.1  |
| [dataImportReport](#dataImportReport)                                                       |       `boolean`      | Generate nice HTML report of Graph errors/warnings                                                                                                             | *Optional* | `false`                           |  2.0  |
| [distanceBetweenElevationSamples](#distanceBetweenElevationSamples)                         |       `double`       | The distance between elevation samples in meters.                                                                                                              | *Optional* | `10.0`                            |  2.0  |
| embedRouterConfig                                                                           |       `boolean`      | Embed the Router config in the graph, which allows it to be sent to a server fully configured over the wire.                                                   | *Optional* | `true`                            |  2.0  |
| [graph](#graph)                                                                             |         `uri`        | URI to the graph object file for reading and writing.                                                                                                          | *Optional* |                                   |  2.0  |
| [includeEllipsoidToGeoidDifference](#includeEllipsoidToGeoidDifference)                     |       `boolean`      | Include the Ellipsoid to Geoid difference in the calculations of every point along every StreetWithElevationEdge.                                              | *Optional* | `false`                           |  2.0  |
| maxAreaNodes                                                                                |       `integer`      | Visibility calculations for an area will not be done if there are more nodes than this limit.                                                                  | *Optional* | `200`                             |  2.1  |
| [maxDataImportIssuesPerFile](#maxDataImportIssuesPerFile)                                   |       `integer`      | When to split the import report.                                                                                                                               | *Optional* | `1000`                            |  2.0  |
| maxElevationPropagationMeters                                                               |       `integer`      | The maximum distance to propagate elevation to vertices which have no elevation.                                                                               | *Optional* | `2000`                            |  1.5  |
| [maxStopToShapeSnapDistance](#maxStopToShapeSnapDistance)                                   |       `double`       | Maximum distance between route shapes and their stops.                                                                                                         | *Optional* | `150.0`                           |  2.1  |
| maxTransferDuration                                                                         |      `duration`      | Transfers up to this duration with a mode-specific speed value will be pre-calculated and included in the Graph.                                               | *Optional* | `"PT30M"`                         |  2.1  |
| [multiThreadElevationCalculations](#multiThreadElevationCalculations)                       |       `boolean`      | Configuring multi-threading during elevation calculations.                                                                                                     | *Optional* | `false`                           |  2.0  |
| [osmCacheDataInMem](#osmCacheDataInMem)                                                     |       `boolean`      | If OSM data should be cached in memory during processing.                                                                                                      | *Optional* | `false`                           |  2.0  |
| [osmNaming](#osmNaming)                                                                     |        `enum`        | A custom OSM namer to use.                                                                                                                                     | *Optional* | `"default"`                       |  1.5  |
| platformEntriesLinking                                                                      |       `boolean`      | Link unconnected entries to public transport platforms.                                                                                                        | *Optional* | `false`                           |  2.0  |
| [readCachedElevations](#readCachedElevations)                                               |       `boolean`      | Whether to read cached elevation data.                                                                                                                         | *Optional* | `true`                            |  2.0  |
| staticBikeParkAndRide                                                                       |       `boolean`      | Whether we should create bike P+R stations from OSM data.                                                                                                      | *Optional* | `false`                           |  1.5  |
| staticParkAndRide                                                                           |       `boolean`      | Whether we should create car P+R stations from OSM data.                                                                                                       | *Optional* | `true`                            |  1.5  |
| stopConsolidationFile                                                                       |         `uri`        | Name of the CSV-formatted file in the build directory which contains the configuration for stop consolidation.                                                 | *Optional* |                                   |  2.5  |
| [streetGraph](#streetGraph)                                                                 |         `uri`        | URI to the street graph object file for reading and writing.                                                                                                   | *Optional* |                                   |  2.0  |
| [subwayAccessTime](#subwayAccessTime)                                                       |       `double`       | Minutes necessary to reach stops served by trips on routes of route_type=1 (subway) from the street.                                                           | *Optional* | `2.0`                             |  1.5  |
| [transitModelTimeZone](#transitModelTimeZone)                                               |      `time-zone`     | Time zone for the graph.                                                                                                                                       | *Optional* |                                   |  2.2  |
| [transitServiceEnd](#transitServiceEnd)                                                     |      `duration`      | Limit the import of transit services to the given end date.                                                                                                    | *Optional* | `"P3Y"`                           |  2.0  |
| [transitServiceStart](#transitServiceStart)                                                 |      `duration`      | Limit the import of transit services to the given START date.                                                                                                  | *Optional* | `"-P1Y"`                          |  2.0  |
| [writeCachedElevations](#writeCachedElevations)                                             |       `boolean`      | Reusing elevation data from previous builds                                                                                                                    | *Optional* | `false`                           |  2.0  |
| [boardingLocationTags](#boardingLocationTags)                                               |      `string[]`      | What OSM tags should be looked on for the source of matching stops to platforms and stops.                                                                     | *Optional* |                                   |  2.2  |
| [dataOverlay](sandbox/DataOverlay.md)                                                       |       `object`       | Config for the DataOverlay Sandbox module                                                                                                                      | *Optional* |                                   |  2.2  |
| [dem](#dem)                                                                                 |      `object[]`      | Specify parameters for DEM extracts.                                                                                                                           | *Optional* |                                   |  2.2  |
|       [elevationUnitMultiplier](#dem_0_elevationUnitMultiplier)                             |       `double`       | Specify a multiplier to convert elevation units from source to meters. Overrides the value specified in `demDefaults`.                                         | *Optional* | `1.0`                             |  2.3  |
|       source                                                                                |         `uri`        | The unique URI pointing to the data file.                                                                                                                      | *Required* |                                   |  2.2  |
| demDefaults                                                                                 |       `object`       | Default properties for DEM extracts.                                                                                                                           | *Optional* |                                   |  2.3  |
|    [elevationUnitMultiplier](#demDefaults_elevationUnitMultiplier)                          |       `double`       | Specify a multiplier to convert elevation units from source to meters.                                                                                         | *Optional* | `1.0`                             |  2.3  |
| [elevationBucket](#elevationBucket)                                                         |       `object`       | Used to download NED elevation tiles from the given AWS S3 bucket.                                                                                             | *Optional* |                                   |   na  |
| [emission](sandbox/Emission.md)                                                             |       `object`       | Emissions configuration.                                                                                                                                       | *Optional* |                                   |  2.5  |
| [fares](sandbox/Fares.md)                                                                   |       `object`       | Fare configuration.                                                                                                                                            | *Optional* |                                   |  2.0  |
| gsConfig                                                                                    |       `object`       | Configuration for Google Cloud Storage                                                                                                                         | *Optional* |                                   |  2.8  |
| gtfsDefaults                                                                                |       `object`       | The gtfsDefaults section allows you to specify default properties for GTFS files.                                                                              | *Optional* |                                   |  2.3  |
|    blockBasedInterlining                                                                    |       `boolean`      | Whether to create stay-seated transfers in between two trips with the same block id.                                                                           | *Optional* | `true`                            |  2.3  |
|    [discardMinTransferTimes](#gd_discardMinTransferTimes)                                   |       `boolean`      | Should minimum transfer times in GTFS files be discarded.                                                                                                      | *Optional* | `false`                           |  2.3  |
|    maxInterlineDistance                                                                     |       `integer`      | Maximal distance between stops in meters that will connect consecutive trips that are made with same vehicle.                                                  | *Optional* | `200`                             |  2.3  |
|    [stationTransferPreference](#gd_stationTransferPreference)                               |        `enum`        | Should there be some preference or aversion for transfers at stops that are part of a station.                                                                 | *Optional* | `"allowed"`                       |  2.3  |
| islandPruning                                                                               |       `object`       | Settings for fixing street graph connectivity errors                                                                                                           | *Optional* |                                   |  2.3  |
|    [adaptivePruningDistance](#islandPruning_adaptivePruningDistance)                        |       `integer`      | Search distance for analyzing islands in pruning.                                                                                                              | *Optional* | `250`                             |  2.3  |
|    [adaptivePruningFactor](#islandPruning_adaptivePruningFactor)                            |       `double`       | Defines how much pruning thresholds grow maximally by distance.                                                                                                | *Optional* | `50.0`                            |  2.3  |
|    [islandWithStopsMaxSize](#islandPruning_islandWithStopsMaxSize)                          |       `integer`      | When a graph island with stops in it should be pruned.                                                                                                         | *Optional* | `2`                               |  2.3  |
|    [islandWithoutStopsMaxSize](#islandPruning_islandWithoutStopsMaxSize)                    |       `integer`      | When a graph island without stops should be pruned.                                                                                                            | *Optional* | `10`                              |  2.3  |
| [localFileNamePatterns](#localFileNamePatterns)                                             |       `object`       | Patterns for matching OTP file types in the base directory                                                                                                     | *Optional* |                                   |  2.0  |
|    [dem](#lfp_dem)                                                                          |       `regexp`       | Pattern for matching elevation DEM files.                                                                                                                      | *Optional* | `"(?i)\.tiff?$"`                  |  2.0  |
|    [gtfs](#lfp_gtfs)                                                                        |       `regexp`       | Patterns for matching GTFS zip-files or directories.                                                                                                           | *Optional* | `"(?i)gtfs"`                      |  2.0  |
|    [netex](#lfp_netex)                                                                      |       `regexp`       | Patterns for matching NeTEx zip files or directories.                                                                                                          | *Optional* | `"(?i)netex"`                     |  2.0  |
|    [osm](#lfp_osm)                                                                          |       `regexp`       | Pattern for matching Open Street Map input files.                                                                                                              | *Optional* | `"(?i)(\.pbf¦\.osm¦\.osm\.xml)$"` |  2.0  |
| netexDefaults                                                                               |       `object`       | The netexDefaults section allows you to specify default properties for NeTEx files.                                                                            | *Optional* |                                   |  2.2  |
|    feedId                                                                                   |       `string`       | This field is used to identify the specific NeTEx feed. It is used instead of the feed_id field in GTFS file feed_info.txt.                                    | *Optional* | `"NETEX"`                         |  2.2  |
|    [groupFilePattern](#nd_groupFilePattern)                                                 |       `regexp`       | Pattern for matching group NeTEx files.                                                                                                                        | *Optional* | `"(\w{3})-.*\.xml"`               |  2.0  |
|    ignoreFareFrame                                                                          |       `boolean`      | Ignore contents of the FareFrame                                                                                                                               | *Optional* | `false`                           |  2.3  |
|    [ignoreFilePattern](#nd_ignoreFilePattern)                                               |       `regexp`       | Pattern for matching ignored files in a NeTEx bundle.                                                                                                          | *Optional* | `"$^"`                            |  2.0  |
|    ignoreParking                                                                            |       `boolean`      | Ignore Parking elements.                                                                                                                                       | *Optional* | `true`                            |  2.6  |
|    noTransfersOnIsolatedStops                                                               |       `boolean`      | Whether we should allow transfers to and from StopPlaces marked with LimitedUse.ISOLATED                                                                       | *Optional* | `false`                           |  2.2  |
|    [sharedFilePattern](#nd_sharedFilePattern)                                               |       `regexp`       | Pattern for matching shared NeTEx files in a NeTEx bundle.                                                                                                     | *Optional* | `"shared-data\.xml"`              |  2.0  |
|    [sharedGroupFilePattern](#nd_sharedGroupFilePattern)                                     |       `regexp`       | Pattern for matching shared group NeTEx files in a NeTEx bundle.                                                                                               | *Optional* | `"(\w{3})-.*-shared\.xml"`        |  2.0  |
|    [ferryIdsNotAllowedForBicycle](#nd_ferryIdsNotAllowedForBicycle)                         |      `string[]`      | List ferries which do not allow bikes.                                                                                                                         | *Optional* |                                   |  2.0  |
| [osm](#osm)                                                                                 |      `object[]`      | Configure properties for a given OpenStreetMap feed.                                                                                                           | *Optional* |                                   |  2.2  |
|       includeOsmSubwayEntrances                                                             |       `boolean`      | Whether to include subway entrances from the OSM data. Overrides the value specified in `osmDefaults`.                                                         | *Optional* | `false`                           |  2.7  |
|       [osmTagMapping](#osm_0_osmTagMapping)                                                 |        `enum`        | The named set of mapping rules applied when parsing OSM tags. Overrides the value specified in `osmDefaults`.                                                  | *Optional* | `"default"`                       |  2.2  |
|       source                                                                                |         `uri`        | The unique URI pointing to the data file.                                                                                                                      | *Required* |                                   |  2.2  |
|       timeZone                                                                              |      `time-zone`     | The timezone used to resolve opening hours in OSM data. Overrides the value specified in `osmDefaults`.                                                        | *Optional* |                                   |  2.2  |
| osmDefaults                                                                                 |       `object`       | Default properties for OpenStreetMap feeds.                                                                                                                    | *Optional* |                                   |  2.2  |
|    includeOsmSubwayEntrances                                                                |       `boolean`      | Whether to include subway entrances from the OSM data.                                                                                                         | *Optional* | `false`                           |  2.7  |
|    [osmTagMapping](#od_osmTagMapping)                                                       |        `enum`        | The named set of mapping rules applied when parsing OSM tags.                                                                                                  | *Optional* | `"default"`                       |  2.2  |
|    timeZone                                                                                 |      `time-zone`     | The timezone used to resolve opening hours in OSM data.                                                                                                        | *Optional* |                                   |  2.2  |
| [transferParametersForMode](#transferParametersForMode)                                     | `enum map of object` | Configures mode-specific properties for transfer calculations.                                                                                                 | *Optional* |                                   |  2.7  |
|    BIKE                                                                                     |       `object`       | NA                                                                                                                                                             | *Optional* |                                   |  2.7  |
|       [bikesAllowedStopMaxTransferDuration](#tpfm_BIKE_bikesAllowedStopMaxTransferDuration) |      `duration`      | This is used for specifying a `maxTransferDuration` value to use with transfers between stops which are visited by trips that allow bikes.                     | *Optional* |                                   |  2.9  |
|       [carsAllowedStopMaxTransferDuration](#tpfm_BIKE_carsAllowedStopMaxTransferDuration)   |      `duration`      | This is used for specifying a `maxTransferDuration` value to use with transfers between stops which are visited by trips that allow cars.                      | *Optional* |                                   |  2.7  |
|       [disableDefaultTransfers](#tpfm_BIKE_disableDefaultTransfers)                         |       `boolean`      | This disables default transfer calculations.                                                                                                                   | *Optional* | `false`                           |  2.7  |
|       maxTransferDuration                                                                   |      `duration`      | This overwrites the default `maxTransferDuration` for the given mode.                                                                                          | *Optional* |                                   |  2.7  |
|    CAR                                                                                      |       `object`       | NA                                                                                                                                                             | *Optional* |                                   |  2.7  |
|       [bikesAllowedStopMaxTransferDuration](#tpfm_CAR_bikesAllowedStopMaxTransferDuration)  |      `duration`      | This is used for specifying a `maxTransferDuration` value to use with transfers between stops which are visited by trips that allow bikes.                     | *Optional* |                                   |  2.9  |
|       [carsAllowedStopMaxTransferDuration](#tpfm_CAR_carsAllowedStopMaxTransferDuration)    |      `duration`      | This is used for specifying a `maxTransferDuration` value to use with transfers between stops which are visited by trips that allow cars.                      | *Optional* |                                   |  2.7  |
|       [disableDefaultTransfers](#tpfm_CAR_disableDefaultTransfers)                          |       `boolean`      | This disables default transfer calculations.                                                                                                                   | *Optional* | `false`                           |  2.7  |
|       maxTransferDuration                                                                   |      `duration`      | This overwrites the default `maxTransferDuration` for the given mode.                                                                                          | *Optional* |                                   |  2.7  |
| [transferRequests](RouteRequest.md)                                                         |      `object[]`      | Routing requests to use for pre-calculating stop-to-stop transfers.                                                                                            | *Optional* |                                   |  2.1  |
| [transitFeeds](#transitFeeds)                                                               |      `object[]`      | Scan for transit data files                                                                                                                                    | *Optional* |                                   |  2.2  |
|    { object }                                                                               |       `object`       | Nested object in array. The object type is determined by the parameters.                                                                                       | *Optional* |                                   |  2.2  |
|       type = "gtfs"                                                                         |        `enum`        | The feed input format.                                                                                                                                         | *Required* |                                   |  2.2  |
|       blockBasedInterlining                                                                 |       `boolean`      | Whether to create stay-seated transfers in between two trips with the same block id. Overrides the value specified in `gtfsDefaults`.                          | *Optional* | `true`                            |  2.3  |
|       [discardMinTransferTimes](#tf_0_discardMinTransferTimes)                              |       `boolean`      | Should minimum transfer times in GTFS files be discarded. Overrides the value specified in `gtfsDefaults`.                                                     | *Optional* | `false`                           |  2.3  |
|       feedId                                                                                |       `string`       | The unique ID for this feed. This overrides any feed ID defined within the feed itself.                                                                        | *Optional* |                                   |  2.2  |
|       maxInterlineDistance                                                                  |       `integer`      | Maximal distance between stops in meters that will connect consecutive trips that are made with same vehicle. Overrides the value specified in `gtfsDefaults`. | *Optional* | `200`                             |  2.3  |
|       source                                                                                |         `uri`        | The unique URI pointing to the data file.                                                                                                                      | *Required* |                                   |  2.2  |
|       [stationTransferPreference](#tf_0_stationTransferPreference)                          |        `enum`        | Should there be some preference or aversion for transfers at stops that are part of a station. Overrides the value specified in `gtfsDefaults`.                | *Optional* | `"allowed"`                       |  2.3  |
|    { object }                                                                               |       `object`       | Nested object in array. The object type is determined by the parameters.                                                                                       | *Optional* |                                   |  2.2  |
|       type = "netex"                                                                        |        `enum`        | The feed input format.                                                                                                                                         | *Required* |                                   |  2.2  |
|       feedId                                                                                |       `string`       | This field is used to identify the specific NeTEx feed. It is used instead of the feed_id field in GTFS file feed_info.txt.                                    | *Required* |                                   |  2.2  |
|       [groupFilePattern](#tf_1_groupFilePattern)                                            |       `regexp`       | Pattern for matching group NeTEx files.                                                                                                                        | *Optional* | `"(\w{3})-.*\.xml"`               |  2.0  |
|       ignoreFareFrame                                                                       |       `boolean`      | Ignore contents of the FareFrame                                                                                                                               | *Optional* | `false`                           |  2.3  |
|       [ignoreFilePattern](#tf_1_ignoreFilePattern)                                          |       `regexp`       | Pattern for matching ignored files in a NeTEx bundle.                                                                                                          | *Optional* | `"$^"`                            |  2.0  |
|       ignoreParking                                                                         |       `boolean`      | Ignore Parking elements.                                                                                                                                       | *Optional* | `true`                            |  2.6  |
|       noTransfersOnIsolatedStops                                                            |       `boolean`      | Whether we should allow transfers to and from StopPlaces marked with LimitedUse.ISOLATED                                                                       | *Optional* | `false`                           |  2.2  |
|       [sharedFilePattern](#tf_1_sharedFilePattern)                                          |       `regexp`       | Pattern for matching shared NeTEx files in a NeTEx bundle.                                                                                                     | *Optional* | `"shared-data\.xml"`              |  2.0  |
|       [sharedGroupFilePattern](#tf_1_sharedGroupFilePattern)                                |       `regexp`       | Pattern for matching shared group NeTEx files in a NeTEx bundle.                                                                                               | *Optional* | `"(\w{3})-.*-shared\.xml"`        |  2.0  |
|       source                                                                                |         `uri`        | The unique URI pointing to the data file.                                                                                                                      | *Required* |                                   |  2.2  |
|       [ferryIdsNotAllowedForBicycle](#tf_1_ferryIdsNotAllowedForBicycle)                    |      `string[]`      | List ferries which do not allow bikes.                                                                                                                         | *Optional* |                                   |  2.0  |
| [transitRouteToStationCentroid](#transitRouteToStationCentroid)                             |  `feed-scoped-id[]`  | List stations that should route to centroid.                                                                                                                   | *Optional* |                                   |  2.7  |

<!-- PARAMETERS-TABLE END -->


## Specifying URIs

As a general rule, references to data files are specified as absolute URIs and must start with the 
protocol name.   

**Example**

Local files: `"file:///Users/kelvin/otp/streetGraph.obj"`  
HTTPS resources: `"https://download.geofabrik.de/europe/norway-latest.osm.pbf"`  
Google Cloud Storage files: `"gs://otp-test-bucket/a/b/graph.obj"`  

Alternatively if a relative URI can be provided, it is interpreted as a path relative to the
[base directory](Configuration.md#Base-Directory).

**Example**

File relative to the base directory (inside the base directory): `streetGraph.obj`   
File relative to the base directory (outside the base directory): `../street-graphs/streetGraph.obj`


### Example With Multiple Data Sources

For example, this configuration could be used to load GTFS and OSM inputs from Google Cloud Storage:

```JSON
// build-config.json
{
  "osm": [
    {
      "source": "gs://bucket-name/streets.pbf"
    }
  ],
  "transitFeeds": [
    {
      "type": "netex",
      "source": "gs://bucket-name/transit1.zip"
    },
    {
      "type": "gtfs",
      "source": "gs://bucket-name/transit2.zip"
    }
  ]
}
```

The Google Storage system will inherit the permissions of the server it's running on within Google
Cloud. It is also possible to supply credentials in this configuration file (see example below).

Note that when files are specified with URIs in this configuration, the file types do not need to be
inferred from the file names, so these GTFS files can have any names - there is no requirement that
they have the letters "gtfs" in them.

The default behavior of scanning the base directory for inputs is overridden independently for each
file type. So in the above configuration, GTFS and OSM will be loaded from Google Cloud Storage, but
OTP2 will still scan the base directory for all other types such as DEM files. Supplying an empty
array for a particular file type will ensure that no inputs of that type are loaded, including by
local directory scanning.


<h2 id="limit-transit-service-period">Limit the transit service period</h2>

The properties `transitServiceStart` and `transitServiceEnd` can be used to limit the service dates.
This affects both GTFS service calendars and dates. The service calendar is reduced and dates
outside the period are dropped. OTP2 will compute a transit schedule for every day for which it can
find at least one trip running. On the other hand, OTP will waste resources if a service end date
is *unbounded* or very large (`9999-12-31`). To avoid this, limit the OTP service period. Also, if
you provide a service with multiple feeds they may have different service end dates. To avoid
inconsistent results, the period can be limited, so all feeds have data for the entire period. The
default is to use a period of 1 year before, and 3 years after the day the graph is built. Limiting
the period will *not* improve the search performance, but OTP will build faster and load faster in
most cases.

The `transitServiceStart` and `transitServiceEnd` parameters are set using an absolute date
like `2020-12-31` or a period like `P1Y6M5D` relative to the graph build date. Negative periods is
used to specify dates in the past. The period is computed using the system time-zone, not the feed
time-zone. Also, remember that the service day might be more than 24 hours. So be sure to include
enough slack to account for the this. Setting the limits too wide have very little impact and is in
general better than trying to be exact. The period and date format follow the ISO 8601 standard.

**Example**

```JSON
// build-config.json
{
  // Include 3 months of history
  "transitServiceStart" : "-P3M",
  // Include 1 year 6 month and 5 days of scheduled data in the future 
  "transitServiceEnd" : "P1Y6M5D"
}
```

## OpenStreetMap(OSM) configuration

It is possible to adjust how OSM data is interpreted by OpenTripPlanner when building the road part
of the routing graph.

### OSM tag mapping

OSM tags have different meanings in different countries, and how the roads in a particular country
or region are tagged affects routing. As an example roads tagged with `highway=trunk are (mainly)
walkable in Norway, but forbidden in some other countries. This might lead to OTP being unable to
snap stops to these roads, or by giving you poor routing results for walking and biking. You can
adjust which road types that are accessible by foot, car & bicycle as well as speed limits,
suitability for biking and walking. It's possible to define "safety" values for cycling and walking which are used in routing.

To add your own OSM tag mapping have a look
at `org.opentripplanner.graph_builder.module.osm.tagmapping.NorwayTagMapper`
and `org.opentripplanner.graph_builder.module.osm.tagmapping.DefaultMapper` as examples. 
If you choose to mainly rely on the default rules, make sure you add your own rules first before applying the default ones.
The mechanism is that for any two identical tags, OTP will use the first one.

```JSON
// build-config.json
{
  "osm": [
    {
      "source": "gs://marduk-dev/osm/oslo_norway.osm-160816.pbf",
      "osmTagMapping": "norway"
    }
  ]
}
```

## Elevation data

OpenTripPlanner can "drape" the OSM street network over a digital elevation model (DEM). This allows
OTP to draw an elevation profile for the on-street portion of itineraries, and helps provide better
routing for bicyclists. It even helps avoid hills for walking itineraries. DEMs are usually supplied
as rasters (regular grids of numbers) stored in image formats such as GeoTIFF.


### Geoid Difference

Some elevation data sets are relative to mean sea level. At a global scale sea level is represented
as a surface called the geoid, which is irregular in shape due to local gravitational anomalies. On
the other hand, GPS elevations are reported relative to the WGS84 spheroid, a perfectly smooth
mathematical surface approximating the geoid. In cases where the two elevation definitions are
mixed, it may be necessary to adjust elevation values to avoid confusing users with things like
negative elevation values in places clearly above sea level.
See [issue #2301](https://github.com/opentripplanner/OpenTripPlanner/issues/2301)
for detailed discussion of this.

OTP allows you to adjust the elevation values reported in API responses in two ways. The first way
is to store ellipsoid (GPS) elevation values internally, but apply a single geoid difference value
in the OTP client where appropriate to display elevations above sea level. 

Using a single value can be sufficient for smaller OTP deployments, but might result in
incorrect values at the edges of larger OTP deployments. If your OTP instance uses this, it is
recommended to set a default request value in the `router-config.json` file as follows:

```JSON
// router-config.json
{
    "routingDefaults": {
        "geoidElevation": true   
    }
}
```

The second way is to precompute these geoid difference values at a more granular level and store all
elevations internally relative to the geoid (sea level). Elevations returned in the API responses
will then not need to be adjusted to match end users' intuitive understanding of elevation. In order
to speed up calculations, these geoid difference values are calculated and cached using only 2
significant digits of GPS coordinates. This is more than enough detail for most regions of the world
and should result in less than one meter of vertical error even in areas that have the largest geoid
irregularities. To enable this, include the following in the `build-config.json` file:

```JSON
// build-config.json
{
  "includeEllipsoidToGeoidDifference": true
}
```

If the geoid difference values are precomputed, be careful to not set the routing resource value
of `geoidElevation` to true in order to avoid having the graph-wide geoid added again to all
elevation values in the relevant street edges in responses.

### Other raster elevation data

For other parts of the world you will need a GeoTIFF file containing the elevation data. These are
often available from national geographic surveys, or you can always fall back on the worldwide
[Space Shuttle Radar Topography Mission](http://www2.jpl.nasa.gov/srtm/) (SRTM) data. This not
particularly high resolution (roughly 30 meters horizontally) but it can give acceptable results.

Simply place the elevation data file in the directory with the other graph builder inputs, alongside
the GTFS and OSM data. Make sure the file has a `.tiff` or `.tif` extension, and the graph builder
should detect its presence and apply the elevation data to the streets.

OTP should automatically handle DEM GeoTIFFs in most common projections. You may want to check for
elevation-related error messages during the graph build process to make sure OTP has properly
discovered the projection. If you are using a DEM in unprojected coordinates make sure that the axis
order is (longitude, latitude) rather than (latitude, longitude). Unfortunately there is no reliable
standard for WGS84 axis order, so OTP uses the same axis order as the above-mentioned SRTM data, 
which is also the default for the popular Proj4 library.

DEM files(USGS DEM) is not supported by OTP, but can be converted to GeoTIFF with tools
like [GDAL](http://www.gdal.org/). Use `gdal_merge.py -o merged.tiff *.dem` to merge a set of `dem`
files into one `tif` file.

See Interline [PlanetUtils](https://github.com/interline-io/planetutils) for a set of scripts to
download, merge, and resample 
[Mapzen/Amazon Terrain Tiles](https://registry.opendata.aws/terrain-tiles/).


### Elevation unit conversion

By default, OTP expects the elevation data to use metres. However, by setting 
`elevationUnitMultiplier` in `build-config.json`, it is possible to define a multiplier that 
converts the elevation values from some other unit to metres.

```JSON
// build-config.json
{
  "dem": [
    {
      "source": "gs://otp-test-bucket/a/b/northpole.dem.tif",
      // Correct conversion multiplier when source data uses decimetres instead of metres
      "elevationUnitMultiplier": 0.1
    }
  ]
}
```

### Elevation Data Calculation Optimizations

Calculating elevations on all StreetEdges can take a dramatically long time. In a very large graph
build for multiple Northeast US states, the time it took to download the elevation data and
calculate all the elevations took roughly 1.5 hours.

If you are using cloud computing for your OTP instances, it is recommended to create prebuilt images
that contain the elevation data you need. This will save time because all the data won't need to be
downloaded.

However, the bulk of the time will still be spent calculating elevations for the street edges.
Therefore, a further optimization can be done to calculate and save the elevation data during  a 
graph build and then save it for future use.


#### Reusing elevation data from previous builds

In order to write out the precalculated elevation data, add this to your `build-config.json` file:

```JSON
// build-config.json
{  
  "writeCachedElevations": true
}
```
See [writeCachedElevations](#writeCachedElevations) for details.


## Parameter Details

<!-- PARAMETERS-DETAILS BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

<h3 id="areaVisibility">areaVisibility</h3>

**Since version:** `1.5` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** / 

Perform visibility calculations.

If this is `true` OTP attempts to calculate a path straight through an OSM area using the
shortest way rather than around the edge of it. (These calculations can be time consuming).


<h3 id="buildReportDir">buildReportDir</h3>

**Since version:** `2.0` ∙ **Type:** `uri` ∙ **Cardinality:** `Optional`   
**Path:** / 

URI to the directory where the graph build report should be written to.

The html report is written into this directory. If the directory exist, any existing files are deleted.
If it does not exist, it is created.


<h3 id="configVersion">configVersion</h3>

**Since version:** `2.1` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** / 

Deployment version of the *build-config.json*.

The config-version is a parameter which each OTP deployment may set to be able to query the
OTP server and verify that it uses the correct version of the config. The version should be
injected into the config in the (continuous) deployment pipeline. How this is done, is up to
the deployment.

The config-version has no effect on OTP, and is provided as is on the API. There is no syntax
or format check on the version and it can be any string.

Be aware that OTP uses the config embedded in the loaded graph if no new config is provided.


<h3 id="dataImportReport">dataImportReport</h3>

**Since version:** `2.0` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** / 

Generate nice HTML report of Graph errors/warnings

The reports are stored in the same location as the graph.

<h3 id="distanceBetweenElevationSamples">distanceBetweenElevationSamples</h3>

**Since version:** `2.0` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `10.0`   
**Path:** / 

The distance between elevation samples in meters.

The default is the approximate resolution of 1/3 arc-second NED data. This should not be smaller than the horizontal resolution of the height data used.

<h3 id="graph">graph</h3>

**Since version:** `2.0` ∙ **Type:** `uri` ∙ **Cardinality:** `Optional`   
**Path:** / 

URI to the graph object file for reading and writing.

The file is created or overwritten if OTP saves the graph to the file.

<h3 id="includeEllipsoidToGeoidDifference">includeEllipsoidToGeoidDifference</h3>

**Since version:** `2.0` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** / 

Include the Ellipsoid to Geoid difference in the calculations of every point along every StreetWithElevationEdge.

When set to true (it is false by default), the elevation module will include the Ellipsoid to
Geoid difference in the calculations of every point along every StreetWithElevationEdge in the
graph.

NOTE: if this is set to true for graph building, make sure to not set the value of
`RoutingResource#geoidElevation` to true otherwise OTP will add this geoid value again to
all of the elevation values in the street edges.


<h3 id="maxDataImportIssuesPerFile">maxDataImportIssuesPerFile</h3>

**Since version:** `2.0` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `1000`   
**Path:** / 

When to split the import report.

  If the number of issues is larger then `maxDataImportIssuesPerFile`, then the files will
  be split in multiple files. Since browsers have problems opening large HTML files.


<h3 id="maxStopToShapeSnapDistance">maxStopToShapeSnapDistance</h3>

**Since version:** `2.1` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `150.0`   
**Path:** / 

Maximum distance between route shapes and their stops.

This field is used for mapping routes geometry shapes. It determines max distance between shape
points and their stop sequence. If mapper cannot find any stops within this radius it will
default to simple stop-to-stop geometry instead.


<h3 id="multiThreadElevationCalculations">multiThreadElevationCalculations</h3>

**Since version:** `2.0` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** / 

Configuring multi-threading during elevation calculations.

  For unknown reasons that seem to depend on data and machine settings, it might be faster
  to use a single processor. If multi-threading is activated, parallel streams will be used
  to calculate the elevations.


<h3 id="osmCacheDataInMem">osmCacheDataInMem</h3>

**Since version:** `2.0` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** / 

If OSM data should be cached in memory during processing.

When loading OSM data, the input is streamed 3 times - one phase for processing RELATIONS, one
for WAYS and last one for NODES. Instead of reading the data source 3 times it might be faster
to cache the entire osm file im memory. The trade off is of course that OTP might use more
memory while loading osm data. You can use this parameter to choose what is best for your
deployment depending on your infrastructure. Set the parameter to `true` to cache the
data, and to `false` to read the stream from the source each time.


<h3 id="osmNaming">osmNaming</h3>

**Since version:** `1.5` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"default"`   
**Path:** /   
**Enum values:** `default` | `portland` | `sidewalks`

A custom OSM namer to use.

<h3 id="readCachedElevations">readCachedElevations</h3>

**Since version:** `2.0` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `true`   
**Path:** / 

Whether to read cached elevation data.

When set to true, the elevation module will attempt to read this file in
order to reuse calculations of elevation data for various coordinate sequences instead of
recalculating them all over again.


<h3 id="streetGraph">streetGraph</h3>

**Since version:** `2.0` ∙ **Type:** `uri` ∙ **Cardinality:** `Optional`   
**Path:** / 

URI to the street graph object file for reading and writing.

The file is created or overwritten if OTP saves the graph to the file

<h3 id="subwayAccessTime">subwayAccessTime</h3>

**Since version:** `1.5` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `2.0`   
**Path:** / 

Minutes necessary to reach stops served by trips on routes of route_type=1 (subway) from the street.

Note! The preferred way to do this is to update the OSM data.
See [In-station navigation](In-Station-Navigation.md).

The ride locations for some modes of transport such as subways can be slow to reach from the street.
When planning a trip, we need to allow additional time to reach these locations to properly inform
the passenger. For example, this helps avoid suggesting short bus rides between two subway rides as
a way to improve travel time. You can specify how long it takes to reach a subway platform.

This setting does not generalize to other modes like airplanes because you often need much longer time
to check in to a flight (2-3 hours for international flights) than to alight and exit the airport
(perhaps 1 hour). Use [`boardSlackForMode`](RouteRequest.md#rd_boardSlackForMode) and
[`alightSlackForMode`](RouteRequest.md#rd_alightSlackForMode) for this.


<h3 id="transitModelTimeZone">transitModelTimeZone</h3>

**Since version:** `2.2` ∙ **Type:** `time-zone` ∙ **Cardinality:** `Optional`   
**Path:** / 

Time zone for the graph.

This is used to store the timetables in the transit model, and to interpret times in incoming requests.

<h3 id="transitServiceEnd">transitServiceEnd</h3>

**Since version:** `2.0` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"P3Y"`   
**Path:** / 

Limit the import of transit services to the given end date.

See [Limit the transit service period](#limit-transit-service-period) for an introduction.

The date is inclusive. If set, any transit service on a day AFTER the given date is dropped and
will not be part of the graph. Use an absolute date or a period relative to the date the graph is
build(BUILD_DAY).

Use an empty string to make it unbounded.


<h3 id="transitServiceStart">transitServiceStart</h3>

**Since version:** `2.0` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"-P1Y"`   
**Path:** / 

Limit the import of transit services to the given START date.

See [Limit the transit service period](#limit-transit-service-period) for an introduction.

The date is inclusive. If set, any transit service on a day BEFORE the given date is dropped and
will not be part of the graph. Use an absolute date or a period relative to the date the graph is
build(BUILD_DAY).

Use an empty string to make unbounded.


<h3 id="writeCachedElevations">writeCachedElevations</h3>

**Since version:** `2.0` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** / 

Reusing elevation data from previous builds

When set to true, the elevation module will create a file cache for calculated elevation data.
Subsequent graph builds can reuse the data in this file.

After building the graph, a file called `cached_elevations.obj` will be written to the cache
directory. By default, this file is not written during graph builds. There is also a graph build
parameter called `readCachedElevations` which is set to `true` by default.

In graph builds, the elevation module will attempt to read the `cached_elevations.obj` file from
the cache directory. The cache directory defaults to `/var/otp/cache`, but this can be overridden
via the CLI argument `--cache <directory>`. For the same graph build for multiple Northeast US
states, the time it took with using this pre-downloaded and precalculated data became roughly 9
minutes.

The cached data is a lookup table where the coordinate sequences of respective street edges are
used as keys for calculated data. It is assumed that all of the other input data except for the
OpenStreetMap data remains the same between graph builds. Therefore, if the underlying elevation
data is changed, or different configuration values for `elevationUnitMultiplier` or
`includeEllipsoidToGeoidDifference` are used, then this data becomes invalid and all elevation data
should be recalculated. Over time, various edits to OpenStreetMap will cause this cached data to
become stale and not include new OSM ways. Therefore, periodic update of this cached data is
recommended.


<h3 id="boardingLocationTags">boardingLocationTags</h3>

**Since version:** `2.2` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** / 

What OSM tags should be looked on for the source of matching stops to platforms and stops.

[Detailed documentation](BoardingLocations.md)

<h3 id="dem">dem</h3>

**Since version:** `2.2` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** / 

Specify parameters for DEM extracts.

The dem section allows you to override the default behavior of scanning for elevation
files in the [base directory](Configuration.md#Base-Directory). You can specify data
located outside the local filesystem (including cloud storage services) or at various
different locations around the local filesystem.

If not specified OTP will fall back to auto-detection based on the directory provided on
the command line.


<h3 id="dem_0_elevationUnitMultiplier">elevationUnitMultiplier</h3>

**Since version:** `2.3` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `1.0`   
**Path:** /dem/[0] 

Specify a multiplier to convert elevation units from source to meters. Overrides the value specified in `demDefaults`.

Unit conversion multiplier for elevation values. No conversion needed if the elevation
values are defined in meters in the source data. If, for example, decimetres are used
in the source data, this should be set to 0.1.


<h3 id="demDefaults_elevationUnitMultiplier">elevationUnitMultiplier</h3>

**Since version:** `2.3` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `1.0`   
**Path:** /demDefaults 

Specify a multiplier to convert elevation units from source to meters.

Unit conversion multiplier for elevation values. No conversion needed if the elevation
values are defined in meters in the source data. If, for example, decimetres are used
in the source data, this should be set to 0.1.


<h3 id="elevationBucket">elevationBucket</h3>

**Since version:** `na` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** / 

Used to download NED elevation tiles from the given AWS S3 bucket.

In the United States, a high resolution [National Elevation Dataset](http://ned.usgs.gov/) is
available for the entire territory. It used to be possible for OTP to download NED tiles on the fly
from a rather complex USGS SOAP service. This process was somewhat unreliable and would greatly slow
down the graph building process. In any case the service has since been replaced. But the USGS would
also deliver the whole dataset in bulk if you
[sent them a hard drive](https://web.archive.org/web/20150811051917/http://ned.usgs.gov:80/faq.html#DATA).
We did this many years back and uploaded the entire data set to Amazon AWS S3. OpenTripPlanner
contains another module that can automatically fetch data in this format from any Amazon S3 copy of
the bulk data.

This `ned13` bucket is still available on S3 under a "requester pays" policy. As long as you specify
valid AWS account credentials you should be able to download tiles, and any bandwidth costs will be
billed to your AWS account.

Once the tiles are downloaded for a particular geographic area, OTP will keep them in local cache
for the next graph build operation. You should add the `--cache <directory>` command line parameter
to specify your NED tile cache location.


<h3 id="gd_discardMinTransferTimes">discardMinTransferTimes</h3>

**Since version:** `2.3` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /gtfsDefaults 

Should minimum transfer times in GTFS files be discarded.

This is useful eg. when the minimum transfer time is only set for ticketing purposes,
but we want to calculate the transfers always from OSM data.


<h3 id="gd_stationTransferPreference">stationTransferPreference</h3>

**Since version:** `2.3` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"allowed"`   
**Path:** /gtfsDefaults   
**Enum values:** `preferred` | `recommended` | `allowed` | `discouraged`

Should there be some preference or aversion for transfers at stops that are part of a station.

This parameter sets the generic level of preference. What is the actual cost can be changed
with the `stopBoardAlightDuringTransferCost` parameter in the router configuration.


<h3 id="islandPruning_adaptivePruningDistance">adaptivePruningDistance</h3>

**Since version:** `2.3` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `250`   
**Path:** /islandPruning 

Search distance for analyzing islands in pruning.

The distance after which disconnected sub graph is considered as real island in pruning heuristics.


<h3 id="islandPruning_adaptivePruningFactor">adaptivePruningFactor</h3>

**Since version:** `2.3` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `50.0`   
**Path:** /islandPruning 

Defines how much pruning thresholds grow maximally by distance.

Expands the pruning thresholds as the distance of an island from the rest of the graph gets smaller.
Even fairly large disconnected sub graphs should be removed if they are badly entangled with other graph.


<h3 id="islandPruning_islandWithStopsMaxSize">islandWithStopsMaxSize</h3>

**Since version:** `2.3` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `2`   
**Path:** /islandPruning 

When a graph island with stops in it should be pruned.

This field indicates the pruning threshold for islands with stops. Any such island under this
edge count will be pruned.


<h3 id="islandPruning_islandWithoutStopsMaxSize">islandWithoutStopsMaxSize</h3>

**Since version:** `2.3` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `10`   
**Path:** /islandPruning 

When a graph island without stops should be pruned.

This field indicates the pruning threshold for islands without stops. Any such island under
this edge count will be pruned.


<h3 id="localFileNamePatterns">localFileNamePatterns</h3>

**Since version:** `2.0` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** / 

Patterns for matching OTP file types in the base directory

When scanning the base directory for inputs, each file's name is checked against patterns to
detect what kind of file it is.

OTP1 used to peek inside ZIP files and read the CSV tables to guess if a ZIP was indeed GTFS. Now
that we support remote input files (cloud storage or arbitrary URLs) not all data sources allow
seeking within files to guess what they are. Therefore, like all other file types GTFS is now
detected from a filename pattern. It is not sufficient to look for the `.zip` extension because
Netex data is also often supplied in a ZIP file.


<h3 id="lfp_dem">dem</h3>

**Since version:** `2.0` ∙ **Type:** `regexp` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"(?i)\.tiff?$"`   
**Path:** /localFileNamePatterns 

Pattern for matching elevation DEM files.

If the filename contains the given pattern it is
considered a match. Any legal Java Regular expression is allowed.


<h3 id="lfp_gtfs">gtfs</h3>

**Since version:** `2.0` ∙ **Type:** `regexp` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"(?i)gtfs"`   
**Path:** /localFileNamePatterns 

Patterns for matching GTFS zip-files or directories.

If the filename contains the given pattern it is considered a match.
Any legal Java Regular expression is allowed.


<h3 id="lfp_netex">netex</h3>

**Since version:** `2.0` ∙ **Type:** `regexp` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"(?i)netex"`   
**Path:** /localFileNamePatterns 

Patterns for matching NeTEx zip files or directories.

If the filename contains the given
pattern it is considered a match. Any legal Java Regular expression is allowed.


<h3 id="lfp_osm">osm</h3>

**Since version:** `2.0` ∙ **Type:** `regexp` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"(?i)(\.pbf|\.osm|\.osm\.xml)$"`   
**Path:** /localFileNamePatterns 

Pattern for matching Open Street Map input files.

If the filename contains the given pattern
it is considered a match. Any legal Java Regular expression is allowed.


<h3 id="nd_groupFilePattern">groupFilePattern</h3>

**Since version:** `2.0` ∙ **Type:** `regexp` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"(\w{3})-.*\.xml"`   
**Path:** /netexDefaults 

Pattern for matching group NeTEx files.

This field is used to match *group files* in the module file (zip file entries).
*group files* are loaded right the after *shared group files* are loaded.
Files are grouped together by the first group pattern in the regular expression.
The pattern `"(\w{3})-.*\.xml"` matches `"RUT-Line-208-Hagalia-Nevlunghavn.xml"`
with group `"RUT"`.


<h3 id="nd_ignoreFilePattern">ignoreFilePattern</h3>

**Since version:** `2.0` ∙ **Type:** `regexp` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"$^"`   
**Path:** /netexDefaults 

Pattern for matching ignored files in a NeTEx bundle.

This field is used to exclude matching *files* in the module file (zip file entries).
The *ignored* files are *not* loaded.


<h3 id="nd_sharedFilePattern">sharedFilePattern</h3>

**Since version:** `2.0` ∙ **Type:** `regexp` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"shared-data\.xml"`   
**Path:** /netexDefaults 

Pattern for matching shared NeTEx files in a NeTEx bundle.

This field is used to match *shared files*(zip file entries) in the module file. Shared
files are loaded first. Then the rest of the files are grouped and loaded.

The pattern `"shared-data.xml"` matches `"shared-data.xml"`

File names are matched in the following order - and treated accordingly to the first match:

 - `ignoreFilePattern`
 - `sharedFilePattern`
 - `sharedGroupFilePattern`
 - `groupFilePattern`


<h3 id="nd_sharedGroupFilePattern">sharedGroupFilePattern</h3>

**Since version:** `2.0` ∙ **Type:** `regexp` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"(\w{3})-.*-shared\.xml"`   
**Path:** /netexDefaults 

Pattern for matching shared group NeTEx files in a NeTEx bundle.

This field is used to match *shared group files* in the module file (zip file entries).
Typically this is used to group all files from one agency together.

*Shared group files* are loaded after shared files, but before the matching group
files. Each *group* of files are loaded as a unit, followed by next group.

Files are grouped together by the first group pattern in the regular expression.

The pattern `"(\w{3})-.*-shared\.xml"` matches `"RUT-shared.xml"` with group `"RUT"`.


<h3 id="nd_ferryIdsNotAllowedForBicycle">ferryIdsNotAllowedForBicycle</h3>

**Since version:** `2.0` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /netexDefaults 

List ferries which do not allow bikes.

Bicycles are allowed on most ferries however the Nordic profile doesn't contain a place
where bicycle conveyance can be defined.

For this reason we allow bicycles on ferries by default and allow to override the rare
case where this is not the case.


<h3 id="osm">osm</h3>

**Since version:** `2.2` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** / 

Configure properties for a given OpenStreetMap feed.

The osm section of build-config.json allows you to override the default behavior of scanning
for OpenStreetMap files in the base directory. You can specify data located outside the
local filesystem (including cloud storage services) or at various different locations around
the local filesystem.


<h3 id="osm_0_osmTagMapping">osmTagMapping</h3>

**Since version:** `2.2` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"default"`   
**Path:** /osm/[0]   
**Enum values:** `default` | `norway` | `uk` | `finland` | `germany` | `hamburg` | `atlanta` | `houston` | `portland` | `constant-speed-finland`

The named set of mapping rules applied when parsing OSM tags. Overrides the value specified in `osmDefaults`.

<h3 id="od_osmTagMapping">osmTagMapping</h3>

**Since version:** `2.2` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"default"`   
**Path:** /osmDefaults   
**Enum values:** `default` | `norway` | `uk` | `finland` | `germany` | `hamburg` | `atlanta` | `houston` | `portland` | `constant-speed-finland`

The named set of mapping rules applied when parsing OSM tags.

<h3 id="transferParametersForMode">transferParametersForMode</h3>

**Since version:** `2.7` ∙ **Type:** `enum map of object` ∙ **Cardinality:** `Optional`   
**Path:** /   
**Enum keys:** `not-set` | `walk` | `bike` | `bike-to-park` | `bike-rental` | `scooter-rental` | `car` | `car-to-park` | `car-pickup` | `car-rental` | `car-hailing` | `flexible`

Configures mode-specific properties for transfer calculations.

This field enables configuring mode-specific parameters for transfer calculations.
To configure mode-specific parameters, the modes should also be used in the `transferRequests` field in the build config.

**Example**

```JSON
// build-config.json
{
  "transferParametersForMode": {
    "CAR": {
      "disableDefaultTransfers": true,
      "carsAllowedStopMaxTransferDuration": "3h"
    },
    "BIKE": {
      "maxTransferDuration": "30m",
      "carsAllowedStopMaxTransferDuration": "3h"
    }
  }
}
```


<h3 id="tpfm_BIKE_bikesAllowedStopMaxTransferDuration">bikesAllowedStopMaxTransferDuration</h3>

**Since version:** `2.9` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional`   
**Path:** /transferParametersForMode/BIKE 

This is used for specifying a `maxTransferDuration` value to use with transfers between stops which are visited by trips that allow bikes.

This parameter configures additional transfers to be calculated for the specified mode only between stops that have trips with bikes.
The transfers are calculated for the mode in a range based on the given duration.
By default, these transfers are not calculated unless specified for a mode with this field.

When compared to walking, using a bike can cover larger distances within the same duration specified in the `maxTransferDuration` field.
This can lead to large amounts of transfers calculated between stops that do not require bike transfers between them.
This in turn can lead to a large increase in memory for the stored graph, depending on the data used in the graph.

For bikes, using this parameter in conjunction with `disableDefaultTransfers` allows calculating transfers only between stops
which have trips which allow carrying bikes. This avoids storing and calculating transfers which are never used so the transit search
can be faster compared to the default transfers.


<h3 id="tpfm_BIKE_carsAllowedStopMaxTransferDuration">carsAllowedStopMaxTransferDuration</h3>

**Since version:** `2.7` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional`   
**Path:** /transferParametersForMode/BIKE 

This is used for specifying a `maxTransferDuration` value to use with transfers between stops which are visited by trips that allow cars.

This parameter configures additional transfers to be calculated for the specified mode only between stops that have trips with cars.
The transfers are calculated for the mode in a range based on the given duration.
By default, these transfers are not calculated unless specified for a mode with this field.

Calculating transfers only between stops that have trips with cars can be useful with car ferries, for example.
Using transit with cars can only occur between certain stops.
These kinds of stops require support for loading cars into ferries, for example.
The default transfers are calculated based on a configurable range (configurable by using the `maxTransferDuration` field)
which limits transfers from stops to only be calculated to other stops that are in range.
When compared to walking, using a car can cover larger distances within the same duration specified in the `maxTransferDuration` field.
This can lead to large amounts of transfers calculated between stops that do not require car transfers between them.
This in turn can lead to a large increase in memory for the stored graph, depending on the data used in the graph.

For cars, using this parameter in conjunction with `disableDefaultTransfers` allows calculating transfers only between relevant stops.
For bikes, using this parameter can enable transfers between ferry stops that would normally not be in range.
In Finland this is useful for bike routes that use ferries near the Turku archipelago, for example.


<h3 id="tpfm_BIKE_disableDefaultTransfers">disableDefaultTransfers</h3>

**Since version:** `2.7` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /transferParametersForMode/BIKE 

This disables default transfer calculations.

The default transfers are calculated based on a configurable range (configurable by using the `maxTransferDuration` field)
which limits transfers from stops to only be calculated to other stops that are in range.
This parameter disables these transfers.
A motivation to disable default transfers could be related to using the `carsAllowedStopMaxTransferDuration` field which only
calculates transfers between stops that have trips with cars, or `bikesAllowedStopMaxTransferDuration` field for bikes.
For example, when using the `carsAllowedStopMaxTransferDuration` field with cars, the default transfers can be redundant.


<h3 id="tpfm_CAR_bikesAllowedStopMaxTransferDuration">bikesAllowedStopMaxTransferDuration</h3>

**Since version:** `2.9` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional`   
**Path:** /transferParametersForMode/CAR 

This is used for specifying a `maxTransferDuration` value to use with transfers between stops which are visited by trips that allow bikes.

This parameter configures additional transfers to be calculated for the specified mode only between stops that have trips with bikes.
The transfers are calculated for the mode in a range based on the given duration.
By default, these transfers are not calculated unless specified for a mode with this field.

When compared to walking, using a bike can cover larger distances within the same duration specified in the `maxTransferDuration` field.
This can lead to large amounts of transfers calculated between stops that do not require bike transfers between them.
This in turn can lead to a large increase in memory for the stored graph, depending on the data used in the graph.

For bikes, using this parameter in conjunction with `disableDefaultTransfers` allows calculating transfers only between stops
which have trips which allow carrying bikes. This avoids storing and calculating transfers which are never used so the transit search
can be faster compared to the default transfers.


<h3 id="tpfm_CAR_carsAllowedStopMaxTransferDuration">carsAllowedStopMaxTransferDuration</h3>

**Since version:** `2.7` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional`   
**Path:** /transferParametersForMode/CAR 

This is used for specifying a `maxTransferDuration` value to use with transfers between stops which are visited by trips that allow cars.

This parameter configures additional transfers to be calculated for the specified mode only between stops that have trips with cars.
The transfers are calculated for the mode in a range based on the given duration.
By default, these transfers are not calculated unless specified for a mode with this field.

Calculating transfers only between stops that have trips with cars can be useful with car ferries, for example.
Using transit with cars can only occur between certain stops.
These kinds of stops require support for loading cars into ferries, for example.
The default transfers are calculated based on a configurable range (configurable by using the `maxTransferDuration` field)
which limits transfers from stops to only be calculated to other stops that are in range.
When compared to walking, using a car can cover larger distances within the same duration specified in the `maxTransferDuration` field.
This can lead to large amounts of transfers calculated between stops that do not require car transfers between them.
This in turn can lead to a large increase in memory for the stored graph, depending on the data used in the graph.

For cars, using this parameter in conjunction with `disableDefaultTransfers` allows calculating transfers only between relevant stops.
For bikes, using this parameter can enable transfers between ferry stops that would normally not be in range.
In Finland this is useful for bike routes that use ferries near the Turku archipelago, for example.


<h3 id="tpfm_CAR_disableDefaultTransfers">disableDefaultTransfers</h3>

**Since version:** `2.7` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /transferParametersForMode/CAR 

This disables default transfer calculations.

The default transfers are calculated based on a configurable range (configurable by using the `maxTransferDuration` field)
which limits transfers from stops to only be calculated to other stops that are in range.
This parameter disables these transfers.
A motivation to disable default transfers could be related to using the `carsAllowedStopMaxTransferDuration` field which only
calculates transfers between stops that have trips with cars, or `bikesAllowedStopMaxTransferDuration` field for bikes.
For example, when using the `carsAllowedStopMaxTransferDuration` field with cars, the default transfers can be redundant.


<h3 id="transitFeeds">transitFeeds</h3>

**Since version:** `2.2` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** / 

Scan for transit data files

The transitFeeds section of `build-config.json` allows you to override the default behavior
of scanning for transit data files in the [base directory](Configuration.md#Base-Directory).
You can specify data located outside the local filesystem (including cloud storage services)
or at various different locations around the local filesystem.

When a feed of a particular type (`netex` or `gtfs`) is specified in the transitFeeds
section, auto-scanning in the base directory for this feed type will be disabled.


<h3 id="tf_0_discardMinTransferTimes">discardMinTransferTimes</h3>

**Since version:** `2.3` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /transitFeeds/[0] 

Should minimum transfer times in GTFS files be discarded. Overrides the value specified in `gtfsDefaults`.

This is useful eg. when the minimum transfer time is only set for ticketing purposes,
but we want to calculate the transfers always from OSM data.


<h3 id="tf_0_stationTransferPreference">stationTransferPreference</h3>

**Since version:** `2.3` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"allowed"`   
**Path:** /transitFeeds/[0]   
**Enum values:** `preferred` | `recommended` | `allowed` | `discouraged`

Should there be some preference or aversion for transfers at stops that are part of a station. Overrides the value specified in `gtfsDefaults`.

This parameter sets the generic level of preference. What is the actual cost can be changed
with the `stopBoardAlightDuringTransferCost` parameter in the router configuration.


<h3 id="tf_1_groupFilePattern">groupFilePattern</h3>

**Since version:** `2.0` ∙ **Type:** `regexp` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"(\w{3})-.*\.xml"`   
**Path:** /transitFeeds/[1] 

Pattern for matching group NeTEx files.

This field is used to match *group files* in the module file (zip file entries).
*group files* are loaded right the after *shared group files* are loaded.
Files are grouped together by the first group pattern in the regular expression.
The pattern `"(\w{3})-.*\.xml"` matches `"RUT-Line-208-Hagalia-Nevlunghavn.xml"`
with group `"RUT"`.


<h3 id="tf_1_ignoreFilePattern">ignoreFilePattern</h3>

**Since version:** `2.0` ∙ **Type:** `regexp` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"$^"`   
**Path:** /transitFeeds/[1] 

Pattern for matching ignored files in a NeTEx bundle.

This field is used to exclude matching *files* in the module file (zip file entries).
The *ignored* files are *not* loaded.


<h3 id="tf_1_sharedFilePattern">sharedFilePattern</h3>

**Since version:** `2.0` ∙ **Type:** `regexp` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"shared-data\.xml"`   
**Path:** /transitFeeds/[1] 

Pattern for matching shared NeTEx files in a NeTEx bundle.

This field is used to match *shared files*(zip file entries) in the module file. Shared
files are loaded first. Then the rest of the files are grouped and loaded.

The pattern `"shared-data.xml"` matches `"shared-data.xml"`

File names are matched in the following order - and treated accordingly to the first match:

 - `ignoreFilePattern`
 - `sharedFilePattern`
 - `sharedGroupFilePattern`
 - `groupFilePattern`


<h3 id="tf_1_sharedGroupFilePattern">sharedGroupFilePattern</h3>

**Since version:** `2.0` ∙ **Type:** `regexp` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"(\w{3})-.*-shared\.xml"`   
**Path:** /transitFeeds/[1] 

Pattern for matching shared group NeTEx files in a NeTEx bundle.

This field is used to match *shared group files* in the module file (zip file entries).
Typically this is used to group all files from one agency together.

*Shared group files* are loaded after shared files, but before the matching group
files. Each *group* of files are loaded as a unit, followed by next group.

Files are grouped together by the first group pattern in the regular expression.

The pattern `"(\w{3})-.*-shared\.xml"` matches `"RUT-shared.xml"` with group `"RUT"`.


<h3 id="tf_1_ferryIdsNotAllowedForBicycle">ferryIdsNotAllowedForBicycle</h3>

**Since version:** `2.0` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /transitFeeds/[1] 

List ferries which do not allow bikes.

Bicycles are allowed on most ferries however the Nordic profile doesn't contain a place
where bicycle conveyance can be defined.

For this reason we allow bicycles on ferries by default and allow to override the rare
case where this is not the case.


<h3 id="transitRouteToStationCentroid">transitRouteToStationCentroid</h3>

**Since version:** `2.7` ∙ **Type:** `feed-scoped-id[]` ∙ **Cardinality:** `Optional`   
**Path:** / 

List stations that should route to centroid.

This field contains a list of station ids for which street legs will start/end at the station
centroid instead of the child stops.

When searching from/to a station the default behaviour is to route from/to any of the stations child
stops. This can cause strange results for stations that have stops spread over a large area.

For some stations you might instead wish to use the centroid of the station as the
origin/destination. In this case the centroid will be used both for direct street search and for
access/egress street search where the station is used as the start/end of the access/egress. But
transit that starts/ends at the station will work as usual without any additional street leg from/to
the centroid.



<!-- PARAMETERS-DETAILS END -->


## Build Config Example

<!-- JSON-EXAMPLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

```JSON
// build-config.json
{
  "transitServiceStart" : "-P3M",
  "transitServiceEnd" : "P1Y",
  "osmCacheDataInMem" : true,
  "localFileNamePatterns" : {
    "osm" : "(i?)\\.osm\\.pbf$",
    "dem" : "(i?)\\.dem\\.tiff?$",
    "gtfs" : "(?i)gtfs",
    "netex" : "(?i)netex"
  },
  "osmDefaults" : {
    "timeZone" : "Europe/Rome",
    "osmTagMapping" : "default"
  },
  "osm" : [
    {
      "source" : "gs://my-bucket/otp-work-dir/norway.osm.pbf",
      "timeZone" : "Europe/Oslo",
      "osmTagMapping" : "norway"
    }
  ],
  "demDefaults" : {
    "elevationUnitMultiplier" : 1.0
  },
  "dem" : [
    {
      "source" : "gs://my-bucket/otp-work-dir/norway.dem.tiff",
      "elevationUnitMultiplier" : 2.5
    }
  ],
  "netexDefaults" : {
    "feedId" : "EN",
    "sharedFilePattern" : "_stops.xml",
    "sharedGroupFilePattern" : "_(\\w{3})_shared_data.xml",
    "groupFilePattern" : "(\\w{3})_.*\\.xml",
    "ignoreFilePattern" : "(temp|tmp)",
    "ferryIdsNotAllowedForBicycle" : [
      "RUT:B107",
      "RUT:B209"
    ]
  },
  "gtfsDefaults" : {
    "stationTransferPreference" : "recommended",
    "discardMinTransferTimes" : false,
    "blockBasedInterlining" : true,
    "maxInterlineDistance" : 200
  },
  "islandPruning" : {
    "islandWithStopsMaxSize" : 2,
    "islandWithoutStopsMaxSize" : 10,
    "adaptivePruningFactor" : 50.0,
    "adaptivePruningDistance" : 250
  },
  "transitFeeds" : [
    {
      "type" : "gtfs",
      "feedId" : "SE",
      "source" : "https://skanetrafiken.se/download/sweden.gtfs.zip"
    },
    {
      "type" : "netex",
      "feedId" : "NO",
      "source" : "gs://BUCKET/OTP_GCS_WORK_DIR/norway-netex.obj",
      "sharedFilePattern" : "_stops.xml",
      "sharedGroupFilePattern" : "_(\\w{3})_shared_data.xml",
      "groupFilePattern" : "(\\w{3})_.*\\.xml",
      "ignoreFilePattern" : "(temp|tmp)"
    }
  ],
  "transferRequests" : [
    {
      "modes" : "WALK"
    },
    {
      "modes" : "WALK",
      "wheelchairAccessibility" : {
        "enabled" : true
      }
    },
    {
      "modes" : "BICYCLE"
    },
    {
      "modes" : "CAR"
    }
  ],
  "stopConsolidationFile" : "consolidated-stops.csv",
  "transferParametersForMode" : {
    "CAR" : {
      "disableDefaultTransfers" : true,
      "carsAllowedStopMaxTransferDuration" : "3h"
    },
    "BIKE" : {
      "disableDefaultTransfers" : true,
      "maxTransferDuration" : "30m",
      "carsAllowedStopMaxTransferDuration" : "3h",
      "bikesAllowedStopMaxTransferDuration" : "1h"
    }
  }
}
```

<!-- JSON-EXAMPLE END -->
