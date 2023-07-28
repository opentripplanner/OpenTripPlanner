## Routing modes

The routing request parameter `modes` determines which transport modalities should be considered when
calculating the list of routes.

Some modes (mostly bicycle and car) also have optional qualifiers `RENT` and `PARK` to specify if
vehicles are to be parked at a station or rented. In theory this can also apply to other modes but
makes sense only in select cases which are listed below.

Whether a transport mode is available highly depends on the input feeds (GTFS, OSM, bike sharing
feeds) and the graph building options supplied to OTP.

<!-- street-modes BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

<h2 id="Street modes">Street modes</h2>

Routing modes on streets, including walking, biking, driving, and car-sharing.

<h3 id="WALK">WALK</h3>

Walking some or all of the way of the route.

<h3 id="BIKE">BIKE</h3>

Cycling for the entirety of the route or taking a bicycle onto the public transport and cycling from the arrival station to the destination. 

<h3 id="BIKE_TO_PARK">BIKE_TO_PARK</h3>

Leaving the bicycle at the departure station and walking from the arrival station to the destination. <br/> This mode needs to be combined with at least one transit mode (or `TRANSIT`) otherwise it behaves like an ordinary bicycle journey. <br/> _Prerequisite:_ Bicycle parking stations present in the OSM file and visible to OTP by enabling the property `staticBikeParkAndRide` during graph build.

<h3 id="BIKE_RENTAL">BIKE_RENTAL</h3>

Taking a rented, shared-mobility bike for part or the entirety of the route. <br/>  _Prerequisite:_ Vehicle positions need to be added to OTP from dynamic data feeds. <br/>   For dynamic bike positions configure an input feed. See [Configuring real-time updaters](UpdaterConfig.md).

<h3 id="SCOOTER_RENTAL">SCOOTER_RENTAL</h3>

Walking to a scooter rental point, riding a scooter to a scooter rental drop-off point, and walking the rest of the way. <br/> This can include scooter rental at fixed locations or free-floating services.

<h3 id="CAR">CAR</h3>

