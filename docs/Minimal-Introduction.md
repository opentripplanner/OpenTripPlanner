# Basic Usage of OpenTripPlanner

This page will get you up and running with your own OTP server. If all goes well it should only take a few minutes!

## Get OTP

OpenTripPlanner is written in Java and distributed as a single runnable JAR file. These JARs are published
[here](http://dev.opentripplanner.org/jars/). Grab one of these JARs for the
[latest released version](http://dev.opentripplanner.org/jars/otp-0.13.0.jar)
or if you're feeling adventurous try the
[bleeding edge development code](http://dev.opentripplanner.org/jars/otp-1.0.0-SNAPSHOT.jar).
You may also want to get your own copy of the OTP source code and [build the JAR from scratch](Building-From-Source),
especially if you plan to do some development yourself.

## Get some data

First you'll need [GTFS data](https://developers.google.com/transit/gtfs/) to build a transit network.
Transport agencies throughout the world provide GTFS
schedules to the public. [GTFS data exchange](http://www.gtfs-data-exchange.com/) is an archive of feeds, Google
maintains a [list of some public feeds](https://code.google.com/p/googletransitdatafeed/wiki/PublicFeeds) and
[this site](http://transitfeeds.com/) also provides an extensive catalog. You'll usually want to fetch the data
directly from transit operators or agencies to be sure you have the most up-to-date version. If you know of a feed you
want to work with, download it and put it in an empty directory you have created for your OTP instance
such as `/home/username/otp` on Linux, `/Users/username/otp` on OSX, or `C:\Users\username\otp` on Windows. If you
don't have a particular feed in mind, the one for Portland, Oregon's
[TriMet agency](http://developer.trimet.org/schedule/gtfs.zip) is a good option.
It is a moderate-sized system and Portland's TriMet agency initiated OTP development and helped develop the GTFS format.

You'll also need OpenStreetMap data to build a road network for walking, cycling, and driving. This is necessary to
accurately describe how to reach transit stops. OpenStreetMap is a massive database covering the entire planet, and in
many places rivals or surpasses the quality of commercial maps. Several services extract smaller geographic
regions from this database. Mapzen provides [Metro Extracts](https://mapzen.com/metro-extracts/) for many urban areas
around the world, and [Geofabrik](http://download.geofabrik.de/) provides extracts for larger areas like countries or
states, from which you can prepare your own smaller bounding-box extracts using
[Osmosis](http://wiki.openstreetmap.org/wiki/Osmosis#Extracting_bounding_boxes)
or [osmconvert](http://wiki.openstreetmap.org/wiki/Osmconvert#Applying_Geographical_Borders).
OSM data can be delivered in "traditional" XML or the more compact binary PBF format. OpenTripPlanner can consume both,
but we usually work with PBF since it's smaller and faster.

Download OSM PBF data for the same geographic region as your GTFS feed. If you are using the TriMet feed,
the [metro extract for Portland](https://s3.amazonaws.com/metro-extracts.mapzen.com/portland_oregon.osm.pbf)
will do the job. Place this PBF file in the same directory you created for the OSM data.

## Start up OTP

As a Java program OTP must be run under a Java virtual machine (JVM), which is provided as part of the Java runtime
(JRE) or Java development kit (JDK). Run `java -version` to check that you have version 1.7 or newer of the JVM installed.
If you do not you will need to install a recent OpenJDK or Oracle Java package for your operating system.

GTFS and OSM data sets are often very large, and OTP is relatively memory-hungry. You will need at least 1GB of memory
when working with the Portland TriMet data set, and several gigabytes for larger inputs. A typical command to start OTP
looks like `java -Xmx1G -jar otp-0.13.0.jar <options>`. The `-Xmx` parameter sets
the limit on how much memory OTP is allowed to consume. If you have sufficient memory in your computer,
set this to a couple of gigabytes; when OTP doesn't have enough "breathing room" it can grind to a halt.

It's possible to analyze the GTFS, OSM and any other input data and save the resulting representation of the transit
network (what we call a ['graph'](http://en.wikipedia.org/wiki/Graph_%28mathematics%29)) to disk.
For simplicity we'll skip saving this file and start up an OTP server immediately. The command to do so is:

`java -Xmx2G -jar otp-0.13.0.jar --build /home/username/otp --inMemory`

where `/home/username/otp` should be the directory where you put your input files. The graph build operation should
take about one minute to complete, and then you'll see a `Grizzly server running` message. At this point you can open
[http://localhost:8080/](http://localhost:8080/) in a web browser. You should be presented with a web client that will
interact with your local OpenTripPlanner instance.

You can also try out some web service URLs to explore the transit data:

- `http://localhost:8080/otp/routers/default/`

- `http://localhost:8080/otp/routers/default/index/routes`

