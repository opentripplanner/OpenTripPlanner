# Configuring OpenTripPlanner

## Base directory

The OTP *base directory* defaults to `/var/otp`. Unless you tell OTP otherwise, all other configuration,
input files and storage directories
will be sought immediately beneath this one. This prefix follows UNIX conventions so it should work in Linux and Mac OSX
environments, but it is inappropriate in Windows and where the user running OTP either cannot obtain permissions to
`/var` or simply wishes to experiment within his or her home directory rather than deploy a system-wide server.
In these cases one should use the basePath switch when starting up OTP to override the default. For example:
`--basePath /home/username/otp` on a Linux system, `--basePath /Users/username/otp` in Mac OSX, or
`--basePath C:\Users\username\otp` in Windows.

## Routers

A single OTP instance can handle several regions independently. Each of these separate (but potentially geographically overlapping)
services is called a *router* and is referred to by a short unique ID such as 'newyork' or 'paris'. Each router has its
own subdirectory in a directory called 'graphs' directly under the OTP base directory, and each router's directory is
always named after its router ID. Thus, by default the files for the router 'tokyo' will
be located at `/var/otp/graphs/tokyo`. Here is an example directory layout for an OTP instance with two routers, one for
New York City and one for Portland, Oregon:

```
/var/otp
├── cache
│   └── ned
└── graphs
    ├── nyc
    │   ├── build-config.json
    │   ├── Graph.obj
    │   ├── long-island-rail-road_20140216_0114.zip
    │   ├── mta-new-york-city-transit_20130212_0419.zip
    │   ├── new-york-city.osm.pbf
    │   └── port-authority-of-new-york-new-jersey_20150217_0111.zip
    └── pdx
        ├── build-config.json
        ├── Graph.obj
        ├── gtfs.zip
        ├── portland_oregon.osm.pbf
        └── router-config.json
```

You can see that each of these subdirectories contains one or more GTFS feeds (which are just zip files full of
comma-separated tables), a PBF street map file, some JSON configuration files, and another file called `Graph.obj`.
On startup, OTP scans router directories for input and configuration files,
and can optionally store the resulting combined representation of the transportation network as Graph.obj in the
same directory to avoid re-processing the data the next time it starts up. The `cache` directory is where OTP will
store its local copies of resources fetched from the internet, such as US elevation tiles.


## System-wide vs. graph build vs. router configuration

OTP is configured via JSON files. The file `otp-config.json` is placed in the OTP base directory and contains settings
that affect the entire OTP instance. Each router within that instance is configured using two other JSON files placed
alongside the input files (OSM, GTFS, elevation data etc.) in the router's directory. These router-level config files
are named `build-config.json` and `router-config.json`. Each configuration option within each of these files is optional,
as are all three of the files themselves. If any option or an entire file is missing, reasonable defaults will be applied.

