# Entur Deployment Configuration

This is a snapshot of Enturs deployment configuration. At Entur we run OTP in the cloud, so some of
the provided config will not work outside Enturs cluster, but it is provided "as is" for others to
replicate if they want.

## Config files

See the config files provided. The `updaters` section of the `router-config.json` is provided, but
is not working. Remove it if you want to run OTP. It is provided for others as an example on how to
configure the SIRI updaters. The same goes for the `storage` section in the `build-config.json`,
remove it run OTP locally.

The `<host>`, `<OperatorNameSpace>` and `${GCS_BUCKET}` are placeholders you need to change.

## Data input files

At Entur we run OTP with the latest NeTEx data we have. You may download it from here:

https://developer.entur.org/stops-and-timetable-data

We use
the [Entire Norway](https://storage.googleapis.com/marduk-production/outbound/netex/rb_norway-aggregated-netex.zip)
file.

In the past the file did not contain the stops, so they needed to be downloaded separably (Entire
Norway (Current stops) - Latest valid version of all country stops) and inserted into the
Netex-file. Unpack the stops zipfile, rename the stops file to `_stops.xml`. Unpack the netex file
and move the `_stops.xml` into the netex directory. Copy the netex directory and config files into
the same directory and start OTP with it as the base directory.

We also build with elevation data, which is not available on the internet without transformation.
Send us a request, and we will find a way to share it.

We download the OSM data
file [norway-latest.osm.pbf](https://download.geofabrik.de/europe/norway.html) every night and build
a street-graph with OSM and elevation data. We also use some custom OSM files for areas outside
Norway, but they in most cases insignificant. If requested, we can provide them.
 