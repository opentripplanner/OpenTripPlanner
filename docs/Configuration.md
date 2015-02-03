# Configuration

## Elevation data 

OpenTripPlanner can "drape" the OSM street network over a digital elevation model (DEM).
This allows OTP to draw an elevation profile for the on-street portion of itineraries, and helps provide better
routing for bicyclists. DEMs are usually supplied as rasters (regular grids) stored in image formats such as GeoTIFF.

### U.S. National Elevation Dataset

In the United States, a high resolution [National Elevation Dataset](http://ned.usgs.gov/) is available for the entire
territory. The US Geological Survey (USGS) delivers this dataset in tiles via a somewhat awkward heavyweight web-based GIS
which generates and emails you download links. OpenTripPlanner contains a module which will automatically contact this
service and download the proper tiles to completely cover your transit and street network area. This process is rather
slow (download is around 1.5 hours, then setting elevation for streets takes about 5 minutes for the Portland, Oregon region),
but once the tiles are downloaded OTP will keep them in local cache for the next graph build operation.

To auto-download NED tiles when building your graph, add the `--elevation` switch to the command line. You may also want
to add the `--cache <directory>` parameter to specify a custom NED tile cache location. For example:

```
./otp --build --elevation --cache /home/myname/nedcache
```

The USGS will also deliver the whole dataset in bulk if you [send them a hard drive](http://ned.usgs.gov/faq.html#DATA).
OpenTripPlanner contains another module that will then automatically fetch data in this format from an Amazon S3 copy of
your bulk data.


### Other raster elevation data

For other parts of the world you will need a GeoTIFF file containing the elevation data. These are often available from
national geographic surveys, or you can always fall back on the worldwide
[Space Shuttle Radar Topography Mission](http://www2.jpl.nasa.gov/srtm/) (SRTM) data. This not particularly high resolution
(roughly 30 meters horizontally) but it can give acceptable results.

Simply place the elevation data file in the directory with the other graph builder inputs, alongside the GTFS and OSM data.
Make sure the file has a `.tiff` extension, and the graph builder should detect its presence and apply
the elevation data to the streets.


## Real-time data

GTFS feeds contain *schedule* data that is is published by an agency or operator in advance. The feed does not account
 for unexpected service changes or traffic disruptions that occur from day to day. Thus, this kind of data is also
 referred to as 'static' data or 'theoretical' arrival and departure times.

The [GTFS Realtime spec]() (GTFS-RT) complements GTFS with three additional kinds of feeds. In contrast to the
base GTFS schedule feed, they provide *real-time* updates (*"dynamic"* data) and are are updated from minute to minute.

- **Alerts** are text messages attached to GTFS objects, informing riders of disruptions and changes.

- **TripUpdates** report on the status of scheduled trips as they happen, providing observed and predicted arrival and
departure times for the remainder of the trip.

- **VehiclePositions** give the location of some or all vehicles currently in service, in terms of geographic coordinates
or position relative to their scheduled stops.

These feeds can be provided using either a pull or push system. In a pull configuration, the GTFS-RT consumer (OTP) polls the
real-time provider over HTTP. That is to say, it fetches a file from a web server every few minutes. In the push configuration,
the consumer opens a persistent connection to the GTFS-RT provider, which then sends incremental updates immediately as
they become available. OTP can use both approaches.


### Alerts


### Trip Updates


### Vehicle Positions


```JSON
{
    updaters: [
        // GTFS-RT service alerts (polling)
        {
            type: "real-time-alerts",
            frequencySec: 30,
            url: "http://developer.trimet.org/ws/V1/FeedSpecAlerts/appID/7D841E367D34BA85B11326EB3",
            defaultAgencyId: "TriMet"
        },

        // Bike rental updater for Citybikes (polling)
        {
            type: "bike-rental",
            frequencySec: 300,
            sourceType: "city-bikes",
            url: "http://host.domain.tld"
        },

        // Bike parking availability
        // bp.type=bike-park

        // Stop Time Updates (polling GTFS-RT TripUpdates)
        {
            type: "stop-time-updater",
            frequencySec: 60,
            // this is either http or file... shouldn't it default to http or guess from the presence of a URL?
            sourceType: "gtfs-http",
            url: "http://developer.trimet.org/ws/V1/TripUpdate/appID/7D841E367D34BA85B11326EB3",
            defaultAgencyId: "TriMet"
        },

        // Stop Time Updates (streaming differential GTFS-RT TripUpdates)
        {
            type: "websocket-gtfs-rt-updater"
        }
    ]
}
```