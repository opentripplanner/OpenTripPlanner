### Cropping OSM data

Services producing automated extracts of OSM data like [Geofabrik](http://download.geofabrik.de)
or [Interline Extracts](https://www.interline.io/osm/extracts/) are limited to predefined areas.
You'll often need to download an extract for a country or region larger than your true analysis
area, then cut it down to size.

Excessively large OSM data can lead to significant increases in computation time and complexity,
both while building the graph and handling trip planning requests. You may want to crop the OSM data
if they cover an area significantly larger than your transit network. Several command line tools are
able to perform these cropping operations: [Osmosis](https://wiki.openstreetmap.org/wiki/Osmosis) is
a multi-platform Java tool that works on Windows, Linux, and MacOS but is relatively
slow, [OSMConvert](https://wiki.openstreetmap.org/wiki/Osmconvert) is a fast tool pre-built for
Windows and Linux and available on MacOS and Linux distributions as part of `osmctools`
package. [Osmium-Tool](https://wiki.openstreetmap.org/wiki/Osmium) is a personal favorite that is
extremely fast but only straightforward to install on Linux and MacOS platforms. Below are some
example crop commands for these different tools:

**Osmosis:** `osmosis --rb input.osm.pbf --bounding-box left=4.34 right=5.84 bottom=43.10 top=43.97 --wb cropped.osm.pbf`

**OsmConvert:** `osmconvert input.osm.pbf -b=-77.255859375,38.77764022307335,-76.81365966796875,39.02345139405933 --complete-ways -o=cropped.osm.pbf`

**Osmium:** `osmium extract --strategy complete_ways --bbox 2.25,48.81,2.42,48.91 input.osm.pbf -o cropped.osm.pbf`

The latter two commands expect bounding boxes to be specified in the
format `min_lon,min_lat,max_lon,max_lat`. We frequently find bounding boxes using the
convenient [Klokantech bounding box tool](https://boundingbox.klokantech.com/). Selecting the "CSV"
format in the lower left will give exactly the format expected by these tools.

### Filtering OSM data

The OSM database contains a lot of other data besides the roads, paths, and public transportation
platform data we need for accessibility analysis. As of this writing, according
to [TagInfo](https://taginfo.openstreetmap.org/) 59% of the ways in OSM are buildings, and only 23%
are roads or paths. Buildings frequently have more complex shapes than roads, and objects like
waterways or political boundaries can be very large in size. It has been jokingly said that OSM
should be renamed "OpenBuildingMap" rather than "OpenStreetMap".

Removing unneeded data will reduce file sizes, facilitating copying or moving files around and
reducing the size of project backups and archives. It may also speed up the processing stage where
the OSM data is converted into a routable street network. Several command line tools exist to filter
OSM data. Command line tools for this purpose
include [Osmosis](https://wiki.openstreetmap.org/wiki/Osmosis)
and [Osmium-Tool](https://osmcode.org/osmium-tool/). Osmium-Tool is extremely fast but is only
straightforward to install on Linux and MacOS platforms. Osmosis is often slower at filtering but
will also work on Windows as it's a multi-platform Java application. OSMFilter cannot work with PBF
format files so we rarely use it. Below are some example commands for retaining only OSM data useful
for accessibility analysis. Here are some example commands:

**Osmosis:** `osmosis --rb input.osm.pbf --tf reject-ways building=* --tf reject-ways waterway=* --tf reject-ways landuse=* --tf reject-ways natural=* --used-node --wb filtered.osm.pbf`

**Osmium-Tool:** `osmium tags-filter input.osm.pbf w/highway wa/public_transport=platform wa/railway=platform w/park_ride=yes r/type=restriction r/type=route -o filtered.osm.pbf -f pbf,add_metadata=false`
