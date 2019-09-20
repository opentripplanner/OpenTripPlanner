# OpenTripPlanner Basic Tutorial

This page will get you up and running with your own OTP server. If all goes well it should only take a few minutes!

## Get OTP

OpenTripPlanner is written in Java and distributed as a single runnable JAR file. These JARs are deployed to the Maven Central repository. Go to [the OTP directory at Maven Central](https://repo1.maven.org/maven2/org/opentripplanner/otp/), navigate to the directory for the highest version number, and download the file whose name ends with `.shaded.jar`.
You may also want to get your own copy of the OTP source code and [build a bleeding edge development JAR from scratch](Getting-OTP), especially if you plan to do some development yourself.

## Get some data

### GTFS for Transit Schedules and Stops

First you'll need GTFS data to build a transit network. There's an excellent description of the GTFS format [here](http://gtfs.org/). Transport agencies throughout the world provide GTFS schedules to the public. Transitland has a
[registry of feeds](https://transit.land/feed-registry) and [TransitFeeds](http://transitfeeds.com/) also provides an extensive catalog. The best option is often to simply fetch the data directly from a transit operator or agency. If you know of a feed you want to work with, download it and put it in an empty directory you have created for your OTP instance such as `/home/username/otp` on Linux, `/Users/username/otp` on OSX, or `C:\Users\username\otp` on Windows. The GTFS file's name must end in `.zip` for OTP to detect it. We often use the convention of ending GTFS file names with `.gtfs.zip` since technically a GTFS feed is just a ZIP file containing a specific set of files. If you don't have a particular feed in mind, the one for Portland, Oregon's TriMet agency is a good option. It is available at [this URL](http://developer.trimet.org/schedule/gtfs.zip). This is a moderate-sized input of good quality (TriMet initiated OTP development and helped develop the GTFS format). On Linux, this could be done on the command line as follows:

    $ cd /home/username
    $ mkdir otp
    $ cd otp
    $ wget "http://developer.trimet.org/schedule/gtfs.zip" -O trimet.gtfs.zip

### OSM for Streets

You'll also need OpenStreetMap data to build a road network for walking, cycling, and driving. [OpenStreetMap](https://www.openstreetmap.org/) is a global collaborative map database that rivals or surpasses the quality of commercial maps in many locations. Several services extract smaller geographic regions from this database. Interline Technologies maintains a collection of [extracts updated daily for urban areas around the world](https://www.interline.io/osm/extracts/). [Geofabrik](http://download.geofabrik.de/) provides extracts for larger areas like countries or states, from which you can prepare your own smaller bounding-box extracts using [Osmosis](http://wiki.openstreetmap.org/wiki/Osmosis#Extracting_bounding_boxes) or [osmconvert](http://wiki.openstreetmap.org/wiki/Osmconvert#Applying_Geographical_Borders). OSM data can be delivered as XML or in the more compact binary PBF format. OpenTripPlanner can consume both, but we always work with PBF since it's smaller and faster.

Download OSM PBF data for the same geographic region as your GTFS feed, and place this PBF file in the same directory you created for the OSM data. If you are using the TriMet GTFS feed, you could download the [Geofabrik extract for the US state of Oregon](http://download.geofabrik.de/north-america/us/oregon.html), then further trim that to just the [TriMet service area](https://trimet.org/pdfs/taxinfo/trimetdistrictboundary.pdf). On Linux or MacOS you could do that as follows:

    $ cd /home/username
    $ wget http://download.geofabrik.de/north-america/us/oregon-latest.osm.pbf
    $ osmconvert oregon-latest.osm.pbf -b=-123.043,45.246,-122.276,45.652 --complete-ways -o=portland.pbf
    $ mv portland.pbf otp

I find [this tool](https://boundingbox.klokantech.com/) useful for determining the geographic coordinates of bounding boxes. The CSV option in that tool produces exactly the format expected by the `osmconvert -b` switch. The `--complete-ways` switch is important to handle roads that cross outside your bounding box.

If you have extracted a smaller PBF file from a larger region, be sure to put only your extract (not the original larger file) in the directory with your GTFS data. Otherwise OTP will try to load both the original file and the extract in a later step.

## Start up OTP

As a Java program, OTP must be run within a Java virtual machine (JVM), which is provided as part of the Java runtime
(JRE) or Java development kit (JDK). Run `java -version` to check that you have version 1.8 or newer of the JVM installed.
If you do not you will need to install a recent OpenJDK or Oracle Java package for your operating system.

GTFS and OSM data sets are often very large, and OTP is relatively memory-hungry. You will need at least 1GB of memory
when working with the Portland TriMet data set, and several gigabytes for larger inputs. A typical command to start OTP
looks like `java -Xmx1G -jar otp-0.19.0-shaded.jar <options>`. The `-Xmx` parameter sets
the limit on how much memory OTP is allowed to consume. If you have sufficient memory in your computer,
set this to a couple of gigabytes (e.g. `-Xmx2G`); when OTP doesn't have enough memory "breathing room" it can grind to a halt.

It's possible to analyze the GTFS, OSM and any other input data and save the resulting representation of the transit
network (what we call a ['graph'](http://en.wikipedia.org/wiki/Graph_%28mathematics%29)) to disk. Then when the OTP server is restarted it can reload this pre-built graph, which is significantly faster than building it from scratch.
For simplicity, in this introductory tutorial we'll skip saving the graph file. After the graph is built we'll immediately pass it to an OTP server in memory. The command to do so is:


```
$ java -Xmx2G -jar otp-0.19.0-shaded.jar --build /home/username/otp --inMemory
```

where `/home/username/otp` should be the directory where you put your input files.

The graph build operation should take about one minute to complete, and then you'll see a `Grizzly server running` message. At this point you have an OpenTripPlanner server running locally and can open [http://localhost:8080/](http://localhost:8080/) in a web browser. You should be presented with a web client that will
interact with your local OpenTripPlanner instance.

This map-based user interface is in fact sending HTTP GET requests to the OTP server running on your local machine. It can be informative to watch the HTTP requests and responses being generated using the developer tools in your web browser.

OTP's built-in web server will run by default on ports 8080 and 8081. If by any chance some other software is already using those port numbers, you can specify different port numbers with switches like `--port 8801 --securePort 8802`.


## Other simple requests

There are a number of different resources available through the HTTP API. Besides trip planning, OTP can also look up information about transit routes and stops from the GTFS you loaded and return this information as JSON. For example:

- Get a list of all available routers: [http://localhost:8080/otp/routers/default/](http://localhost:8080/otp/routers/default/)

- Get a list all GTFS routes on the default router: [http://localhost:8080/otp/routers/default/index/routes](http://localhost:8080/otp/routers/default/index/routes)

- Find all stops on TriMet route 52: [http://localhost:8080/otp/routers/default/index/routes/TriMet:52/stops](http://localhost:8080/otp/routers/default/index/routes/TriMet:52/stops)

- Find all routes passing though TriMet stop ID 7003: [http://localhost:8080/otp/routers/default/index/stops/TriMet:7003/routes](http://localhost:8080/otp/routers/default/index/stops/TriMet:7003/routes)


- Return all unique sequences of stops on the TriMet Green rail line: [http://localhost:8080/otp/routers/default/index/routes/TriMet:4/patterns](http://localhost:8080/otp/routers/default/index/routes/TriMet:4/patterns)

We refer to this as the Index API. It is also documented [in the OTP HTTP API docs](http://dev.opentripplanner.org/apidoc/1.4.0/resource_IndexAPI.html).
