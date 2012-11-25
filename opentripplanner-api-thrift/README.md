--== OVERVIEW ==--

This is the subproject for a Thrift API to OpenTripPlanner. The Thrift API
was designed and built to support somewhat lower-level queries than the 
already-exposed REST JSON/XML API. For example, it supports the calculation
of the expected duration of a trip from A to B without actually calculating 
the directions narrative between those points, thereby making the calculation
much faster (< 10 ms instead of ~200 ms). 

--== INSTALL ==--

General installation instructions are available on the website:
http://opentripplanner.org/wiki/Install

You will also need to install the Apache Thrift runtime on your machine. Instructions are here:
http://thrift.apache.org/download/

