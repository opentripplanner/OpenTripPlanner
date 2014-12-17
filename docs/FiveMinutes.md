# Deep-Dive into OTP

*Note that this page describes configuration and usage of older versions of OTP through 0.10.x. It does not apply to the master branch, where graphs can be built and a server started entirely from the standalone JAR file, as described in the [minimal introduction page](Minimal-Introduction).*

The intent of this lesson is for you to build your own Graph.obj, first using the TriMet data...then using a transit agency GTFS file of your choosing.  Going past the initial lesson, we'll dive into adding elevation data to your graph (which is needed for the nifty street slope chart seen with biking directions).

***

### PORTLAND GRAPH

 1. Download [http://maps.trimet.org/otp-dev/otp.zip](http://maps.trimet.org/otp-dev/otp.zip) (otp.zip is a 240mb file ... do a mouse right-click and save-as ... or better yet, use a cmd-line client like curl or wget to download). NOTE: otp.zip was last packaged on July 18, 2013 -- it is fairly old code from November 2012.

 1. Unzip the contents to the / directory (or C:\ for Windows users).

    NOTE: it's very important to have the pre-built web app run from /otp (or C:\otp).  If you want to run from another location, you'll need to edit the file WEB-INF/classes/data-sources.xml within /otp/webapps/opentripplanner-api-webapp.war.

 1. Download OSM street data for Portland [http://maps.trimet.org/otp-dev/or-wa.osm] (http://maps.trimet.org/otp-dev/or-wa.osm) IMPORTANT: this file is ~370 megs ... do a mouse right-click, then 'Save link as...', otherwise this text file is so big, you'll crash your browser trying to load it in a browser window.

 1. Move or-wa.osm to the /otp/cache/osm/ directory

 1. open a command prompt (cmd.exe on Windows)

 1. cd to /otp

 1. bin/build-graph.sh (or bin\build-graph.bat for Windows users)

    NOTE: this takes a while...but you should end up with a new Graph.obj file.  

 1. run bin/graph-viz.sh (or bin\graph-viz.bat for Windows users) to view your new Graph.obj 

 1. bin/start-server.sh  (or bin\start-server.bat for Windows users), then open  [http://localhost:8080/opentripplanner-webapp](http://localhost:8080/opentripplanner-webapp) in a web browser to see OTP route around your new graph...

 1. See [[GraphStructure]] and [[GraphBuilder]] for more information on content and configuration of Graph.obj

***

### CUSTOM GRAPH

 1. First, find a URL to a GTFS feed that you're interested in (see [http://www.gtfs-data-exchange.com/](http://www.gtfs-data-exchange.com/) for ideas).

 1. cd /otp

 1. open graph-builder.xml in a text editor -- we're going to make two edits

 1. edit !#1 - change the GTFS url on/about line 17, from [http://developer1.trimet.org/schedule/gtfs.zip](http://developer1.trimet.org/schedule/gtfs.zip) to your desired gtfs.zip url path.  Further, either edit 'TriMet' as the defaultAgencyId, or simply remove this element.

 1. edit !#2 - specify the OSM data for your region (not the or-wa.osm data).  You have two options for acquiring OSM data for your region: 

    a) [RECOMMENDED] obtain a .osm file for your region, then edit the path to that file on line 66 of graph-builder.xml.  NOTE that your .osm file can have a much larger extent than your transit data ... preferable in fact. BTW, here's the url I use to retrieve the OSM data for or-wa.osm [http://open.mapquestapi.com/xapi/api/0.6/map?bbox=-123.4,44.8,-121.5,45.8](http://open.mapquestapi.com/xapi/api/0.6/map?bbox=-123.4,44.8,-121.5,45.8) << **DANGER** clicking this xapi link will result in a big file that will probably crash your browser...again, right-click to do a save-as of this file>>.  

    b) let OTP download the .osm data for your region (based on the stop locations within your gtfs.zip file) -- to make that change, you need to un-comment the RegionBasedOpenStreetMapProviderImpl provider on line 45 of graph-builder.xml, and then comment out the StreamedFileBasedOpenStreetMapProviderImpl provider on line 65 of graph-builder.xml.  

    <p><a href="http://wiki.openstreetmap.org/wiki/Xapi#Servers" target="_blank">Xapi</a> courtesy of <a href="http://www.mapquest.com/" target="_blank">MapQuest</a> <img src="http://developer.mapquest.com/content/osm/mq_logo.png"></p>

 1. cd to /otp

 1. bin/build-graph.sh (or bin\build-graph.bat for Windows users)

    NOTE: this takes a while...but you should end up with a new Graph.obj file.  If you don't see any log messages for more than about ten minutes, try increasing the amount of memory used with -Xmx, or not using MapBuilder.

 1. run bin/graph-viz.sh (or bin\graph-viz.bat for Windows users) to view your new Graph.obj 

 1. bin/start-server.sh (or bin\start-sever.bat for Windows users), then open  [http://localhost:8080/opentripplanner-webapp](http://localhost:8080/opentripplanner-webapp) in a web browser to see OTP route around your new graph...

 1. See [[GraphStructure]] and [[GraphBuilder]] for more information on content and configuration of Graph.obj

***

### ELEVATION DATA


 1. If you want to add elevation data (for the U.S. only -- see [http://ned.usgs.gov](http://ned.usgs.gov) for more) to your graph, we'll again edit graph-builder.xml

 1. open graph-builder.xml in a text editor

 1. uncomment the 'nedBuilder' ref on line 118.

 1. cd to /otp

 1. bin/build-graph.sh

    NOTE: NED downloads take a real long time, and the graph building is really slow...

    NOTE: Those outside the US can also potentially use another elevation data set...see  [[GraphBuilder#Elevationdata]] for (not much) more information.

 1. etc...  see instructions above for running your new graph though otp ...





Good luck...