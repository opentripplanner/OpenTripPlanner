This page is intended as an exhaustive listing of what OTP's routing engine is capable of and 
therefore documents internal names. Since OTP has multiple APIs where each works slightly differently,
please consult your API documentation on how to select the appropriate mode.

<!-- street-modes BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

<h2 id="Street modes">Street modes</h2>

Routing modes on streets, including walking, biking, driving, and car-sharing.

<h4 id="BIKE">BIKE</h4>

Cycling for the entirety of the route or taking a bicycle onto the public transport and cycling from the arrival station to the destination.

Taking a bicycle onto transit is only possible if information about the permission to do so is supplied in the source data. In GTFS this field
is called `bikesAllowed`.


<h4 id="BIKE_RENTAL">BIKE_RENTAL</h4>

Taking a rented, shared-mobility bike for part or the entirety of the route.

_Prerequisite:_ Vehicle or station locations need to be added to OTP from dynamic data feeds.
See [Configuring GBFS](GBFS-Config.md) on how to add one.


<h4 id="BIKE_TO_PARK">BIKE_TO_PARK</h4>

Leaving the bicycle at the departure station and walking from the arrival station to the destination.
This mode needs to be combined with at least one transit mode otherwise it behaves like an ordinary bicycle journey.

_Prerequisite:_ Bicycle parking stations present in the OSM file and visible to OTP by enabling the property `staticBikeParkAndRide` during graph build.


<h4 id="CAR">CAR</h4>

Driving your own car the entirety of the route.
This can be combined with transit, where will return routes with a [Kiss & Ride](https://en.wikipedia.org/wiki/Park_and_ride#Kiss_and_ride_/_kiss_and_fly) component.
This means that the car is not parked in a permanent parking area but rather the passenger is dropped off (for example, at an airport) and the driver continues driving the car away from the drop off location.


<h4 id="CAR_HAILING">CAR_HAILING</h4>

Using a car hailing app like Uber or Lyft to get to a train station or all the way to the destination.

See [the sandbox documentation](sandbox/RideHailing.md) on how to configure it.


<h4 id="CAR_PICKUP">CAR_PICKUP</h4>

Walking to a pickup point along the road, driving to a drop-off point along the road, and walking the rest of the way. <br/> This can include various taxi-services or kiss & ride.

<h4 id="CAR_RENTAL">CAR_RENTAL</h4>

Walk to a car rental point, drive to a car rental drop-off point and walk the rest of the way.
This can include car rental at fixed locations or free-floating services.

_Prerequisite:_ Vehicle or station locations need to be added to OTP from dynamic data feeds.
See [Configuring GBFS](GBFS-Config.md) on how to add one.


<h4 id="CAR_TO_PARK">CAR_TO_PARK</h4>

Driving a car to the park-and-ride facilities near a station and taking publictransport.
This mode needs to be combined with at least one transit mode otherwise, it behaves like an ordinary car journey.
_Prerequisite:_ Park-and-ride areas near the stations need to be present in the OSM input file.


<h4 id="FLEXIBLE">FLEXIBLE</h4>

Encompasses all types of on-demand and flexible transportation for example GTFS Flex or NeTEx Flexible Stop Places.

<h4 id="SCOOTER_RENTAL">SCOOTER_RENTAL</h4>

Walking to a scooter rental point, riding a scooter to a scooter rental drop-off point, and walking the rest of the way.
This can include scooter rental at fixed locations or free-floating services.

_Prerequisite:_ Vehicle or station locations need to be added to OTP from dynamic data feeds.
See [Configuring GBFS](GBFS-Config.md) on how to add one.


<h4 id="WALK">WALK</h4>

Walking some or all of the way of the route.


<!-- street-modes END -->

<!-- transit-modes BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

<h2 id="Transit modes">Transit modes</h2>

Routing modes for transit, including rail, bus, ferry, etc. Equivalent to [GTFS `route_type`](https://developers.google.com/transit/gtfs/reference/#routestxt) or to NeTEx TransportMode. 

<h4 id="AIRPLANE">AIRPLANE</h4>

Taking an airplane

<h4 id="BUS">BUS</h4>

Used for short- and long-distance bus routes.

<h4 id="CABLE_CAR">CABLE_CAR</h4>

Used for street-level rail cars where the cable runs beneath the vehicle.

<h4 id="CARPOOL">CARPOOL</h4>

Private car trips shared with others.

This is currently not specified in GTFS so we use the mode type values 1550-1560 which are in the range of private taxis.


<h4 id="COACH">COACH</h4>

Used for long-distance bus routes.

<h4 id="FERRY">FERRY</h4>

Used for short- and long-distance boat service.

<h4 id="FUNICULAR">FUNICULAR</h4>

Used for any rail system that moves on steep inclines with a cable traction system.

<h4 id="GONDOLA">GONDOLA</h4>

Gondola or suspended cable car. Typically used for aerial cable cars where the car is suspended from the cable.

<h4 id="MONORAIL">MONORAIL</h4>

Used for any rail system that runs on a single rail.

<h4 id="RAIL">RAIL</h4>

Used for intercity or long-distance travel.

<h4 id="SUBWAY">SUBWAY</h4>

Subway or Metro, used for any underground rail system within a metropolitan area.

<h4 id="TAXI">TAXI</h4>

Using a taxi service

<h4 id="TRAM">TRAM</h4>

Tram, streetcar or light rail. Used for any light rail or street level system within a metropolitan area.

<h4 id="TROLLEYBUS">TROLLEYBUS</h4>

Used for trolleybus systems which draw power from overhead wires using poles on the roof of the vehicle.


<!-- transit-modes END -->
