# NeTEx & SIRI tutorial

One important new feature of OTP2 is the ability to
load [NeTEx](https://en.wikipedia.org/wiki/NeTEx) and [SIRI](https://en.wikipedia.org/wiki/Service_Interface_for_Real_Time_Information) 
data. NeTEx is a European [specification for transit data exchange](http://netex-cen.eu), comparable in purpose to
GTFS but broader in scope. 

First of all, you need to download a [bundled jar of OTP](Getting-OTP.md).

Secondly, you will use the [Norwegian NeTEx file](https://developer.entur.org/pages-intro-files) as
well as the [Norwegian OSM data](http://download.geofabrik.de/europe/norway.html), but OTP can download the NeTEx one for you.

## Configuring the build

Create a working directory and place the OTP jar file in it and call it `otp.jar.`

Since we download the OSM data from a free source, we don't want to put undue stress on the server.
Therefore we download it before building the graph, not during.

```
curl https://download.geofabrik.de/europe/norway-latest.osm.pbf -o norway.osm.pbf
```

Now create a file called `build-config.json` in the same folder and fill it with the following
content:

<!-- INSERT: build-config -->

Note the special section specifying how to find NeTEx XML files within the single ZIP archive that
OTP downloads.

Now you can instruct OTP to build a graph from this configuration file:

`java -Xmx16G -jar otp.jar --build --save .`

This should produce a file `graph.obj` in the same directory as your `build-config.json`.

Building the Norway graph requires downloading about 250MB of input data so stay patient at the beginning
particularly on a slow internet connection.
The actual build takes approximately 10 minutes (without elevation data, as is configured above), 
and can be done within 16GB of heap memory (JVM switch `-Xmx16G`). The Graph file it produces is 
about 1.1 GB. The server will take about 30 seconds to load this graph and start up, and will 
consume about 6GB of heap memory under light use.

You can then start up an OTP server with a command like this:

`java -Xmx6G -jar otp.jar --load .`

Once the server is started up, go to `http://localhost:8080` in a browser to try out your server
using OTP's built in testing web client. Try some long trips like Oslo to Bergen and see if you can
get long distance trains and flights as alternatives. You might need to increase the walking limit
above its very low default value.

## Adding SIRI real time Data

Another important feature in OTP version 2 is the ability to
use [SIRI real-time data](https://en.wikipedia.org/wiki/Service_Interface_for_Real_Time_Information).
Within the EU data standards, SIRI is analogous to GTFS-RT: a way to apply real-time updates on top
of schedule data. While technically a distinct specification from NeTEx, both NeTEx and SIRI use the
Transmodel vocabulary, allowing SIRI messages to reference entities in NeTEx schedule data. Like
GTFS-RT, SIRI is consumed by OTP2 using "graph updaters" which are configured in
the `router-config.json` file, which is placed in the same directory as the `graph.obj` file and
loaded at server startup.

<!-- INSERT: router-config -->

After saving the file in the working directory, restart OTP.

The updaters fetch two different kinds of SIRI data:

- Situation Exchange (SX, text notices analogous to GTFS-RT Alerts)
- Estimated Timetable (ET, predicted arrival times analogous to GTFS-RT TripUpdates)

These updaters can handle differential updates, but they use a polling approach rather than the
message-oriented streaming approach of the GTFS-RT Websocket updater. The server keeps track of
clients, sending only the things that have changed since the last polling operation.

Note that between these SIRI updaters and the GTFS-RT Websocket updater, we now have both polling
and streaming examples of GTFS-RT "incrementality" semantics, so should be able to finalize that
part of the specification.