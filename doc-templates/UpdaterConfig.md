<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc-templates
   - Generated directory is: /docs 
-->


# Updater configuration

This section covers all options that can be set in the *router-config.json* in the 
[updaters](RouterConfiguration.md) section.


## Real-time data

GTFS feeds contain *schedule* data that is published by an agency or operator in advance. The
feed does not account for unexpected service changes or traffic disruptions that occur from day to
day. Thus, this kind of data is also referred to as 'static' data or 'theoretical' arrival and
departure times.

Real-time data sources are configured in the `updaters` section is an array of JSON objects, each
of which has a `type` field and other configuration fields specific to that type. Common to all
updater entries that connect to a network resource is the `url` field.

### GTFS-Realtime

The [GTFS-RT spec](https://developers.google.com/transit/gtfs-realtime/) complements GTFS with three
additional kinds of feeds. In contrast to the base GTFS schedule feed, they provide *real-time*
updates (*'dynamic'* data) and are updated from minute to minute.

### Configuring real-time updaters

Real-time data can be provided using either a pull or push system. In a pull configuration, the
GTFS-RT consumer polls the real-time provider over HTTP. That is to say, OTP fetches a file from a
web server every few minutes. In the push configuration, the consumer opens a persistent connection
to the GTFS-RT provider, which then sends incremental updates immediately as they become available.
OTP can use both approaches.
The [OneBusAway GTFS-realtime exporter project](https://github.com/OneBusAway/onebusaway-gtfs-realtime-exporter)
provides this kind of streaming, incremental updates over a websocket rather than a single large
file.


### Realtime Alerts

Alerts are text messages attached to GTFS objects, informing riders of disruptions and changes.

<!-- INSERT: real-time-alerts -->


### TripUpdates

TripUpdates report on the status of scheduled trips as they happen, providing observed and 
predicted arrival and departure times for the remainder of the trip.

<!-- INSERT: stop-time-updater -->


### TripUpdates Websocket GTFS RT

<!-- INSERT: websocket-gtfs-rt-updater -->


### Vehicle Positions

VehiclePositions give the location of some or all vehicles currently in service, in terms of 
geographic coordinates or position relative to their scheduled stops.

<!-- INSERT: vehicle-positions -->


### Vehicle rental systems using GBFS

Besides GTFS-RT transit data, OTP can also fetch real-time data about vehicle rental networks
including the number of bikes and free parking spaces at each station. We support vehicle rental
systems from using GBFS feed format.


[GBFS](https://github.com/NABSA/gbfs) is used for a variety of shared mobility services, with
partial support for both v1 and v2.2 ([list of known GBFS feeds](https://github.com/NABSA/gbfs/blob/master/systems.csv)).


#### Arriving with rental bikes at the destination

In some cases it may be useful to not drop off the rented bicycle before arriving at the
destination. This is useful if bicycles may only be rented for round trips, or the destination is an
intermediate place.

For this to be possible three things need to be configured:

1. In the updater configuration `allowKeepingRentedBicycleAtDestination` should be set to `true`.

2. `allowKeepingRentedBicycleAtDestination` should also be set for each request, either using
   [routing defaults](#routing-defaults), or per-request.

3. If keeping the bicycle at the destination should be discouraged, then
   `keepingRentedBicycleAtDestinationCost` (default: `0`) may also be set in the
   [routing defaults](#routing-defaults).

#### Header Settings
Sometimes GBFS Feeds might need some headers e.g. for authentication. For those use cases headers
can be configured as a json. Any header key, value can be inserted.


<!-- INSERT: vehicle-rental -->


### Vehicle parking (sandbox feature)

Vehicle parking options and configuration is documented in
its [sandbox documentation](sandbox/VehicleParking.md).

<!-- INSERT: vehicle-parking -->



### SIRI SX updater for Azure Service Bus (sandbox feature)

This is a Sandbox updater se [sandbox documentation](sandbox/SiriAzureUpdater.md).

<!-- INSERT: siri-azure-sx-updater -->


### Vehicle Rental Service Directory configuration (sandbox feature)

To configure and url for
the [VehicleRentalServiceDirectory](sandbox/VehicleRentalServiceDirectory.md).

