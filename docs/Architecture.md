## Basic OTP Architecture

At the core of OpenTripPlanner is a library of Java code that finds efficient paths through multi-modal transportation networks built from [OpenStreetMap](http://wiki.openstreetmap.org/wiki/Main_Page) and [GTFS](https://developers.google.com/transit/gtfs/) data. Several different services are built upon this library:

The **OTP Routing API** is a [RESTful](https://en.wikipedia.org/wiki/Representational_state_transfer) web service that responds to journey planning requests with itineraries in a JSON or XML representation. You can combine this API with OTP's standard Javascript front end to provide users with trip planning functionality in a familiar map interface, or write your own applications that talk directly to the API.

The **OTP Transit Index API** is another [RESTful](https://en.wikipedia.org/wiki/Representational_state_transfer) web service that provides information derived from the input GTFS feed(s). Examples include routes serving a particular stop, upcoming vehicles at a particular stop, upcoming stops on a given trip, etc. More complex transit data requests can be formulated using a GraphQL API.

The term "OTP Analyst" refers to parts of OTP that apply the routing engine to transportation network analysis rather than end-to-end trip planning. OTP Analyst includes:

The **OTP Analyst Web Services** provide network analysis results such as travel time maps and isochrones as standard web Mercator tiles or GIS rasters via a [WMS](http://en.wikipedia.org/wiki/Web_Map_Service)-derived API. These web services are conceptually separate from the routing API, but are provided by the same servlet: once you have a working OTP trip planner you can also use it to produce travel time maps and other visualizations of transit service. See [this blog post](http://conveyal.com/blog/2012/07/02/analyst) for discussion and examples.

The **OTP Analyst Batch Processor** is a command-line tool that handles more complex one-off network analysis tasks. It uses the same core routing library and data sources as other OTP services, but allows for very open-ended configuration and the inclusion of population or opportunity data. While configuration and use are currently somewhat opaque for non-developers, the "Batch Analyst" is becoming a powerful tool for visualizing how transportation networks affect access to urban opportunities. See [this article](http://www.theatlanticcities.com/commute/2013/01/best-maps-weve-seen-sandys-transit-outage-new-york/4488/) for an example case study on the effects of hurricane Sandy in New York.

The **OTP Scripting API** allow the execution of routing requests from within scripts (such as _Python_). It is composed of a stable internal API, and an embedded Jython interpreter. It can be used in different contexts, such as batch analysis or automated regression testing. [More information here](Scripting).
