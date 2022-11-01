## Routing modes

The routing request parameter `modes` determines which transport modalities should be considered when
calculating the list of routes.

Some modes (mostly bicycle and car) also have optional qualifiers `RENT` and `PARK` to specify if
vehicles are to be parked at a station or rented. In theory this can also apply to other modes but
makes sense only in select cases which are listed below.

Whether a transport mode is available highly depends on the input feeds (GTFS, OSM, bike sharing
feeds) and the graph building options supplied to OTP.

The complete list of modes are:

- `WALK`: Walking some or all of the route.

- `TRANSIT`: General catch-all for all public transport modes.

- `BICYCLE`: Cycling for the entirety of the route or taking a bicycle onto the public transport and
  cycling from the arrival station to the destination.

- `BICYCLE_RENT`: Taking a rented, shared-mobility bike for part or the entirety of the route.

  _Prerequisite:_ Vehicle positions need to be added to OTP from dynamic data feeds.

  For dynamic bike positions configure an input feed.
  See [Configuring real-time updaters](RouterConfiguration.md#Configuring real-time updaters).

- `BICYCLE_PARK`: Leaving the bicycle at the departure station and walking from the arrival station
  to the destination.

  This mode needs to be combined with at least one transit mode (or `TRANSIT`) otherwise it behaves
  like an ordinary bicycle journey.

  _Prerequisite:_ Bicycle parking stations present in the OSM file and visible to OTP by enabling
  the property `staticBikeParkAndRide` during graph build.

- `CAR`: Driving your own car the entirety of the route.

  If this is combined with `TRANSIT` it will return routes with a
  [Kiss & Ride](https://en.wikipedia.org/wiki/Park_and_ride#Kiss_and_ride_/_kiss_and_fly) component.
  This means that the car is not parked in a permanent parking area but rather the passenger is
  dropped off (for example, at an airport) and the driver continues driving the car away from the
  drop off location.

- `CAR_PARK`: Driving a car to the park-and-ride facilities near a station and taking public
  transport.

  This mode needs to be combined with at least one transit mode (or `TRANSIT`) otherwise it behaves
  like an ordinary car journey.

  _Prerequisite:_ Park-and-ride areas near the station need to be present in the OSM input file.

The following modes are 1-to-1 mappings from
the [GTFS `route_type`](https://developers.google.com/transit/gtfs/reference/#routestxt):

- `TRAM`: Tram, streetcar, or light rail. Used for any light rail or street-level system within a
  metropolitan area.

- `SUBWAY`: Subway or metro. Used for any underground rail system within a metropolitan area.

- `RAIL`: Used for intercity or long-distance travel.

- `BUS`: Used for short- and long-distance bus routes.

- `FERRY`: Ferry. Used for short- and long-distance boat service.

- `CABLE_CAR`: Cable car. Used for street-level cable cars where the cable runs beneath the car.

- `GONDOLA`: Gondola or suspended cable car. Typically used for aerial cable cars where the car is
  suspended from the cable.

- `FUNICULAR`: Funicular. Used for any rail system that moves on steep inclines with a cable
  traction system.

Lastly, this mode is part of
the [Extended GTFS route types](https://developers.google.com/transit/gtfs/reference/extended-route-types):

- `AIRPLANE`: Taking an airplane.

Note that there are conceptual overlaps between `TRAM`, `SUBWAY` and `RAIL` and some transport
providers categorize their routes differently to others. In other words, what is considered
a `SUBWAY` in one city might be of type `RAIL` in another. Study your input GTFS feed carefully to
find out the appropriate mapping in your region.

