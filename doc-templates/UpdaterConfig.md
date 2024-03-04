<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc-templates
   - Generated directory is: /docs 
-->


# Updater configuration

This section covers options that can be set in the updaters section of `router-config.json`. 
See the parameter summary and examples in the router configuration documentation

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

### Streaming TripUpdates via MQTT

This updater connects to an MQTT broker and processes TripUpdates in a streaming fashion. This means
that they will be applied individually in near-realtime rather than in batches at a certain interval.

This system powers the realtime updates in Helsinki and more information can be found 
[on Github](https://github.com/HSLdevcom/transitdata).

<!-- INSERT: mqtt-gtfs-rt-updater -->

### Vehicle Positions

VehiclePositions give the location of some or all vehicles currently in service, in terms of 
geographic coordinates or position relative to their scheduled stops.
The information is downloaded in a single HTTP request and polled regularly.

<!-- INSERT: vehicle-positions -->


## GBFS vehicle rental systems

Besides GTFS-RT transit data, OTP can also fetch real-time data about vehicle rental networks
including the number of vehicles and free parking spaces at each station. We support vehicle rental
systems that use the GBFS standard.

[GBFS](https://github.com/NABSA/gbfs) can be used for a variety of shared mobility services, with
partial support for both v1 and v2.2 ([list of known GBFS feeds](https://github.com/NABSA/gbfs/blob/master/systems.csv)). OTP supports the following
GBFS form factors:

- bicycle
- scooter
- car

<!-- INSERT: vehicle-rental -->

## Other updaters in sandboxes

- [Vehicle parking](sandbox/VehicleParking.md)
- [Siri over HTTP](sandbox/siri/SiriUpdater.md)
- [Siri over Azure Message Bus](sandbox/siri/SiriAzureUpdater.md)
- [VehicleRentalServiceDirectory](sandbox/VehicleRentalServiceDirectory.md)