Some parts of the process that loads the street and transit network description are time consuming and memory-hungry.
To avoid repeating these slow steps every time OTP starts up, we can trigger them manually whenever the input files change,
saving the resulting transportation network description to disk. We call this prepared product a *graph* (following
[mathematical terminology](https://en.wikipedia.org/wiki/Graph_%28mathematics%29)), and refer to these "heavier" steps as
*graph building*. They are controlled by `build-config.json`. There are many other details of OTP operation that can be
modified without requiring the potentially long operation of rebuilding the graph. These run-time configuration options
are found in `router-config.json`.

# Graph build configuration

This table lists the possible settings that can be defined in a `build-config.json` file. Sections follow that describe particular settings in more depth.

config key | description | value type | value default | notes
---------- | ----------- | ---------- | ------------- | -----
`htmlAnnotations` |  Generate nice HTML report of Graph errors/warnings (annotations) | boolean | false |
`transit` | Include all transit input files (GTFS) from scanned directory | boolean | true |
`useTransfersTxt` | Create direct transfer edges from transfers.txt in GTFS, instead of based on distance | boolean | false |
`parentStopLinking` | Link GTFS stops to their parent stops | boolean | false |
`stationTransfers` | Create direct transfers between the constituent stops of each parent station | boolean | false |
`stopClusterMode` | Stop clusters can be built in one of two ways, either by geographical proximity and name, or according to a parent/child station topology, if it exists | enum | `proximity` | options: `proximity`, `parentStation`
`subwayAccessTime` | Minutes necessary to reach stops served by trips on routes of `route_type=1` (subway) from the street | double | 2.0 | units: minutes
`streets` | Include street input files (OSM/PBF) | boolean | true | 
`embedRouterConfig` | Embed the Router config in the graph, which allows it to be sent to a server fully configured over the wire | boolean | true |
`areaVisibility` | Perform visibility calculations. If this is `true` OTP attempts to calculate a path straight through an OSM area using the shortest way rather than around the edge of it. (These calculations can be time consuming). | boolean | false |
`platformEntriesLinking` | Link unconnected entries to public transport platforms | boolean | false |
`matchBusRoutesToStreets` | Based on GTFS shape data, guess which OSM streets each bus runs on to improve stop linking | boolean | false |
`fetchElevationUS` | Download US NED elevation data and apply it to the graph | boolean | false |
`elevationBucket` | If specified, download NED elevation tiles from the given AWS S3 bucket | object | null | provide an object with `accessKey`, `secretKey`, and `bucketName` for AWS S3
`elevationUnitMultiplier` | Specify a multiplier to convert elevation units from source to meters | double | 1.0 | see [Elevation unit conversion](#elevation-unit-conversion)
`readCachedElevations` | If true, reads in pre-calculated elevation data. | boolean | true | see [Elevation Data Calculation Optimizations](#elevation-data-calculation-optimizations)
`writeCachedElevations` | If true, writes the calculated elevation data. | boolean | false | see [Elevation Data Calculation Optimizations](#elevation-data-calculation-optimizations)
`multiThreadElevationCalculations` | If true, the elevation module will use multi-threading during elevation calculations. | boolean | false | see [Elevation Data Calculation Optimizations](#elevation-data-calculation-optimizations)
`fares` | A specific fares service to use | object | null | see [fares configuration](#fares-configuration)
`osmNaming` | A custom OSM namer to use | object | null | see [custom naming](#custom-naming)
`osmWayPropertySet` | Custom OSM way properties | string | `default` | options: `default`, `norway`, `uk`
`staticBikeRental` | Whether bike rental stations should be loaded from OSM, rather than periodically dynamically pulled from APIs | boolean | false | 
`staticParkAndRide` | Whether we should create car P+R stations from OSM data | boolean | true | 
`staticBikeParkAndRide` | Whether we should create bike P+R stations from OSM data | boolean | false | 
`maxHtmlAnnotationsPerFile` | If number of annotations is larger then specified number annotations will be split in multiple files | int | 1,000 | 
`maxInterlineDistance` | Maximal distance between stops in meters that will connect consecutive trips that are made with same vehicle | int | 200 | units: meters
`islandWithoutStopsMaxSize` | Pruning threshold for islands without stops. Any such island under this size will be pruned | int | 40 | 
`islandWithStopsMaxSize` | Pruning threshold for islands with stops. Any such island under this size will be pruned | int | 5 | 
`banDiscouragedWalking` | should walking should be allowed on OSM ways tagged with `foot=discouraged"` | boolean | false | 
`banDiscouragedBiking` | should walking should be allowed on OSM ways tagged with `bicycle=discouraged"` | boolean | false | 
`maxTransferDistance` | Transfers up to this length in meters will be pre-calculated and included in the Graph | double | 2,000 | units: meters
`extraEdgesStopPlatformLink` | add extra edges when linking a stop to a platform, to prevent detours along the platform edge | boolean | false | 

This list of parameters in defined in the [code](https://github.com/opentripplanner/OpenTripPlanner/blob/master/src/main/java/org/opentripplanner/standalone/GraphBuilderParameters.java#L186-L215) for `GraphBuilderParameters`.

## Reaching a subway platform

The boarding locations for some modes of transport such as subways and airplanes can be slow to reach from the street.
When planning a trip, we need to allow additional time to reach these locations to properly inform the passenger. For
example, this helps avoid suggesting short bus rides between two subway rides as a way to improve travel time. You can
specify how long it takes to reach a subway platform

```JSON
// build-config.json
{
  "subwayAccessTime": 2.5
}
```

Stops in GTFS do not necessarily serve a single transit mode, but in practice this is usually the case. This additional
access time will be added to any stop that is visited by trips on subway routes (GTFS route_type = 1).

This setting does not generalize well to airplanes because you often need much longer to check in to a flight (2-3 hours
for international flights) than to alight and exit the airport (perhaps 1 hour). Therefore there is currently no
per-mode access time, it is subway-specific.

## Transferring within stations

Subway systems tend to exist in their own layer of the city separate from the surface, though there are exceptions where
tracks lie right below the street and transfers happen via the surface. In systems where the subway is quite deep
and transfers happen via tunnels, the time required for an in-station transfer is often less than that for a
surface transfer. A proposal was made to provide detailed station pathways in GTFS but it is not in common use.

One way to resolve this problem is by ensuring that the GTFS feed codes each platform as a separate stop, then
micro-mapping stations in OSM. When OSM data contains a detailed description of walkways, stairs, and platforms within
a station, GTFS stops can be linked to the nearest platform and transfers will happen via the OSM ways, which should
yield very realistic transfer time expectations. This works particularly well in above-ground train stations where
the layering of non-intersecting ways is less prevalent. Here's an example in the Netherlands:

<iframe width="425" height="350" frameborder="0" scrolling="no" marginheight="0" marginwidth="0" src="http://www.openstreetmap.org/export/embed.html?bbox=4.70502644777298%2C52.01675028000761%2C4.7070810198783875%2C52.01813190694357&amp;layer=mapnik" style="border: 1px solid black"></iframe><small><a href="http://www.openstreetmap.org/#map=19/52.01744/4.70605">View Larger Map</a></small>
When such micro-mapping data is not available, we need to rely on information from GTFS including how stops are grouped
into stations and a table of transfer timings where available. During the graph build, OTP can create preferential
connections between each pair of stops in the same station to favor in-station transfers:

```JSON
// build-config.json
{
  "stationTransfers": true
}
```

Note that this method is at odds with micro-mapping and might make some transfers artificially short.


## Elevation data

OpenTripPlanner can "drape" the OSM street network over a digital elevation model (DEM).
This allows OTP to draw an elevation profile for the on-street portion of itineraries, and helps provide better
routing for bicyclists. It even helps avoid hills for walking itineraries. DEMs are usually supplied as rasters
(regular grids of numbers) stored in image formats such as GeoTIFF.

### U.S. National Elevation Dataset

In the United States, a high resolution [National Elevation Dataset](http://ned.usgs.gov/) is available for the entire
territory. The US Geological Survey (USGS) delivers this dataset in tiles via a somewhat awkward heavyweight web-based GIS
which generates and emails you download links. OpenTripPlanner contains a module which will automatically contact this
service and download the proper tiles to completely cover your transit and street network area. This process is rather
slow (download is around 1.5 hours, then setting elevation for streets takes about 5 minutes for the Portland, Oregon region),
but once the tiles are downloaded OTP will keep them in local cache for the next graph build operation.

To auto-download NED tiles when building your graph, add the following line to `build-config.json` in your router
directory:

```JSON
// build-config.json
{
  "fetchElevationUS": true
}
```

You may also want to add the `--cache <directory>` command line parameter to specify a custom NED tile cache location.

NED downloads take quite a long time and slow down the graph building process. The USGS will also deliver the
whole dataset in bulk if you [send them a hard drive](http://ned.usgs.gov/faq.html#DATA). OpenTripPlanner contains
another module that will then automatically fetch data in this format from an Amazon S3 copy of your bulk data.
You can configure it as follows in `build-config.json`:

```JSON
{
    "elevationBucket" : {
        "accessKey" : "your-aws-access-key",
        "secretKey" : "corresponding-aws-secret-key",
        "bucketName" : "ned13"
    }
}
```

### Geoid Difference

With some elevation data, the elevation values are specified as relative to the a geoid (irregular estimate of mean sea level). See [issue #2301](https://github.com/opentripplanner/OpenTripPlanner/issues/2301) for detailed discussion of this. In these cases, it is necessary to also add this geoid value onto the elevation value to get the correct result. OTP can automatically calculate these values in one of two ways. 

The first way is to use the geoid difference value that is calculated once at the center of the graph. This value is returned in each trip plan response in the [ElevationMetadata](http://dev.opentripplanner.org/apidoc/1.4.0/json_ElevationMetadata.html) field. Using a single value can be sufficient for smaller OTP deployments, but might result in incorrect values at the edges of larger OTP deployments. If your OTP instance uses this, it is recommended to set a default request value in the `router-config.json` file as follows:

```JSON
// router-config.json
{
    "routingDefaults": {
        "geoidElevation ": true   
    }
}
```

The second way is to precompute these geoid difference values at a more granular level and include them when calculating elevations for each sampled point along each street edge. In order to speed up calculations, the geoid difference values are calculated and cached using only 2 significant digits of GPS coordinates. This is more than enough detail for most regions of the world and should result in less than one meter of difference in areas that have large changes in geoid difference values. To enable this, include the following in the `build-config.json` file: 

```JSON
// build-config.json
{
  "includeEllipsoidToGeoidDifference": true
}
```

If the geoid difference values are precomputed, be careful to not set the routing resource value of `geoidElevation` to true in order to avoid having the graph-wide geoid added again to all elevation values in the relevant street edges in responses.

### Other raster elevation data

For other parts of the world you will need a GeoTIFF file containing the elevation data. These are often available from
national geographic surveys, or you can always fall back on the worldwide
[Space Shuttle Radar Topography Mission](http://www2.jpl.nasa.gov/srtm/) (SRTM) data. This not particularly high resolution
(roughly 30 meters horizontally) but it can give acceptable results.

Simply place the elevation data file in the directory with the other graph builder inputs, alongside the GTFS and OSM data.
Make sure the file has a `.tiff` or `.tif` extension, and the graph builder should detect its presence and apply
the elevation data to the streets.

OTP should automatically handle DEM GeoTIFFs in most common projections. You may want to check for elevation-related
error messages during the graph build process to make sure OTP has properly discovered the projection. If you are using
a DEM in unprojected coordinates make sure that the axis order is (longitude, latitude) rather than
(latitude, longitude). Unfortunately there is no reliable standard for WGS84 axis order, so OTP uses the same axis
order as the above-mentioned SRTM data, which is also the default for the popular Proj4 library.

DEM files(USGS DEM) is not supported by OTP, but can be converted to GeoTIFF with tools like [GDAL](http://www.gdal.org/). 
Use `gdal_merge.py -o merged.tiff *.dem` to merge a set of `dem` files into one `tif` file.

See Interline [PlanetUtils](https://github.com/interline-io/planetutils) for a set of scripts to download, merge, and resample [Mapzen/Amazon Terrain Tiles](https://registry.opendata.aws/terrain-tiles/).

### Elevation unit conversion

By default, OTP expects the elevation data to use metres. However, by setting `elevationUnitMultiplier` in `build-config.json`,
it is possible to define a multiplier that converts the elevation values from some other unit to metres.

```JSON
// build-config.json
{
  // Correct conversation multiplier when source data uses decimetres instead of metres
  "elevationUnitMultiplier": 0.1
}
```

### Elevation Data Calculation Optimizations

Calculating elevations on all StreetEdges can take a dramatically long time. In a very large graph build for multiple Northeast US states, the time it took to download the elevation data and calculate all of the elevations took 5,509 seconds (roughly 1.5 hours).

If you are using cloud computing for your OTP instances, it is recommended to create prebuilt images that contain the elevation data you need. This will save time because all of the data won't need to be downloaded.

However, the bulk of the time will still be spent calculating elevations for all of the street edges. Therefore, a further optimization can be done to calculate and save the elevation data during a graph build and then save it for future use.

#### Reusing elevation data from previous builds

In order to write out the precalculated elevation data, add this to your `build-config.json` file:

```JSON
// build-config.json
{  
  "writeCachedElevations": true
}
```

After building the graph, a file called `cached_elevations.obj` will be written to the cache directory. By default, this file is not written during graph builds. There is also a graph build parameter called `readCachedElevations` which is set to `true` by default.

In graph builds, the elevation module will attempt to read the `cached_elevations.obj` file from the cache directory. The cache directory defaults to `/var/otp/cache`, but this can be overriden via the CLI argument `--cache <directory>`. For the same graph build for multiple Northeast US states, the time it took with using this predownloaded and precalculated data became 543.7 seconds (roughly 9 minutes).

The cached data is a lookup table where the coordinate sequences of respective street edges are used as keys for calculated data. It is assumed that all of the other input data except for the OpenStreetMap data remains the same between graph builds. Therefore, if the underlying elevation data is changed, or different configuration values for `elevationUnitMultiplier` or `includeEllipsoidToGeoidDifference` are used, then this data becomes invalid and all elevation data should be recalculated. Over time, various edits to OpenStreetMap will cause this cached data to become stale and not include new OSM ways. Therefore, periodic update of this cached data is recommended.

#### Configuring multi-threading during elevation calculations

For unknown reasons that seem to depend on data and machine settings, it might be faster to use a single processor. For this reason, multi-threading of elevation calculations is only done if `multiThreadElevationCalculations` is set to true. To enable multi-threading in the elevation module, add the following to the `build-config.json` file:
                                                               
```JSON
// build-config.json
{  
  "multiThreadElevationCalculations": true
}
```

## Fares configuration

By default OTP will compute fares according to the GTFS specification if fare data is provided in your GTFS input.
For more complex scenarios or to handle bike rental fares, it is necessary to manually configure fares using the
`fares` section in `build-config.json`. You can combine different fares (for example transit and bike-rental)
by defining a `combinationStrategy` parameter, and a list of sub-fares to combine (all fields starting with `fare`
are considered to be sub-fares).

```JSON
// build-config.json
{
  // Select the custom fare "seattle"
  "fares": "seattle",
  // OR this alternative form that could allow additional configuration
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
      "type": "bike-rental-time-based",
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

The current list of custom fare type is:

- `bike-rental-time-based` - accepting the following parameters:
    - `currency` - the ISO 4217 currency code to use, such as `"EUR"` or `"USD"`,
    - `prices` - a list of {time, price}. The resulting cost is the smallest cost where the elapsed time of bike rental is lower than the defined time.
- `san-francisco` (no parameters)
- `new-york` (no parameters)
- `seattle` (no parameters)

The current list of `combinationStrategy` is:

- `additive` - simply adds all sub-fares.

## OSM / OpenStreetMap configuration

It is possible to adjust how OSM data is interpreted by OpenTripPlanner when building the road part of the routing graph.

### Way property sets

OSM tags have different meanings in different countries, and how the roads in a particular country or region are tagged affects routing. As an example are roads tagged with `highway=trunk (mainly) walkable in Norway, but forbidden in some other countries. This might lead to OTP being unable to snap stops to these roads, or by giving you poor routing results for walking and biking.
You can adjust which road types that are accessible by foot, car & bicycle as well as speed limits, suitability for biking and walking.

There are currently 3 wayPropertySets defined;

- `default` which is based on California/US mapping standard
- `norway` which is adjusted to rules and speeds in Norway
- `uk` which is adjusted to rules and speed in the UK

To add your own custom property set have a look at `org.opentripplanner.graph_builder.module.osm.NorwayWayPropertySet` and `org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySet`. If you choose to mainly rely on the default rules, make sure you add your own rules first before applying the default ones. The mechanism is that for any two identical tags, OTP will use the first one.

```JSON
// build-config.json
{
  osmWayPropertySet: "norway"
}
```


### Custom naming

You can define a custom naming scheme for elements drawn from OSM by defining an `osmNaming` field in `build-config.json`,
such as:

```JSON
// build-config.json
{
  "osmNaming": "portland"
}
```

There is currently only one custom naming module called `portland` (which has no parameters).


# Runtime router configuration

This section covers all options that can be set for each router using the `router-config.json` file.
These options can be applied by the OTP server without rebuilding the graph.

config key | description | value type | value default | notes
---------- | ----------- | ---------- | ------------- | -----
`routingDefaults` | Default routing parameters, which will be applied to every request | object |  | see [routing defaults](#routing-defaults)
`timeout` | maximum time limit for route queries | double | null | units: seconds; see [timeouts](#timeouts)
`timeouts` | when returning multiple itineraries, set different maximum time limits for the 1st, 2nd, etc. itinerary | array of doubles | `[5, 4, 2]` | units: seconds; see [timeouts](#timeouts)
`requestLogFile` | Path to a plain-text file where requests will be logged | string | null | see [logging incoming requests](#logging-incoming-requests)
`boardTimes` | change boarding times by mode | object | null | see [boarding and alighting times](#boarding-and-alighting-times)
`alightTimes` | change alighting times by mode | object | null | see [boarding and alighting times](#boarding-and-alighting-times)
`updaters` | configure real-time updaters, such as GTFS-realtime feeds | object | null | see [configuring real-time updaters](#configuring-real-time-updaters)

## Routing defaults

There are many trip planning options used in the OTP web API, and more exist
internally that are not exposed via the API. You may want to change the default value for some of these parameters,
i.e. the value which will be applied unless it is overridden in a web API request.

A full list of them can be found in the RoutingRequest class
[in the Javadoc](http://dev.opentripplanner.org/javadoc/1.4.0/org/opentripplanner/routing/core/RoutingRequest.html).
Any public field or setter method in this class can be given a default value using the routingDefaults section of
`router-config.json` as follows:

```JSON
{
    "routingDefaults": {
        "walkSpeed": 2.0,
        "stairsReluctance": 4.0,
        "carDropoffTime": 240
    }
}
```

## Routing modes

The routing request parameter `mode` determines which transport modalities should be considered when calculating the list
of routes.

Some modes (mostly bicycle and car) also have optional qualifiers `RENT` and `PARK` to specify if vehicles are to be parked at a station or rented. In theory
this can also apply to other modes but makes sense only in select cases which are listed below.

Whether a transport mode is available highly depends on the input feeds (GTFS, OSM, bike sharing feeds) and the graph building options supplied to OTP.

The complete list of modes are:

- `WALK`: Walking some or all of the route.

- `TRANSIT`: General catch-all for all public transport modes.

- `BICYCLE`: Cycling for the entirety of the route or taking a bicycle onto the public transport and cycling from the arrival station to the destination.

- `BICYCLE_RENT`: Taking a rented, shared-mobility bike for part or the entirety of the route.  

    _Prerequisite:_ Vehicle positions need to be added to OTP either as static stations or dynamic data feeds. 

    For dynamic bike positions configure an input feed. See [Configuring real-time updaters](Configuration.md#configuring-real-time-updaters).

    For static stations check the graph building documentation for the property `staticBikeRental`.

- `BICYCLE_PARK`: Leaving the bicycle at the departure station and walking from the arrival station to the destination.

    This mode needs to be combined with at least one transit mode (or `TRANSIT`) otherwise it behaves like an ordinary bicycle journey.

    _Prerequisite:_ Bicycle parking stations present in the OSM file and visible to OTP by enabling the property `staticBikeParkAndRide` during graph build.

- `CAR`: Driving your own car the entirety of the route. 

    If this is combined with `TRANSIT` it will return routes with a 
    [Kiss & Ride](https://en.wikipedia.org/wiki/Park_and_ride#Kiss_and_ride_/_kiss_and_fly) component. This means that the car is not parked in a permanent
    parking area but rather the passenger is dropped off (for example, at an airport) and the driver continues driving the car away from the drop off
    location.

- `CAR_PARK`: Driving a car to the park-and-ride facilities near a station and taking public transport.

    This mode needs to be combined with at least one transit mode (or `TRANSIT`) otherwise it behaves like an ordinary car journey.

    _Prerequisite:_ Park-and-ride areas near the station need to be present in the OSM input file.


The following modes are 1-to-1 mappings from the [GTFS `route_type`](https://developers.google.com/transit/gtfs/reference/#routestxt):

- `TRAM`: Tram, streetcar, or light rail. Used for any light rail or street-level system within a metropolitan area.

- `SUBWAY`: Subway or metro. Used for any underground rail system within a metropolitan area.

- `RAIL`: Used for intercity or long-distance travel.

- `BUS`: Used for short- and long-distance bus routes.

- `FERRY`: Ferry. Used for short- and long-distance boat service.

- `CABLE_CAR`: Cable car. Used for street-level cable cars where the cable runs beneath the car.

- `GONDOLA`: Gondola or suspended cable car. Typically used for aerial cable cars where the car is suspended from the cable.

- `FUNICULAR`: Funicular. Used for any rail system that moves on steep inclines with a cable traction system.

Lastly, this mode is part of the [Extended GTFS route types](https://developers.google.com/transit/gtfs/reference/extended-route-types):

- `AIRPLANE`: Taking an airplane.

Note that there are conceptual overlaps between `TRAM`, `SUBWAY` and `RAIL` and some transport providers categorize their routes differently to others.
In other words, what is considered a `SUBWAY` in one city might be of type `RAIL` in another. Study your input GTFS feed carefully to 
find out the appropriate mapping in your region.

### Drive-to-transit routing defaults

When using the "park and ride" or "kiss and ride" modes (drive to transit), the initial driving time to reach a transit
stop or park and ride facility is constrained. You can set a drive time limit in seconds by adding a line like
`maxPreTransitTime = 1200` to the routingDefaults section. If the limit is too high on a very large street graph, routing
performance may suffer.


## Boarding and alighting times

Sometimes there is a need to configure a longer boarding or alighting times for specific modes, such as airplanes or ferries,
where the check-in process needs to be done in good time before boarding. The boarding time is added to the time when going
from the stop (offboard) vertex to the onboard vertex, and the alight time is added vice versa. The times are configured as
seconds needed for the boarding and alighting processes in `router-config.json` as follows:

```JSON
{
  "boardTimes": {
    "AIRPLANE": 2700
  },
  "alightTimes": {
    "AIRPLANE": 1200
  }
}
```

## Timeouts

Path searches can sometimes take a long time to complete, especially certain problematic cases that have yet to be optimized.
Often a first itinerary is found quickly, but it is time-consuming or impossible to find subsequent alternative itineraries
and this delays the response. You can set timeouts to avoid tying up server resources on pointless searches and ensure that
your users receive a timely response. When a search times out, a WARN level log entry is made with information that can
help identify problematic searches and improve our routing methods. The simplest timeout option is:

```JSON
// router-config.json
{
  "timeout": 5.5
}
```

This specifies a single timeout in (optionally fractional) seconds. Searching is aborted after this many seconds and any
paths already found are returned to the client. This is equivalent to specifying a `timeouts` array with a single element.
The alternative is:

```JSON
// router-config.json
{
  "timeouts": [5, 4, 3, 1]
}
```

Here, the configuration key is `timeouts` (plural) and we specify an array of times in floating-point seconds. The Nth
element in the array applies to the Nth itinerary search, and importantly all values are relative to the beginning of the
search for the *first* itinerary. If OTP is configured to find more itineraries than there are elements in the timeouts
array, the final element in the timeouts array will apply to all remaining unmatched searches.

This allows you to keep overall response time down while ensuring that the end user will get at least one
response, providing more only when it won't hurt response time. The timeout values will typically be decreasing to
reflect the decreasing marginal value of alternative itineraries: everyone wants at least one response, it's nice to
have two for comparison, but we only care about having three, four, or more options if completing those extra searches
doesn't cause annoyingly long response times.

## Logging incoming requests

You can log some characteristics of trip planning requests in a file for later analysis. Some transit agencies and
operators find this information useful for identifying existing or unmet transportation demand. Logging will be
performed only if you specify a log file name in the router config:

```JSON
// router-config.json
{
  "requestLogFile": "/var/otp/request.log"
}
```

Each line in the resulting log file will look like this:

`2016-04-19T18:23:13.486 0:0:0:0:0:0:0:1 ARRIVE 2016-04-07T00:17 WALK,BUS,CABLE_CAR,TRANSIT,BUSISH 45.559737193889966 -122.64999389648438 45.525592487765635 -122.39044189453124 6095 3 5864 3 6215 3`

The fields are separated by whitespace and are (in order):

1. Date and time the request was received
2. IP address of the user
3. Arrive or depart search
4. The arrival or departure time
5. A comma-separated list of all transport modes selected
6. Origin latitude and longitude
7. Destination latitude and longitude

Finally, for each itinerary returned to the user, there is a travel duration in seconds and the number of transit vehicles used in that itinerary.


## Real-time data

GTFS feeds contain *schedule* data that is is published by an agency or operator in advance. The feed does not account
 for unexpected service changes or traffic disruptions that occur from day to day. Thus, this kind of data is also
 referred to as 'static' data or 'theoretical' arrival and departure times.

### GTFS-Realtime

The [GTFS-RT spec](https://developers.google.com/transit/gtfs-realtime/) complements GTFS with three additional kinds of
feeds. In contrast to the base GTFS schedule feed, they provide *real-time* updates (*'dynamic'* data) and are are
updated from minute to minute.

- **Alerts** are text messages attached to GTFS objects, informing riders of disruptions and changes.

- **TripUpdates** report on the status of scheduled trips as they happen, providing observed and predicted arrival and
departure times for the remainder of the trip.

- **VehiclePositions** give the location of some or all vehicles currently in service, in terms of geographic coordinates
or position relative to their scheduled stops.

### Bicycle rental systems

Besides GTFS-RT transit data, OTP can also fetch real-time data about bicycle rental networks including the number
of bikes and free parking spaces at each station. We support bike rental systems from JCDecaux, BCycle, VCub, Keolis,
Bixi, the Dutch OVFiets system, ShareBike, GBFS and a generic KML format.
It is straightforward to extend OTP to support any bike rental system that
exposes a JSON API or provides KML place markers, though it requires writing a little code.

The generic KML needs to be in format like

```XML
<?xml version="1.0" encoding="utf-8" ?>
<kml xmlns="http://www.opengis.net/kml/2.2">
<Document id="root_doc">
<Schema name="citybikes" id="citybikes">
    <SimpleField name="ID" type="int"></SimpleField>
</Schema>
  <Placemark>
    <name>A Bike Station</name>
    <ExtendedData><SchemaData schemaUrl="#citybikes">
        <SimpleData name="ID">0</SimpleData>
    </SchemaData></ExtendedData>
      <Point><coordinates>24.950682884886643,60.155923430488102</coordinates></Point>
  </Placemark>
</Document></kml>
```

### Configuring real-time updaters

Real-time data can be provided using either a pull or push system. In a pull configuration, the GTFS-RT consumer polls the
real-time provider over HTTP. That is to say, OTP fetches a file from a web server every few minutes. In the push
configuration, the consumer opens a persistent connection to the GTFS-RT provider, which then sends incremental updates
immediately as they become available. OTP can use both approaches. The [OneBusAway GTFS-realtime exporter project](https://github.com/OneBusAway/onebusaway-gtfs-realtime-exporter) provides this kind of streaming, incremental updates over a websocket rather than a single large file.

Real-time data sources are configured in `router-config.json`. The `updaters` section is an array of JSON objects, each
of which has a `type` field and other configuration fields specific to that type. Common to all updater entries that
connect to a network resource is the `url` field.

```JSON
// router-config.json
{
    // Routing defaults are any public field or setter in the Java class
    // org.opentripplanner.routing.core.RoutingRequest
    "routingDefaults": {
        "numItineraries": 6,
        "walkSpeed": 2.0,
        "stairsReluctance": 4.0,
        "carDropoffTime": 240
    },

    "updaters": [

        // GTFS-RT service alerts (frequent polling)
        {
            "type": "real-time-alerts",
            "frequencySec": 30,
            "url": "http://developer.trimet.org/ws/V1/FeedSpecAlerts/appID/0123456789ABCDEF",
            "feedId": "TriMet"
        },

        // Polling bike rental updater.
        // sourceType can be: jcdecaux, b-cycle, bixi, keolis-rennes, ov-fiets,
        // city-bikes, citi-bike-nyc, next-bike, vcub, kml
        {
            "type": "bike-rental",
            "frequencySec": 300,
            "sourceType": "city-bikes",
            "url": "http://host.domain.tld"
        },

        //<!--- San Francisco Bay Area bike share -->
        {
          "type": "bike-rental",
          "frequencySec": 300,
          "sourceType": "sf-bay-area",
          "url": "http://www.bayareabikeshare.com/stations/json"
        },

        //<!--- Tampa Area bike share -->
        {
          "type": "bike-rental",
          "frequencySec": 300,
          "sourceType": "gbfs",
          "url": "http://coast.socialbicycles.com/opendata/"
        },


        // Polling bike rental updater for DC bikeshare (a Bixi system)
        // Negative update frequency means to run once and then stop updating (essentially static data)
        {
            "type": "bike-rental",
            "sourceType": "bixi",
            "url": "https://www.capitalbikeshare.com/data/stations/bikeStations.xml",
            "frequencySec": -1
		},

        // Bike parking availability
        {
            "type": "bike-park"
        },

        // Polling for GTFS-RT TripUpdates)
        {
            "type": "stop-time-updater",
            "frequencySec": 60,
            // this is either http or file... shouldn't it default to http or guess from the presence of a URL?
            "sourceType": "gtfs-http",
            "url": "http://developer.trimet.org/ws/V1/TripUpdate/appID/0123456789ABCDEF",
            "feedId": "TriMet"
        },

        // Streaming differential GTFS-RT TripUpdates over websockets
        {
            "type": "websocket-gtfs-rt-updater"
        }
    ]
}
```
#### GBFS Configuration

Steps to add a GBFS feed to a router:

- Add one entry in the `updater` field of `router-config.json` in the format

```JSON
{
     "type": "bike-rental",
     "frequencySec": 60,
     "sourceType": "gbfs",
     "url": "http://coast.socialbicycles.com/opendata/"
}
```

- Follow these instructions to fill these fields:

```
type: "bike-rental"
frequencySec: frequency in seconds in which the GBFS service will be polled
sourceType: "gbfs"
url: the URL of the GBFS feed (do not include the gbfs.json at the end) *
```
\* For a list of known GBFS feeds see the [list of known GBFS feeds](https://github.com/NABSA/gbfs/blob/master/systems.csv)

# Configure using command-line arguments

Certain settings can be provided on the command line, when starting OpenTripPlanner. See the `CommandLineParameters` class for [a full list of arguments](http://dev.opentripplanner.org/javadoc/1.4.0/org/opentripplanner/standalone/CommandLineParameters.html).
