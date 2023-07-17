# OTP APIs and Data Sources

## APIs

Several different services are built upon this routing library, and expose APIs:

### GraphQL

The [GTFS GraphQL API](sandbox/GtfsGraphQlApi.md) has been used by the Digitransit and otp-react-redux 
projects in production for many years.

The [Transmodel GraphQL API](sandbox/TransmodelApi.md) is the Transmodel API (version 3) used at
Entur in production since 2020.

The [Actuator API](sandbox/ActuatorAPI.md) provides endpoints for checking the health status of the
OTP instance. It can be useful when running OTP in a container.

### Legacy APIs (to be removed)

The OTP REST API used to power many apps and frontends. For years it was the only way to access
OTP programmatically.

Over time it has been replaced by the GraphQL APIs and is scheduled to be disabled by default
and eventually removed completely. It's therefore not recommended to use it.

## Input Formats

At the core of OpenTripPlanner is a library of Java code that finds efficient paths through
multi-modal transportation networks built
from [OpenStreetMap](http://wiki.openstreetmap.org/wiki/Main_Page)
and [GTFS](https://developers.google.com/transit/gtfs/) data. It can also receive GTFS-RT (realtime)
data...

In addition to GTFS, OTP can also load data in the Nordic Profile of Netex, the EU-standard transit
data interchange format. The upcoming EU-wide profile was heavily influenced by the Nordic Profile
and uses the same schema, so eventual support for the full the EU profile is a possibility.

GTFS and Netex data are converted into OTP's own internal model which is a superset of both. It is
therefore possible to mix Netex and GTFS data, and potentially even data from other sources.
