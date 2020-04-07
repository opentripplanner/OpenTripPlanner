# GTFS-Flex routing

Many agencies run flexible services to complement their fixed-route service. "Flexible" service does
not follow a strict timetable or route. It may include any of the following features: boardings
or alightings outside its scheduled timetable and route; booking and scheduling in advance; or
transit parameters which depend on customer requests ("demand-responsive transit" or DRT). These
services are typically used in rural areas or for mobility-impaired riders.
 
A GTFS extension called [GTFS-Flex](https://github.com/MobilityData/gtfs-flex/blob/master/spec/reference.md) defines
how to model some kinds of flexible transit. A subset of GTFS-Flex has been implemented in 
OpenTripPlanner as part of US DOT's [Mobility-on-Demand Sandbox Grant](https://www.transit.dot.gov/research-innovation/fiscal-year-2016-mobility-demand-mod-sandbox-program-projects).

In particular, OTP now has support for these modes of GTFS-Flex:

- "flag stops", in which a passenger can flag down the a vehicle along its route to board, or
alight in between stops
- "deviated-route service", in which a vehicle can deviate from its route within an area or radius to
do a dropoff or pickup
- "call-and-ride", which is an entirely deviated, point-to-point segment.

These modes can co-exist with fixed-route transit, and with each other. For example, some agencies 
have fixed-route services that start in urban areas, where passengers must board at designated
stops, but end in rural areas where passengers can board and alight wherever they please. A
fixed-route service may terminate in an defined area where it can drop off passengers anywhere --
or have such an area at the beginning or middle of its route. A vehicle may be able to deviate a
certain radius outside its scheduled route to pick up or drop off passengers. If both a pickup and
dropoff occur in between scheduled timepoints, from the passenger's perspective, the service may
look like a call-and-ride trip. Other call-and-ride services may operate more like taxis, in which
all rides are independently scheduled.

## Configuration

In order to use flexible routing, an OTP graph must be built with a GTFS-Flex dataset and
OpenStreetMap data. The GTFS data must include `shapes.txt`.

In addition, the parameter `useFlexService: true` must be added to `router-config.json`.

A number of routing parameters can be used to control aspects of flexible service. These parameters
typically change the relative cost of using various flexible services relative to fixed-route
transit. All flex-related parameters begin with the prefix "flex" and can be found in the Javadocs
for `RoutingRequest.java`.

The following example `router-config.json` enables flexible routing and sets some parameters:

    {
      "useFlexService": true,
      "routingDefaults": {
        "flexCallAndRideReluctance": 3,
        "flexMaxCallAndRideSeconds": 7200,
        "flexFlagStopExtraPenalty": 180
      }
    }
    
## Implementation

The general approach of the GTFS-Flex implementation is as follows: prior to the main graph search,
special searches are run around the origin and destination to discover possible flexible options.
One search is with the WALK mode, to find flag stops, and the other is in the CAR mode, to find
deviated-route and call-and-ride options. These searches result in the creation of temporary, 
request-specific vertices and edges. Then, the graph search proceeds as normal. Temporary graph
structures are disposed at the end of the request's lifecycle.

For flag stops and deviated-route service, timepoints in between scheduled locations are determined
via linear interpolation. For example, say a particular trip departs stop A at 9:00am and arrives
at stop B at 9:30am. A passenger would be able to board 20% of the way in between stop A and stop B
at 9:06am, since 20% of 30 minutes is 6 minutes.

For deviated-route service and call-and-ride service, the most pessimistic assumptions of vehicle
travel time are used -- e.g. vehicle travel time is calculated via the `drt_max_travel_time`
formula in the GTFS-Flex (see the spec [here](https://github.com/MobilityData/gtfs-flex/blob/master/spec/reference.md#defining-service-parameters)).