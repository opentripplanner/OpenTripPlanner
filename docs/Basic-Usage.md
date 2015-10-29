# Basic Usage of OpenTripPlanner

This page will get you up and running with your own OTP server. If all goes well it should only take a few minutes!

## Get OTP

OpenTripPlanner is written in Java and distributed as a single runnable JAR file. These JARs are published on the Conveyal Maven repository [here](http://maven.conveyal.com/org/opentripplanner/otp/). Grab one of these JARs for the [latest released version 0.19](http://maven.conveyal.com.s3.amazonaws.com/org/opentripplanner/otp/0.19.0/otp-0.19.0-shaded.jar)
You may also want to get your own copy of the OTP source code and [build a bleeding edge development JAR from scratch](Getting-OTP), especially if you plan to do some development yourself.

## Get some data

First you'll need [GTFS data](https://developers.google.com/transit/gtfs/) to build a transit network.
Transport agencies throughout the world provide GTFS
schedules to the public. [GTFS data exchange](http://www.gtfs-data-exchange.com/) is an archive of feeds, Google
maintains a [list of some public feeds](https://code.google.com/p/googletransitdatafeed/wiki/PublicFeeds) and
[this site](http://transitfeeds.com/) also provides an extensive catalog. You'll usually want to fetch the data
directly from transit operators or agencies to be sure you have the most up-to-date version. If you know of a feed you
want to work with, download it and put it in an empty directory you have created for your OTP instance
such as `/home/username/otp` on Linux, `/Users/username/otp` on OSX, or `C:\Users\username\otp` on Windows. The file's
name must end in `.zip` for OTP to detect it. If you don't have a particular feed in mind, the one for Portland, Oregon's
[TriMet agency](http://developer.trimet.org/schedule/gtfs.zip) is a good option.
This is a moderate-sized input of good quality (Portland's TriMet agency initiated OTP development and helped develop the GTFS format).

    $ cd /home/username
    $ mkdir otp
    $ cd otp
    $ wget "http://developer.trimet.org/schedule/gtfs.zip" -O trimet.gtfs.zip

You'll also need OpenStreetMap data to build a road network for walking, cycling, and driving. OpenStreetMap is a
global database that rivals or surpasses the quality of commercial maps in many locations.
Several services extract smaller geographic regions from this database. A collection of continually updated
[Metro Extracts](https://mapzen.com/metro-extracts/)
for urban areas around the world was originally compiled by Michal Migurski and now maintained by Mapzen.
[Geofabrik](http://download.geofabrik.de/) provides extracts for larger areas like countries or states, from which you
can prepare your own smaller bounding-box extracts using
[Osmosis](http://wiki.openstreetmap.org/wiki/Osmosis#Extracting_bounding_boxes)
or [osmconvert](http://wiki.openstreetmap.org/wiki/Osmconvert#Applying_Geographical_Borders).
OSM data can be delivered as XML or in the more compact binary PBF format. OpenTripPlanner can consume both,
but we always work with PBF since it's smaller and faster.

Download OSM PBF data for the same geographic region as your GTFS feed. If you are using the TriMet feed,
the [metro extract for Portland](https://s3.amazonaws.com/metro-extracts.mapzen.com/portland_oregon.osm.pbf)
will do the job. Place this PBF file in the same directory you created for the OSM data.

    $ cd /home/username/otp
    $ wget https://s3.amazonaws.com/metro-extracts.mapzen.com/portland_oregon.osm.pbf


## Start up OTP

As a Java program OTP must be run under a Java virtual machine (JVM), which is provided as part of the Java runtime
(JRE) or Java development kit (JDK). Run `java -version` to check that you have version 1.8 or newer of the JVM installed.
If you do not you will need to install a recent OpenJDK or Oracle Java package for your operating system.

GTFS and OSM data sets are often very large, and OTP is relatively memory-hungry. You will need at least 1GB of memory
when working with the Portland TriMet data set, and several gigabytes for larger inputs. A typical command to start OTP
looks like `java -Xmx1G -jar otp-0.19.0-shaded.jar <options>`. The `-Xmx` parameter sets
the limit on how much memory OTP is allowed to consume. If you have sufficient memory in your computer,
set this to a couple of gigabytes; when OTP doesn't have enough "breathing room" it can grind to a halt.

It's possible to analyze the GTFS, OSM and any other input data and save the resulting representation of the transit
network (what we call a ['graph'](http://en.wikipedia.org/wiki/Graph_%28mathematics%29)) to disk.
For simplicity we'll skip saving this file and start up an OTP server immediately after the graph is built. The command to do so is:

    java -Xmx2G -jar otp-0.19.0-shaded.jar --build /home/username/otp --inMemory

where `/home/username/otp` should be the directory where you put your input files. The graph build operation should
take about one minute to complete, and then you'll see a `Grizzly server running` message. At this point you can open
[http://localhost:8080/](http://localhost:8080/) in a web browser. Remember to use the `--analyst` flag to start the program if you wish to use the Analyst extension. You should be presented with a web client that will
interact with your local OpenTripPlanner instance. You can also try out some web service URLs to explore the transit data:

- [A list of all routers](http://localhost:8080/otp/routers/default/)

- [List all GTFS routes on the default router](http://localhost:8080/otp/routers/default/index/routes)

- [All stops on TriMet route 52](http://localhost:8080/otp/routers/default/index/routes/TriMet:52/stops)

- [All routes passing though TriMet stop 7003](http://localhost:8080/otp/routers/default/index/stops/TriMet:7003/routes)

- [All unique sequences of stops on the TriMet Green rail line](http://localhost:8080/otp/routers/default/index/routes/TriMet:4/patterns)

## Advanced usage

Try the `--help` option for a full list of command line parameters. See the [configuration](Configuration) page for more advanced topics.


