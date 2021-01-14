# OTP Interfaces (APIs) and Data Sources


## Input Formats

At the core of OpenTripPlanner is a library of Java code that finds efficient paths through multi-modal transportation networks built from [OpenStreetMap](http://wiki.openstreetmap.org/wiki/Main_Page) and [GTFS](https://developers.google.com/transit/gtfs/) data. It can also receive GTFS-RT (realtime) data...

In addition to GTFS, OTP2 can also load data in the Nordic Profile of Netex, the EU-standard transit data interchange format. The upcoming EU-wide profile was heavily influenced by the Nordic Profile and uses the same schema, so eventual support for the full the EU profile is a possibility.

GTFS and Netex data are converted into OTP's own internal model which is a superset of both. It is therefore possible to mix Netex and GTFS data, and potentially even data from other sources.

## Interfaces to Services (APIs)

Several different services are built upon this routing library, and expose APIs:

The **OTP Routing API** is a [RESTful](https://en.wikipedia.org/wiki/Representational_state_transfer) web service that responds to journey planning requests with itineraries in a JSON or XML representation. You can combine this API with OTP's standard Javascript front end to provide users with trip planning functionality in a familiar map interface, or write your own applications that talk directly to the API.

The **OTP Transit Index API** is another [RESTful](https://en.wikipedia.org/wiki/Representational_state_transfer) web service that provides information derived from the input GTFS feed(s). Examples include routes serving a particular stop, upcoming vehicles at a particular stop, upcoming stops on a given trip, etc. More complex transit data requests can be formulated using a GraphQL API.

## Sandbox APIs

Additional experimental APIs are provided by [sandbox extensions](SandboxExtension.md):

The [Health API](sandbox/HealthAPI.md) provides endpoints for checking the health status of the OTP instance. It can be useful when running OTP in a container.

The [Transmodel GraphQL API](sandbox/TransmodelApi.md) is the Transmodel API (version 3) used at Entur in production(Sep, 2020).

The [HSL Legacy GraphQL API](sandbox/LegacyGraphQLApi.md) is the HSL's GraphQL API used by the Digitransit project.
