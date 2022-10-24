# Graph Build Configuration

This table lists all the JSON properties that can be defined in a `build-config.json` file. These
will be stored in the graph itself, and affect any server that subsequently loads that graph.
Sections follow that describe particular settings in more depth.


## Parameters Overview

<!-- PARAMETERS-TABLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

<!-- PARAMETERS-TABLE END -->

## OSM 

The osm section of `build-config.json` allows you to override the default behavior of scanning
for OpenStreetMap files in the [base directory](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/docs/Configuration.md#Base-Directory). You can specify data
located outside the local filesystem (including cloud storage services) or at various
different locations around the local filesystem.

| config key       | description                                                                                                                                         | value type                                                     | value default |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|---------------|
| `source`         | The unique URI pointing to the data file.                                                                                                           | uri                                                            | mandatory     |
| `timeZone`       | The timezone used to resolve opening hours in OSM data. Overrides the value specified in osmDefaults.                                               | The value can be given either as a zone id, or an UTC offset.  | `null`        |
| `osmTagMapping`  | The named set of mapping rules applied when parsing OSM tags. Example: `default`, `norway`, `finland`. Overrides the value specified in osmDefaults | string                                                         | `null`        |

## OSM Defaults

The osmDefaults section of `build-config.json` allows you to specify default properties for OpenStreetMap files. 

| config key          | description                                                                                                                      | value type                                                     | value default |
|---------------------|----------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|---------------|
| `timeZone`          | The timezone used to resolve opening hours in OSM data. If the parameter is not specified, the opening hours will not be parsed. | The value can be given either as a zone id, or an UTC offset.  | `null`        |
| `osmTagMapping`     | The named set of mapping rules applied when parsing OSM tags. Example: `default`, `norway`, `finland`                            | string                                                         | `default`     |

## DEM

The dem section of `build-config.json` allows you to override the default behavior of scanning
for elevation files in the [base directory](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/docs/Configuration.md#Base-Directory). You can specify data
located outside the local filesystem (including cloud storage services) or at various
different locations around the local filesystem.

| config key                 | description                                                                                                                                                                                                                                               | value type | value default |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|---------------|
| `source`                   | The unique URI pointing to the data file.                                                                                                                                                                                                                 | uri        | mandatory     |
| `elevationUnitMultiplier`  | The multiplier applied to elevation values. Use 0.1 if values are given in decimeters. See [Elevation unit conversion](#elevation-unit-conversion). Overrides the value specified in `elevationUnitMultiplier` at the top-level of the configuration file | double     | 1.0           |

## Specifying URIs

As a general rule, references to data files are specified as absolute URIs and must start with the protocol name.   
Example:   
Local files: `"file:///Users/kelvin/otp/streetGraph.obj"`  
HTTPS resources: `"https://download.geofabrik.de/europe/norway-latest.osm.pbf"`  
Google Cloud Storage files: `"gs://otp-test-bucket/a/b/graph.obj"`  

Alternatively if a relative URI can be provided, it is interpreted as a path relative to the [base directory](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/docs/Configuration.md#Base-Directory).
Example:   
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
      "type": "gtfs",
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

See the comments in the source code of
class [`BuildConfig`](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/src/main/java/org/opentripplanner/standalone/config/BuildConfig.java)
for an up-to-date detailed description of each config parameter.

### Local Filename Patterns

When scanning the base directory for inputs, each file's name is checked against patterns to detect
what kind of file it is. These patterns can be overridden in the config, by nesting a
`localFileNamePatterns` property in the build configuration file. Here are the
keys you can place inside `localFileNamePatterns`:

| config key | description                                               | value type     | value default  |
|------------|-----------------------------------------------------------|----------------|----------------|
| `osm`      | Pattern used to match Open Street Map files on local disk | regexp pattern | `(?i)(\.pbf)`  |
| `dem`      | Pattern used to match Elevation DEM files on local disk   | regexp pattern | `(?i)\.tiff?$` |
| `gtfs`     | Pattern used to match GTFS files on local disk            | regexp pattern | `(?i)gtfs`     |
| `netex`    | Pattern used to match NeTEx files on local disk           | regexp pattern | `(?i)netex`    |

OTP1 used to peek inside ZIP files and read the CSV tables to guess if a ZIP was indeed GTFS. Now
that we support remote input files (cloud storage or arbitrary URLs) not all data sources allow
seeking within files to guess what they are. Therefore, like all other file types GTFS is now
detected from a filename pattern. It is not sufficient to look for the `.zip` extension because
Netex data is also often supplied in a ZIP file.

### Configuration example

```JSON
// build-config.json 
{
  "localFileNamePatterns": {
    // All filenames that start with "g-" and end with ".zip" is imported as a GTFS file.
    "gtfs" : "^g-.*\\.zip$"
  }
}
```

## Limit the transit service period

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

## Reaching a subway platform

The ride locations for some modes of transport such as subways and airplanes can be slow to reach
from the street. When planning a trip, we need to allow additional time to reach these locations to
properly inform the passenger. For example, this helps avoid suggesting short bus rides between two
subway rides as a way to improve travel time. You can specify how long it takes to reach a subway
platform

```JSON
// build-config.json
{
  "subwayAccessTime": 2.5
}
```

Stops in GTFS do not necessarily serve a single transit mode, but in practice this is usually the
case. This additional access time will be added to any stop that is visited by trips on subway
routes (GTFS route_type = 1).

This setting does not generalize well to airplanes because you often need much longer to check in to
a flight (2-3 hours for international flights) than to alight and exit the airport (perhaps 1 hour).
Therefore there is currently no per-mode access time, it is subway-specific.

## Transferring within stations

Subway systems tend to exist in their own layer of the city separate from the surface, though there
are exceptions where tracks lie right below the street and transfers happen via the surface. In
systems where the subway is quite deep and transfers happen via tunnels, the time required for an
in-station transfer is often less than that for a surface transfer.

One way to resolve this problem is by ensuring that the GTFS feed codes each platform as a separate
stop, then micro-mapping stations in OSM. When OSM data contains a detailed description of walkways,
stairs, and platforms within a station, GTFS stops can be linked to the nearest platform and
transfers will happen via the OSM ways, which should yield very realistic transfer time
expectations. This works particularly well in above-ground train stations where the layering of
non-intersecting ways is less prevalent. See [BoardingLocations](BoardingLocations.md) for more details.

An alternative approach is to use GTFS pathways to model entrances and platforms within stations.

## Elevation data

OpenTripPlanner can "drape" the OSM street network over a digital elevation model (DEM). This allows
OTP to draw an elevation profile for the on-street portion of itineraries, and helps provide better
routing for bicyclists. It even helps avoid hills for walking itineraries. DEMs are usually supplied
as rasters
(regular grids of numbers) stored in image formats such as GeoTIFF.

### U.S. National Elevation Dataset

In the United States, a high resolution [National Elevation Dataset](http://ned.usgs.gov/) is
available for the entire territory. It used to be possible for OTP to download NED tiles on the fly
from a rather complex USGS SOAP service. This process was somewhat unreliable and would greatly slow
down the graph building process. In any case the service has since been replaced. But the USGS would
also deliver the whole dataset in bulk if
you [sent them a hard drive](https://web.archive.org/web/20150811051917/http://ned.usgs.gov:80/faq.html#DATA)
. We did this many years back and uploaded the entire data set to Amazon AWS S3. OpenTripPlanner
contains another module that can automatically fetch data in this format from any Amazon S3 copy of
the bulk data. You can configure it as follows in `build-config.json`:

```JSON
// router-config.json
{
    "elevationBucket": {
        "accessKey": "your-aws-access-key",
        "secretKey": "corresponding-aws-secret-key",
        "bucketName": "ned13"
    }
}
```

This `ned13` bucket is still available on S3 under a "requester pays" policy. As long as you specify
valid AWS account credentials you should be able to download tiles, and any bandwidth costs will be
billed to your AWS account.

Once the tiles are downloaded for a particular geographic area, OTP will keep them in local cache
for the next graph build operation. You should add the `--cache <directory>` command line parameter
to specify your NED tile cache location.

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
in the OTP client where appropriate to display elevations above sea level. This ellipsoid to geoid
difference is returned in each trip plan response in the
[ElevationMetadata](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/src/main/java/org/opentripplanner/api/resource/ElevationMetadata.java)
field. Using a single value can be sufficient for smaller OTP deployments, but might result in
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
particularly high resolution
(roughly 30 meters horizontally) but it can give acceptable results.

Simply place the elevation data file in the directory with the other graph builder inputs, alongside
the GTFS and OSM data. Make sure the file has a `.tiff` or `.tif` extension, and the graph builder
should detect its presence and apply the elevation data to the streets.

OTP should automatically handle DEM GeoTIFFs in most common projections. You may want to check for
elevation-related error messages during the graph build process to make sure OTP has properly
discovered the projection. If you are using a DEM in unprojected coordinates make sure that the axis
order is (longitude, latitude) rather than
(latitude, longitude). Unfortunately there is no reliable standard for WGS84 axis order, so OTP uses
the same axis order as the above-mentioned SRTM data, which is also the default for the popular
Proj4 library.

DEM files(USGS DEM) is not supported by OTP, but can be converted to GeoTIFF with tools
like [GDAL](http://www.gdal.org/). Use `gdal_merge.py -o merged.tiff *.dem` to merge a set of `dem`
files into one `tif` file.

See Interline [PlanetUtils](https://github.com/interline-io/planetutils) for a set of scripts to
download, merge, and
resample [Mapzen/Amazon Terrain Tiles](https://registry.opendata.aws/terrain-tiles/).

### Elevation unit conversion

By default, OTP expects the elevation data to use metres. However, by
setting `elevationUnitMultiplier` in `build-config.json`, it is possible to define a multiplier that
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
calculate all of the elevations took 5,509 seconds (roughly 1.5 hours).

If you are using cloud computing for your OTP instances, it is recommended to create prebuilt images
that contain the elevation data you need. This will save time because all of the data won't need to
be downloaded.

However, the bulk of the time will still be spent calculating elevations for all of the street
edges. Therefore, a further optimization can be done to calculate and save the elevation data during
a graph build and then save it for future use.

#### Reusing elevation data from previous builds

In order to write out the precalculated elevation data, add this to your `build-config.json` file:

```JSON
// build-config.json
{  
  "writeCachedElevations": true
}
```

After building the graph, a file called `cached_elevations.obj` will be written to the cache
directory. By default, this file is not written during graph builds. There is also a graph build
parameter called `readCachedElevations` which is set to `true` by default.

In graph builds, the elevation module will attempt to read the `cached_elevations.obj` file from the
cache directory. The cache directory defaults to `/var/otp/cache`, but this can be overriden via the
CLI argument `--cache <directory>`. For the same graph build for multiple Northeast US states, the
time it took with using this predownloaded and precalculated data became 543.7 seconds (roughly 9
minutes).

The cached data is a lookup table where the coordinate sequences of respective street edges are used
as keys for calculated data. It is assumed that all of the other input data except for the
OpenStreetMap data remains the same between graph builds. Therefore, if the underlying elevation
data is changed, or different configuration values for `elevationUnitMultiplier`
or `includeEllipsoidToGeoidDifference` are used, then this data becomes invalid and all elevation
data should be recalculated. Over time, various edits to OpenStreetMap will cause this cached data
to become stale and not include new OSM ways. Therefore, periodic update of this cached data is
recommended.

#### Configuring multi-threading during elevation calculations

For unknown reasons that seem to depend on data and machine settings, it might be faster to use a
single processor. For this reason, multi-threading of elevation calculations is only done
if `multiThreadElevationCalculations` is set to true. To enable multi-threading in the elevation
module, add the following to the `build-config.json` file:

```JSON
// build-config.json
{  
  "multiThreadElevationCalculations": true
}
```

## Fares configuration

By default OTP will compute fares according to the GTFS specification if fare data is provided in
your GTFS input. It is possible to turn off this by setting the fare to "off". For more complex
scenarios or to handle vehicle rental fares, it is necessary to manually configure fares using the
`fares` section in `build-config.json`. You can combine different fares (for example transit and
vehicle-rental) by defining a `combinationStrategy` parameter, and a list of sub-fares to combine
(all fields starting with `fare` are considered to be sub-fares).

```JSON
// build-config.json
{
  // Select the custom fare "seattle"
  "fares": "seattle"
}
```

Or this alternative form that could allow additional configuration

```JSON
// build-config.json
{
  "fares": {
	"type": "seattle"
  }
}
```

```JSON
// build-config.json
{
  "fares": {
    // Combine two fares by simply adding them
    "combinationStrategy": "additive",
    // First fare to combine
    "fare0": "new-york",
    // Second fare to combine
    "fare1": {
      "type": "vehicle-rental-time-based",
      "currency": "USD",
      "prices": {
          // For trip shorter than 30', $4 fare
          "30":   4.00,
          // For trip shorter than 1h, $6 fare
          "1:00": 6.00
      }
    }
    // We could also add fareFoo, fareBar...
  }
}
```

Turning the fare service _off_, this will ignore any fare data in the provided GTFS data.

```JSON
// build-config.json
{
  "fares": "off"
}
```

The current list of custom fare type is:

- `vehicle-rental-time-based` - accepting the following parameters:
    - `currency` - the ISO 4217 currency code to use, such as `"EUR"` or `"USD"`,
    - `prices` - a list of {time, price}. The resulting cost is the smallest cost where the elapsed
      time of vehicle rental is lower than the defined time.
- `san-francisco` (no parameters)
- `new-york` (no parameters)
- `seattle` (no parameters)
- `highest-fare-in-free-transfer-window` Will apply the highest observed transit fare (across all
  operators) within a free transfer window, adding to the cost if a trip is boarded outside the free
  transfer window. It accepts the following parameters:
    - `freeTransferWindow` the duration (in ISO8601-ish notation) that free transfers are
      possible after the board time of the first transit leg. Default: `2h30m`.
    - `analyzeInterlinedTransfers` If true, will treat interlined transfers as actual transfers.
      This is merely a work-around for transit agencies that choose to code their fares in a
      route-based fashion instead of a zone-based fashion. Default: `false`
- `atlanta` (no parameters)
- `combine-interlined-legs` Will treat two interlined legs (those with a stay-seated transfer in 
   between them) as a single leg for the purpose of fare calculation.
   It has a single parameter `mode` which controls when exactly the combination should happen:
    - `ALWAYS`: All interlined legs are combined. (default)
    - `SAME_ROUTE`: Only interlined legs whose route ID are identical are encountered.
- `off` (no parameters)

The current list of `combinationStrategy` is:

- `additive` - simply adds all sub-fares.

## OSM / OpenStreetMap configuration

It is possible to adjust how OSM data is interpreted by OpenTripPlanner when building the road part
of the routing graph.

### OSM tag mapping

OSM tags have different meanings in different countries, and how the roads in a particular country
or region are tagged affects routing. As an example roads tagged with `highway=trunk are (mainly)
walkable in Norway, but forbidden in some other countries. This might lead to OTP being unable to
snap stops to these roads, or by giving you poor routing results for walking and biking. You can
adjust which road types that are accessible by foot, car & bicycle as well as speed limits,
suitability for biking and walking. It's possible to define "safety" values for cycling and walking which are used in routing.

There are currently following OSM tag mapping defined;

- `default` which is based on California/US mapping standard
- `finland` which is adjusted to rules and speeds in Finland
- `norway` which is adjusted to rules and speeds in Norway
- `uk` which is adjusted to rules and speed in the UK
- `germany` which is adjusted to rules and speed in Germany
- `atlanta` which is adjusted to rules in Atlanta
- `houston` which is adjusted to rules in Houston

To add your own OSM tag mapping have a look
at `org.opentripplanner.graph_builder.module.osm.NorwayWayPropertySet`
and `org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySet`. If you choose to mainly
rely on the default rules, make sure you add your own rules first before applying the default ones.
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

### Custom naming

You can define a custom naming scheme for elements drawn from OSM by defining an `osmNaming` field
in `build-config.json`, such as:

```JSON
// build-config.json
{
  "osmNaming": "portland"
}
```

There is currently only one custom naming module called `portland` (which has no parameters).

## Parameter Details

<!-- PARAMETERS-DETAILS BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

<!-- PARAMETERS-DETAILS END -->
