# Minimal Introduction to OTP

Here's how to get an instance of OTP up and running quickly. In this tutorial you'll be using the "master" branch of OTP, the one on which most active development occurs.

Some things you'll need:
* A Linux or Mac machine with over 1GB memory. OTP should work on any platform with Java, but the filenames and commands in this page use UNIX-like conventions.
* A number of supporting software packages, including:
 * git (a version control system)
 * maven (a build system, install version 3 which is now common)

#### Get OTP

    $ cd /path/to/projects
    $ git clone git://github.com/opentripplanner/OpenTripPlanner.git
For normal usage latest stable version is recommended. Curently this is version 0.11
To get it you need to switch to [0.11.x branch](https://github.com/opentripplanner/OpenTripPlanner/tree/0.11.x):

    $ git checkout 0.11.x

If you want to try new unstable version or you want to help with developing you don't need to do anything, because master is development branch.
More information about different versions in [[Version-Notes]].

#### Build OTP

    $ cd OpenTripPlanner
    $ mvn clean package

This stage takes a while. If it completes with a message like `BUILD FAILED`, then the rest of this tutorial won't work.

#### Build a graph

A graph is a file that combines and links transportation information from a number of sources into a form that's easy for OTP use. Basic graphs use OpenStreetMap road data, and public transport data in GTFS format.

First, download a GTFS from your favorite city. Here's the GTFS for Portland's Trimet system.

    $ cd /path/to/downloads
    $ mkdir pdx
    $ cd pdx
    $ wget "http://developer.trimet.org/schedule/gtfs.zip" -O trimet.gtfs.zip

Then, get a subset of OpenStreetMap data corresponding to the same area. There are many ways to get OSM data. One convenient way is a collection of "metro extracts" originally compiled by Michal Migurski and now maintained by Mapzen at [[https://mapzen.com/metro-extracts/]]. You'll want to get map data in the OSM PBF format, which is much more compact and faster to load than the older XML format.

    $ wget https://s3.amazonaws.com/metro-extracts.mapzen.com/portland.osm.pbf

Current way to build the graph
  
    $ cd /path/to/projects/OpenTripPlanner
    $ java -Xmx2G -jar target/otp.jar --build /path/to/downloads/pdx


In version 0.11.x `otp.jar` is located in `otp-core/target/otp.jar `

If you have old GraphBuilder.xml from previous versions or some specific settings that can't be represented yet in otp.jar parameters you can still use old [[GraphBuilder]] like:

```shell
mvn package -DskipTests
./build-old /path/to/my/graph-config.xml
```

#### Run the server

Make a `/var/otp/graphs` directory if necessary, and copy the graph there

    $ sudo mkdir /var/otp/graphs
    $ mv /path/to/downloads/pdx/Graph.obj /var/otp/graphs

Then head over to the OTP directory and run the server:

    $ cd /path/to/projects/OpenTripPlanner
    $ java -Xmx2G -jar target/otp.jar --server

This will take a minute. Once you see `Grizzly server running.` check out [http://localhost:8080/](http://localhost:8080/)

**NOTE** Due to a known bug in our web server library, you will need to specify the document name explicitly [http://localhost:8080/index.html](http://localhost:8080/index.html) until we can switch to a newer version.

Once the server starts up, you can also try some web service URLs to verify that it's working:
- `http://localhost:8080/otp/routers/default/` 
- `http://localhost:8080/otp/routers/default/index/routes`

You could also do: 

`java -jar target/otp.jar -p 9090 -r mexico --server`

in order to run on port 9090 and load the graph for routerId 'mexico'. You can also specify the base directory for graphs with -g. As we continue to work on standalone mode, it should continue to function in the same way but just be enriched with more command line options. Try the `--help` option for a full list of command line parameters.

#### Next Steps

We need pages on advanced graph building topics (carshare, bikeshare, elevation &c) in standalone OTP.

### ELEVATION DATA (TBC)

If you want to add elevation data (for the U.S. only -- see [http://ned.usgs.gov](http://ned.usgs.gov) for more) to your graph, we'll again edit graph-builder.xml

NOTE: NED downloads take a real long time, and the graph building is really slow...

NOTE: Those outside the US can also potentially use another elevation data set...see  [[GraphBuilder#Elevationdata]] for (not much) more information.
