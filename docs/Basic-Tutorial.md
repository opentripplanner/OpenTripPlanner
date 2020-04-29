# OpenTripPlanner 2 Basic Tutorial

This page should allow you to set up and test your own OTP2 server. If all goes well it should only take a few minutes!
**Note that this covers OTP2 which is just beginning beta testing, not the existing OTP 1.x release versions.**

## Get Java

OTP2 has recently been updated to be compatible with Java 11 or later. We recommend running on Java 11, which is a long-term support release, though OTP might build and run on older JDKs and JREs. Java 11 has tighter security restrictions than previous versions, so when running OTP under Java 11 you will see warnings like this:
```
WARNING: An illegal reflective access operation has occurred
WARNING: Please consider reporting this to the maintainers of com.esotericsoftware.kryo.util.UnsafeUtil
```
This warning will probably be around for a while. Migration to Java's new module system will take some time, as we need to wait for all libraries OTP2 uses to migrate before we can do so fully. 

## Get OTP

OpenTripPlanner is written in Java and distributed as a single runnable JAR file. Once OTP2 is released, 
it will be available from the Maven Central repository. You will be able to go to [the OTP directory at Maven Central](https://repo1.maven.org/maven2/org/opentripplanner/otp/), navigate to the directory for the highest version number, and download the file whose name ends with `.shaded.jar`.

Leading up to the release, we will occasionally deploy some snapshot builds for use in beta testing. These will be in the 2.0 directory of [the snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/org/opentripplanner/otp/2.0.0-SNAPSHOT/). The latest testing build should be the file with the latest timestamp ending in `.shaded.jar`.

Here are direct links to recent builds:

 - [OTP2 Beta 1 JAR file (~90MB)](https://oss.sonatype.org/content/repositories/snapshots/org/opentripplanner/otp/2.0.0-SNAPSHOT/otp-2.0.0-20191023.121912-2-shaded.jar) built from [commit bf3d4e66357ff9e596c38a2074b02cba98e5fd38 on dev-2.x](https://github.com/opentripplanner/OpenTripPlanner/tree/66a5e6dd6c6312b52b9ccf641241b07647bfff60).

You may also want to get your own copy of the OTP source code and [build a bleeding edge development JAR from scratch](Getting-OTP), especially if you plan to do some development yourself. If you're beta testing OTP2, be sure to check out the `dev-2.x`branch before running the build.

## Get some data

### GTFS for Transit Schedules and Stops

First you'll need GTFS data to build a transit network. There's an excellent description of the GTFS format [here](http://gtfs.org/). Transport agencies throughout the world provide GTFS schedules to the public. Transitland has a
[registry of feeds](https://transit.land/feed-registry) and [TransitFeeds](http://transitfeeds.com/) also provides an extensive catalog. The best option is often to simply fetch the data directly from a transit operator or agency. If you know of a feed you want to work with, download it and put it in an empty directory you have created for your OTP instance such as `/home/username/otp` on Linux, `/Users/username/otp` on OSX, or `C:\Users\username\otp` on Windows. The GTFS file's name must end in `.zip` for OTP to detect it. We often use the convention of ending GTFS file names with `.gtfs.zip` since technically a GTFS feed is just a ZIP file containing a specific set of files. If you don't have a particular feed in mind, the one for Portland, Oregon's TriMet agency is a good option. It is available at [this URL](http://developer.trimet.org/schedule/gtfs.zip). This is a moderate-sized input of good quality (TriMet initiated OTP development and helped develop the GTFS format). On Linux, this could be done on the command line as follows:

    $ cd /home/username
    $ mkdir otp
    $ cd otp
    $ wget "http://developer.trimet.org/schedule/gtfs.zip" -O trimet.gtfs.zip

### OSM for Streets

You'll also need OpenStreetMap data to build a road network for walking, cycling, and driving. [OpenStreetMap](https://www.openstreetmap.org/) is a global collaborative map database that rivals or surpasses the quality of commercial maps in many locations. Several services extract smaller geographic regions from this database. Interline Technologies maintains a collection of [extracts updated daily for urban areas around the world](https://www.interline.io/osm/extracts/). [Geofabrik](http://download.geofabrik.de/) provides extracts for larger areas like countries or states, from which you can prepare your own smaller bounding-box extracts using [Osmosis](http://wiki.openstreetmap.org/wiki/Osmosis#Extracting_bounding_boxes), [osmconvert](http://wiki.openstreetmap.org/wiki/Osmconvert#Applying_Geographical_Borders), or my current favorite [Osmium-Tool](https://osmcode.org/osmium-tool/manual.html#creating-geographic-extracts). OSM data can be delivered as XML or in the more compact binary PBF format. OpenTripPlanner consumes PBF since it's smaller and more efficient.

Download OSM PBF data for the same geographic region as your GTFS feed, and place this PBF file in the same directory you created for the OSM data. If you are using the TriMet GTFS feed, you could download the [Geofabrik extract for the US state of Oregon](http://download.geofabrik.de/north-america/us/oregon.html), then further trim that to just the [TriMet service area](https://trimet.org/pdfs/taxinfo/trimetdistrictboundary.pdf) using the bounding box switch of one of the above tools. On Linux or MacOS you could do that as follows:

    $ cd /home/username
    $ wget http://download.geofabrik.de/north-america/us/oregon-latest.osm.pbf
    $ osmconvert oregon-latest.osm.pbf -b=-123.043,45.246,-122.276,45.652 --complete-ways -o=portland.pbf
    $ mv portland.pbf otp

I find [this tool](https://boundingbox.klokantech.com/) useful for determining the geographic coordinates of bounding boxes. The CSV option in that tool produces exactly the format expected by the `osmconvert -b` switch. The `--complete-ways` switch is important to handle roads that cross outside your bounding box.

If you have extracted a smaller PBF file from a larger region, be sure to put only your extract (not the original larger file) in the directory with your GTFS data. Otherwise OTP will try to load both the original file and the extract in a later step. See the [page on preparing OSM data](Preparing-OSM) for additional information and example commands for cropping and filtering OSM data.

## Start up OTP
As a Java program, OTP must be run within a Java virtual machine (JVM), which is provided as part of the Java runtime (JRE) or Java development kit (JDK). Run `java -version` to check that you have version 11 or newer of the JVM installed. If you do not, you will need to install a recent OpenJDK or Oracle Java package for your operating system.

GTFS and OSM data sets are often very large, and OTP is relatively memory-hungry. You will need at 
least 1GB of memory when working with the Portland TriMet data set, and several gigabytes for larger
 inputs. A typical command to start OTP looks like `java -Xmx1G -jar otp.shaded.jar <options>`. The 
 `-Xmx` parameter sets the limit on how much memory OTP is allowed to consume. If you have
 sufficient memory in your computer, set this to a couple of gigabytes (e.g. `-Xmx2G`); when OTP 
 doesn't have enough memory "breathing room" it can grind to a halt. 
 [VisualVM](https://visualvm.github.io) is a good way to inspect Java memory usage, especially with
 the [VisualGC plugin](https://visualvm.github.io/plugins.html).

### OTP Main Phases
OTP has three phases that can be run in sequence or in isolation. It's possible to analyze the 
GTFS, OSM and any other input data and save the resulting representation of the transit network 
(what we call a ['graph'](http://en.wikipedia.org/wiki/Graph_%28mathematics%29)) to disk. Depending 
on the data it might take a while to build a graph. Normally, building the street graph, especially 
with elevation data, takes a long time, so it can be convenient to build the street graph once, and
then use that as a starting point into which we add transit data to make the final graph. The final 
graph can then be saved for later or served by the same OTP instance. When an OTP server is 
restarted it can reload a pre-built graph, which is significantly faster than building it from 
scratch. 

These are the three main phases:

1. Building a street-graph _streetGraph.obj_. 
2. Building adding transit data to produce _graph.obj_.
3. Serving the graph.

Steps 1 and 2 can be combined for smaller or simpler street networks. When starting OTP the command line parameter is used to control witch phases are run. The diagram below shows the flow depending on the parameters used.

![Command-Line-Parameter-Flow](images/cli-flow.svg)

You must use at least one of the required 
parameters: `--load`, `--loadStreet`, `--build`, `--buildStreet`. Some of the parameters are 
implied by a _required_ parameter. For example `--serve` has no effect when used together
with `--load` because `--load` implies `--serve`.    


### Building a graph and serving it 

The simplest way to start OTP is to build everything and start serving the graph without saving it. The command to do
so is:

`$ java -Xmx2G -jar otp.shaded.jar --build --serve /home/username/otp`

where `/home/username/otp` should be the directory where you put your configuration and input files.
 
The graph build operation should take about one minute to complete, and then you'll see a 
`Grizzly server running` message. At this point you have an OpenTripPlanner server running locally 
and can open [http://localhost:8080/](http://localhost:8080/) in a web browser. You should be 
presented with a web client that will interact with your local OpenTripPlanner instance.

This map-based user interface is in fact sending HTTP GET requests to the OTP server running on your
local machine. It can be informative to watch the HTTP requests and responses being generated using
the developer tools in your web browser. OTP's built-in web server will run by default on ports
8080 and 8081. If by any chance some other software is already using those port numbers, you can
specify different port numbers with switches like `--port 8801 --securePort 8802`.


### Saving a graph

If you want to save the graph to speed up the process of repeatedly starting up a server with the
same graph, building the graph and starting the server need to be done in two or three stages. In 
this example we start the OTP instance in the current directory(`.`)  where the data and config 
files are saved:

- To build a graph from street and transit data, then save it to a file, use the `--build` and `--save` parameters:

    `$ java -Xmx2G -jar otp.shaded.jar --build --save .`

- To build a street-graph (OSM and elevation data only, ignoring transit input files):

    `$ java -Xmx2G -jar otp.shaded.jar --buildStreet --save .`

- To build a graph layering transit data on top of an existing street graph (built using the previous command):

    `$ java -Xmx2G -jar otp.shaded.jar --loadStreet --build --save .`
