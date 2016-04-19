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

## Reaching a subway platform

The boarding locations for some modes of transport such as subways and airplanes can be slow to reach from the street.
When planning a trip, we need to allow additional time to reach these locations to properly inform the passenger. For
example, this helps avoid suggesting short bus rides between two subway rides as a way to improve travel time. You can
specify how long it takes to reach a subway platform

```JSON
// build-config.json
{
  subwayAccessTime: 2.5
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
  stationTransfers: true
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
  fetchElevationUS: true
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
  fares: "seattle"
  // OR this alternative form that could allow additional configuration
  fares: {
	type: "seattle"
  }
}
```

```JSON
// build-config.json
{
  fares: {
    // Combine two fares by simply adding them
    combinationStrategy: "additive",
    // First fare to combine
    fare0: "new-york",
    // Second fare to combine
    fare1: {
      type: "bike-rental-time-based",
      currency: "USD",
      prices: {
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


## Custom OSM naming

You can define a custom naming scheme for elements drawn from OSM by defining an `osmNaming` field in `build-config.json`,
such as:

```JSON
// build-config.json
{
  osmNaming: "portland"
}
```

There is currently only one custom naming module called `portland` (which has no parameters).


# Runtime router configuration

This section covers all options that can be set for each router using the `router-config.json` file.
These options can be applied by the OTP server without rebuilding the graph.


## Routing defaults

There are many trip planning options used in the OTP web API, and more exist
internally that are not exposed via the API. You may want to change the default value for some of these parameters,
i.e. the value which will be applied unless it is overridden in a web API request.

A full list of them can be found in the RoutingRequest class
[in the Javadoc](http://dev.opentripplanner.org/javadoc/master/org/opentripplanner/routing/core/RoutingRequest.html).
Any public field or setter method in this class can be given a default value using the routingDefaults section of
`router-config.json` as follows:

```JSON
{
    routingDefaults: {
        walkSpeed: 2.0,
        stairsReluctance: 4.0,
        carDropoffTime: 240
    }
}
```

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
  boardTimes: {
    AIRPLANE: 2700
  },
  alightTimes: {
    AIRPLANE: 1200
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
  timeout: 5.5
}
```

This specifies a single timeout in (optionally fractional) seconds. Searching is aborted after this many seconds and any
paths already found are returned to the client. This is equivalent to specifying a `timeouts` array with a single element.
The alternative is:

```JSON
// router-config.json
{
  timeouts: [5, 4, 3, 1]
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
  requestLogFile: "/var/otp/request.log"
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
Bixi, the Dutch OVFiets system, and a generic KML format.
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

### Configuration

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
    routingDefaults: {
        numItineraries: 6,
        walkSpeed: 2.0,
        stairsReluctance: 4.0,
        carDropoffTime: 240
    },

    updaters: [

        // GTFS-RT service alerts (frequent polling)
        {
            type: "real-time-alerts",
            frequencySec: 30,
            url: "http://developer.trimet.org/ws/V1/FeedSpecAlerts/appID/0123456789ABCDEF",
            feedId: "TriMet"
        },

        // Polling bike rental updater.
        // sourceType can be: jcdecaux, b-cycle, bixi, keolis-rennes, ov-fiets,
        // city-bikes, citi-bike-nyc, next-bike, vcub, kml
        {
            type: "bike-rental",
            frequencySec: 300,
            sourceType: "city-bikes",
            url: "http://host.domain.tld"
        },

        <!--- San Francisco Bay Area bike share -->
        {
          "type": "bike-rental",
          "frequencySec": 300,
          "sourceType": "sf-bay-area",
          "url": "http://www.bayareabikeshare.com/stations/json"
        }

        // Polling bike rental updater for DC bikeshare (a Bixi system)
        // Negative update frequency means to run once and then stop updating (essentially static data)
        {
            type: "bike-rental",
            sourceType: "bixi",
            url: "https://www.capitalbikeshare.com/data/stations/bikeStations.xml",
            frequencySec: -1
		},

        // Bike parking availability
        {
            type: "bike-park"
        }

        // Polling for GTFS-RT TripUpdates)
        {
            type: "stop-time-updater",
            frequencySec: 60,
            // this is either http or file... shouldn't it default to http or guess from the presence of a URL?
            sourceType: "gtfs-http",
            url: "http://developer.trimet.org/ws/V1/TripUpdate/appID/0123456789ABCDEF",
            feedId: "TriMet"
        },

        // Streaming differential GTFS-RT TripUpdates over websockets
        {
            type: "websocket-gtfs-rt-updater"
        },

        // OpenTraffic data
        {
          "type": "opentraffic-updater",
          "frequencySec": -1,
          // relative to OTP's working directory, where is traffic data stored.
          // Should have subdirectories z/x/y.traffic.pbf (i.e. a tile tree of traffic tiles)
          "tileDirectory": "traffic"
        }
    ]
}
```
