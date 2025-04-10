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

<!-- INSERT: PARAMETERS-TABLE -->


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

<!-- INSERT: PARAMETERS-DETAILS -->


## Build Config Example

<!-- INSERT: JSON-EXAMPLE -->