Driving your own car the entirety of the route. <br/> If this is combined with `TRANSIT`, it will return routes with a [Kiss & Ride](https://en.wikipedia.org/wiki/Park_and_ride#Kiss_and_ride_/_kiss_and_fly) component. This means that the car is not parked in a permanent parking  area but rather the passenger is dropped off (for example, at an airport) and the driver continues driving the car away from the drop off location.

<h3 id="CAR_TO_PARK">CAR_TO_PARK</h3>

Driving a car to the park-and-ride facilities near a station and taking publictransport. <br/> This mode needs to be combined with at least one transit mode (or `TRANSIT`) otherwise, it behaves like an ordinary car journey. <br/> _Prerequisite:_ Park-and-ride areas near the stations need to be present in the OSM input file.

<h3 id="CAR_PICKUP">CAR_PICKUP</h3>

Walking to a pickup point along the road, driving to a drop-off point along the road, and walking the rest of the way. <br/> This can include various taxi-services or kiss & ride.

<h3 id="CAR_RENTAL">CAR_RENTAL</h3>

Walk to a car rental point, drive to a car rental drop-off point and walk the rest of the way. <br/> This can include car rental at fixed locations or free-floating services.

<h3 id="CAR_HAILING">CAR_HAILING</h3>

Using a car hailing app like Uber or Lyft to get to a train station or all the way to the destination.

<h3 id="FLEXIBLE">FLEXIBLE</h3>

Encompasses all types of on-demand and flexible transportation.


<!-- street-modes END -->

<!-- transit-modes BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

<h2 id="Street modes">Street modes</h2>

Routing modes on streets, including walking, biking, driving, and car-sharing.

<h3 id="WALK">WALK</h3>

Walking some or all of the way of the route.

<h3 id="BIKE">BIKE</h3>

Cycling for the entirety of the route or taking a bicycle onto the public transport and cycling from the arrival station to the destination. 

<h3 id="BIKE_TO_PARK">BIKE_TO_PARK</h3>

Leaving the bicycle at the departure station and walking from the arrival station to the destination. <br/> This mode needs to be combined with at least one transit mode (or `TRANSIT`) otherwise it behaves like an ordinary bicycle journey. <br/> _Prerequisite:_ Bicycle parking stations present in the OSM file and visible to OTP by enabling the property `staticBikeParkAndRide` during graph build.

<h3 id="BIKE_RENTAL">BIKE_RENTAL</h3>

Taking a rented, shared-mobility bike for part or the entirety of the route. <br/>  _Prerequisite:_ Vehicle positions need to be added to OTP from dynamic data feeds. <br/>   For dynamic bike positions configure an input feed. See [Configuring real-time updaters](UpdaterConfig.md).

<h3 id="SCOOTER_RENTAL">SCOOTER_RENTAL</h3>

Walking to a scooter rental point, riding a scooter to a scooter rental drop-off point, and walking the rest of the way. <br/> This can include scooter rental at fixed locations or free-floating services.

<h3 id="CAR">CAR</h3>

Driving your own car the entirety of the route. <br/> If this is combined with `TRANSIT`, it will return routes with a [Kiss & Ride](https://en.wikipedia.org/wiki/Park_and_ride#Kiss_and_ride_/_kiss_and_fly) component. This means that the car is not parked in a permanent parking  area but rather the passenger is dropped off (for example, at an airport) and the driver continues driving the car away from the drop off location.

<h3 id="CAR_TO_PARK">CAR_TO_PARK</h3>

Driving a car to the park-and-ride facilities near a station and taking publictransport. <br/> This mode needs to be combined with at least one transit mode (or `TRANSIT`) otherwise, it behaves like an ordinary car journey. <br/> _Prerequisite:_ Park-and-ride areas near the stations need to be present in the OSM input file.

<h3 id="CAR_PICKUP">CAR_PICKUP</h3>

Walking to a pickup point along the road, driving to a drop-off point along the road, and walking the rest of the way. <br/> This can include various taxi-services or kiss & ride.

<h3 id="CAR_RENTAL">CAR_RENTAL</h3>

Walk to a car rental point, drive to a car rental drop-off point and walk the rest of the way. <br/> This can include car rental at fixed locations or free-floating services.

<h3 id="CAR_HAILING">CAR_HAILING</h3>

Using a car hailing app like Uber or Lyft to get to a train station or all the way to the destination.

<h3 id="FLEXIBLE">FLEXIBLE</h3>

Encompasses all types of on-demand and flexible transportation.

<h2 id="Transit modes">Transit modes</h2>

Routing modes for transit, including rail, bus, ferry, etc. Equivalent to [GTFS `route_type`](https://developers.google.com/transit/gtfs/reference/#routestxt) or to NeTEx TransportMode. 

<h3 id="RAIL">RAIL</h3>

Used for intercity or long-distance travel.

<h3 id="COACH">COACH</h3>

Used for long-distance bus routes.

<h3 id="SUBWAY">SUBWAY</h3>

Subway or Metro, used for any underground rail system within a metropolitan area.

<h3 id="BUS">BUS</h3>

Used for short- and long-distance bus routes.

<h3 id="TRAM">TRAM</h3>

Tram, streetcar or light rail. Used for any light rail or street level system within a metropolitan area.

<h3 id="FERRY">FERRY</h3>

Used for short- and long-distance boat service.

<h3 id="AIRPLANE">AIRPLANE</h3>

Part of the [Extended GTFS route types](https://developers.google.com/transit/gtfs/reference/extended-route-types): Taking an airplane

<h3 id="CABLE_CAR">CABLE_CAR</h3>

Used for street-level cable cars where the cable runs beneath the car.

<h3 id="GONDOLA">GONDOLA</h3>

Gondola or suspended cable car. Typically used for aerial cable cars where the car is suspended from the cable.

<h3 id="FUNICULAR">FUNICULAR</h3>

Used for any rail system that moves on steep inclines with a cable traction system.

<h3 id="TROLLEYBUS">TROLLEYBUS</h3>

Used for trolleybus systems which draw power from overhead wires using poles on the roof of the vehicle.

<h3 id="MONORAIL">MONORAIL</h3>

Used for any rail system that runs on a single rail.

<h3 id="CARPOOL">CARPOOL</h3>

Car pooling

<h3 id="TAXI">TAXI</h3>

Using a taxi service


<!-- transit-modes END -->

### Note
Note that there are conceptual overlaps between `TRAM`, `SUBWAY` and `RAIL` and some transport <br/>
providers categorize their routes differently to others. In other words, what is considered <br/>
a `SUBWAY` in one city might be of type `RAIL` in another. <br/> Similarly the `TROLLEYBUS` mode is categorized by some operators as `BUS`. Study your input GTFS feed carefully to <br/>
find out the appropriate mapping in your region. 