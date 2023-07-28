## Routing modes

The routing request parameter `modes` determines which transport modalities should be considered when
calculating the list of routes.

Some modes (mostly bicycle and car) also have optional qualifiers `RENT` and `PARK` to specify if
vehicles are to be parked at a station or rented. In theory this can also apply to other modes but
makes sense only in select cases which are listed below.

Whether a transport mode is available highly depends on the input feeds (GTFS, OSM, bike sharing
feeds) and the graph building options supplied to OTP.

<!-- INSERT: street-modes -->

<!-- INSERT: transit-modes -->

### Note
Note that there are conceptual overlaps between `TRAM`, `SUBWAY` and `RAIL` and some transport <br/>
providers categorize their routes differently to others. In other words, what is considered <br/>
a `SUBWAY` in one city might be of type `RAIL` in another. <br/> Similarly the `TROLLEYBUS` mode is categorized by some operators as `BUS`. Study your input GTFS feed carefully to <br/>
find out the appropriate mapping in your region. 