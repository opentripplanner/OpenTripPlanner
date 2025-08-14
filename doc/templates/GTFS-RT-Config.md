<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc/templates
   - Generated directory is: /doc/user 
-->

GTFS feeds contain *schedule* data that is published by an agency or operator in advance. The
feed does not account for unexpected service changes or traffic disruptions that occur from day to
day. Thus, this kind of data is also referred to as 'static' data or 'scheduled' arrival and
departure times.

[GTFS-Realtime](https://gtfs.org/realtime/) complements GTFS with 
additional kinds of feeds. In contrast to the base GTFS schedule feed, they provide *real-time*
updates (*'dynamic'* data) and are updated from minute to minute.

## Alerts

Alerts are text messages attached to GTFS objects, informing riders of disruptions and changes. 
The information is downloaded in a single HTTP request and polled regularly.

<!-- INSERT: real-time-alerts -->

## TripUpdates via HTTP(S)

TripUpdates report on the status of scheduled trips as they happen, providing observed and 
predicted arrival and departure times for the remainder of the trip.
The information is downloaded in a single HTTP request and polled regularly.

<!-- INSERT: stop-time-updater -->

## Streaming TripUpdates via MQTT

This updater connects to an MQTT broker and processes TripUpdates in a streaming fashion. This means
that they will be applied individually in near-real-time rather than in batches at a certain interval.

This system powers the real-time updates in Helsinki and more information can be found 
[on Github](https://github.com/HSLdevcom/transitdata).

<!-- INSERT: mqtt-gtfs-rt-updater -->

## Vehicle Positions

VehiclePositions give the location of some or all vehicles currently in service, in terms of 
geographic coordinates or position relative to their scheduled stops.
The information is downloaded in a single HTTP request and polled regularly.

<!-- INSERT: vehicle-positions -->


