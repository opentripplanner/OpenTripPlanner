# APIs

Several different services are built upon this routing library, and expose these APIs:

## GraphQL

The [GTFS GraphQL API](sandbox/GtfsGraphQlApi.md) has been used by the Digitransit and otp-react-redux 
projects in production for many years. If your input data is mostly GTFS then this is probably your
best choice as it uses the same vocabulary.

The [Transmodel GraphQL API](sandbox/TransmodelApi.md) is the Transmodel API (version 3) used at
Entur in production since 2020. If your input data is mostly NeTeX then you should use this.

The [Actuator API](sandbox/ActuatorAPI.md) provides endpoints for checking the health status of the
OTP instance. It can be useful when running OTP in a container.

The [Geocoder API](sandbox/GeocoderAPI.md) allows you to geocode street corners and stop names.

## Legacy APIs (to be removed)

The OTP REST API used to power many apps and frontends. For years it was the only way to access
OTP programmatically.

Over time it has been replaced by the GraphQL APIs and is scheduled to be disabled by default
and eventually removed completely. It's therefore not recommended to use it.