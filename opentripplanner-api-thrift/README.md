opentripplanner-api-thrift
============================

Overview
============================

This is the subproject for a Thrift API to OpenTripPlanner. The Thrift API
was designed and built to support lower-level queries than the 
already-exposed REST JSON/XML API. For example, it supports the calculation
of the expected duration of a trip from A to B without actually calculating 
the directions narrative between those points, thereby making the calculation
much faster (< 10 ms instead of ~200 ms). 

Installation
============================

General installation instructions are available on the website:
http://opentripplanner.org/wiki/Install

You will also need to install the Apache Thrift runtime on your machine. Instructions are here:
http://thrift.apache.org/download/

Running the Server
============================
There is a sample Spring configuration located in
src/main/resources/org/opentripplanner/api_thrift/example-service-config.xml

You will need to edit this file to point to the location of your 
graph (server only supports a single graph for now) and to select a
non-default port to run on. Build the project using Maven in the
usual way and then run 
org.opentripplanner.api.thrift.OTPServerMain
passing in the location of your edited configuration file as the
only command line argument.
