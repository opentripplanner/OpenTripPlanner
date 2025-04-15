# APIs

Several services are built upon OTP's routing and transit data indexing engines. They expose these APIs:

The [GTFS GraphQL API](GTFS-GraphQL-API.md) has been used by the Digitransit and otp-react-redux 
projects as a general purpose routing and transit data API in production for many years. 
If your input data is mostly GTFS then this is probably the best choice as it uses the same vocabulary.

The [Transmodel GraphQL API](TransmodelApi.md) is used at
Entur in production since 2020. Like the GTFS GraphQL API it is also a general purpose API.
If your input data is mostly NeTeX then you might want to investigate
this API as it uses the [Transmodel vocabulary](https://en.wikipedia.org/wiki/Transmodel) to describe 
its entities.

The [Vector tiles API](../sandbox/MapboxVectorTilesApi.md) is a special purpose API for displaying
entities on a vector map.

The [Actuator API](../sandbox/ActuatorAPI.md) provides endpoints for checking the health status of the
OTP instance and reading live application metrics. 

The [Geocoder API](../sandbox/GeocoderAPI.md) allows you to geocode stop names and codes.

The OTP REST API was removed in 2025.
