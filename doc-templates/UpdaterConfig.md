<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc-templates
   - Generated directory is: /docs 
-->


# Updater configuration

This section covers all options that can be set in the *router-config.json* in the 
[updaters](RouterConfiguration.md) section.

Real-time data are those that are not added to OTP during the graph build phase but during runtime.

Real-time data sources are configured in the `updaters` section is an array of JSON objects, each
of which has a `type` field and other configuration fields specific to that type. 

## GTFS-Realtime

GTFS feeds contain *schedule* data that is published by an agency or operator in advance. The
feed does not account for unexpected service changes or traffic disruptions that occur from day to
day. Thus, this kind of data is also referred to as 'static' data or 'theoretical' arrival and
departure times.

[GTFS-Realtime](https://gtfs.org/realtime/) complements GTFS with three
additional kinds of feeds. In contrast to the base GTFS schedule feed, they provide *real-time*
updates (*'dynamic'* data) and are updated from minute to minute.


### Alerts

Alerts are text messages attached to GTFS objects, informing riders of disruptions and changes. 
The information is downloaded in a single HTTP request and polled regularly.

<!-- INSERT: real-time-alerts -->


### TripUpdates via HTTP(S)

TripUpdates report on the status of scheduled trips as they happen, providing observed and 
predicted arrival and departure times for the remainder of the trip.
The information is downloaded in a single HTTP request and polled regularly.

<!-- INSERT: stop-time-updater -->


### TripUpdates via WebSocket

This updater doesn't poll a data source but opens a persistent connection to the GTFS-RT provider, 
which then sends incremental updates immediately as they become available.

The [OneBusAway GTFS-realtime exporter project](https://github.com/OneBusAway/onebusaway-gtfs-realtime-exporter)
provides this kind of streaming, incremental updates over a websocket rather than a single large
file.

<!-- INSERT: websocket-gtfs-rt-updater -->


### Vehicle Positions

VehiclePositions give the location of some or all vehicles currently in service, in terms of 
geographic coordinates or position relative to their scheduled stops.
The information is downloaded in a single HTTP request and polled regularly.

<!-- INSERT: vehicle-positions -->


## GBFS vehicle rental systems

Besides GTFS-RT transit data, OTP can also fetch real-time data about vehicle rental networks
including the number of bikes and free parking spaces at each station. We support vehicle rental
systems that use the GBFS feed format.

[GBFS](https://github.com/NABSA/gbfs) is used for a variety of shared mobility services, with
partial support for both v1 and v2.2 ([list of known GBFS feeds](https://github.com/NABSA/gbfs/blob/master/systems.csv)).

<!-- INSERT: vehicle-rental -->

## Other updaters in sandboxes

- [Vehicle parking](sandbox/VehicleParking.md)
- [Siri over HTTP](sandbox/SiriUpdater.md)
- [Siri over Azure Message Bus](sandbox/SiriAzureUpdater.md)
- [VehicleRentalServiceDirectory](sandbox/VehicleRentalServiceDirectory.md)

