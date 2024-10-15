<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc/templates
   - Generated directory is: /doc/user 
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

<!-- real-time-alerts BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter          |       Type      | Summary                                                                      |  Req./Opt. | Default Value | Since |
|---------------------------|:---------------:|------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "real-time-alerts" |      `enum`     | The type of the updater.                                                     | *Required* |               |  1.5  |
| earlyStartSec             |    `integer`    | How long before the posted start of an event it should be displayed to users | *Optional* | `0`           |  1.5  |
| feedId                    |     `string`    | The id of the feed to apply the alerts to.                                   | *Required* |               |  1.5  |
| frequency                 |    `duration`   | How often the URL should be fetched.                                         | *Optional* | `"PT1M"`      |  1.5  |
| fuzzyTripMatching         |    `boolean`    | Whether to match trips fuzzily.                                              | *Optional* | `false`       |  1.5  |
| url                       |     `string`    | URL to fetch the GTFS-RT feed from.                                          | *Required* |               |  1.5  |
| [headers](#u_0_headers)   | `map of string` | HTTP headers to add to the request. Any header key, value can be inserted.   | *Optional* |               |  2.3  |


##### Parameter details

<h4 id="u_0_headers">headers</h4>

**Since version:** `2.3` ∙ **Type:** `map of string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[0] 

HTTP headers to add to the request. Any header key, value can be inserted.



##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "real-time-alerts",
      "frequency" : "30s",
      "url" : "http://developer.trimet.org/ws/V1/FeedSpecAlerts/appID/0123456789ABCDEF",
      "feedId" : "TriMet",
      "headers" : {
        "Some-Header" : "A-Value"
      }
    }
  ]
}
```

<!-- real-time-alerts END -->


### TripUpdates via HTTP(S)

TripUpdates report on the status of scheduled trips as they happen, providing observed and 
predicted arrival and departure times for the remainder of the trip.
The information is downloaded in a single HTTP request and polled regularly.

<!-- stop-time-updater BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                                      |       Type      | Summary                                                                    |  Req./Opt. | Default Value        | Since |
|-----------------------------------------------------------------------|:---------------:|----------------------------------------------------------------------------|:----------:|----------------------|:-----:|
| type = "stop-time-updater"                                            |      `enum`     | The type of the updater.                                                   | *Required* |                      |  1.5  |
| [backwardsDelayPropagationType](#u__5__backwardsDelayPropagationType) |      `enum`     | How backwards propagation should be handled.                               | *Optional* | `"required-no-data"` |  2.2  |
| feedId                                                                |     `string`    | Which feed the updates apply to.                                           | *Required* |                      |  1.5  |
| frequency                                                             |    `duration`   | How often the data should be downloaded.                                   | *Optional* | `"PT1M"`             |  1.5  |
| fuzzyTripMatching                                                     |    `boolean`    | If the trips should be matched fuzzily.                                    | *Optional* | `false`              |  1.5  |
| [url](#u__5__url)                                                     |     `string`    | The URL of the GTFS-RT resource.                                           | *Required* |                      |  1.5  |
| [headers](#u__5__headers)                                             | `map of string` | HTTP headers to add to the request. Any header key, value can be inserted. | *Optional* |                      |  2.3  |


##### Parameter details

<h4 id="u__5__backwardsDelayPropagationType">backwardsDelayPropagationType</h4>

**Since version:** `2.2` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"required-no-data"`   
**Path:** /updaters/[5]   
**Enum values:** `required-no-data` | `required` | `always`

How backwards propagation should be handled.

  REQUIRED_NO_DATA:
  Default value. Only propagates delays backwards when it is required to ensure that the times
  are increasing, and it sets the NO_DATA flag on the stops so these automatically updated times
  are not exposed through APIs.

  REQUIRED:
  Only propagates delays backwards when it is required to ensure that the times are increasing.
  The updated times are exposed through APIs.

  ALWAYS
  Propagates delays backwards on stops with no estimates regardless if it's required or not.
  The updated times are exposed through APIs.


<h4 id="u__5__url">url</h4>

**Since version:** `1.5` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[5] 

The URL of the GTFS-RT resource.

`file:` URLs are also supported if you want to read a file from the local disk.

<h4 id="u__5__headers">headers</h4>

**Since version:** `2.3` ∙ **Type:** `map of string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[5] 

HTTP headers to add to the request. Any header key, value can be inserted.



##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "stop-time-updater",
      "frequency" : "1m",
      "backwardsDelayPropagationType" : "REQUIRED_NO_DATA",
      "url" : "http://developer.trimet.org/ws/V1/TripUpdate/appID/0123456789ABCDEF",
      "feedId" : "TriMet",
      "headers" : {
        "Authorization" : "A-Token"
      }
    }
  ]
}
```

<!-- stop-time-updater END -->

### Streaming TripUpdates via MQTT

This updater connects to an MQTT broker and processes TripUpdates in a streaming fashion. This means
that they will be applied individually in near-realtime rather than in batches at a certain interval.

This system powers the realtime updates in Helsinki and more information can be found 
[on Github](https://github.com/HSLdevcom/transitdata).

<!-- mqtt-gtfs-rt-updater BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                                      |    Type   | Summary                                      |  Req./Opt. | Default Value        | Since |
|-----------------------------------------------------------------------|:---------:|----------------------------------------------|:----------:|----------------------|:-----:|
| type = "mqtt-gtfs-rt-updater"                                         |   `enum`  | The type of the updater.                     | *Required* |                      |  1.5  |
| [backwardsDelayPropagationType](#u__6__backwardsDelayPropagationType) |   `enum`  | How backwards propagation should be handled. | *Optional* | `"required-no-data"` |  2.2  |
| feedId                                                                |  `string` | The feed id to apply the updates to.         | *Required* |                      |  2.0  |
| fuzzyTripMatching                                                     | `boolean` | Whether to match trips fuzzily.              | *Optional* | `false`              |  2.0  |
| qos                                                                   | `integer` | QOS level.                                   | *Optional* | `0`                  |  2.0  |
| topic                                                                 |  `string` | The topic to subscribe to.                   | *Required* |                      |  2.0  |
| url                                                                   |  `string` | URL of the MQTT broker.                      | *Required* |                      |  2.0  |


##### Parameter details

<h4 id="u__6__backwardsDelayPropagationType">backwardsDelayPropagationType</h4>

**Since version:** `2.2` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"required-no-data"`   
**Path:** /updaters/[6]   
**Enum values:** `required-no-data` | `required` | `always`

How backwards propagation should be handled.

  REQUIRED_NO_DATA:
  Default value. Only propagates delays backwards when it is required to ensure that the times
  are increasing, and it sets the NO_DATA flag on the stops so these automatically updated times
  are not exposed through APIs.

  REQUIRED:
  Only propagates delays backwards when it is required to ensure that the times are increasing.
  The updated times are exposed through APIs.

  ALWAYS:
  Propagates delays backwards on stops with no estimates regardless if it's required or not.
  The updated times are exposed through APIs.




##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "mqtt-gtfs-rt-updater",
      "url" : "tcp://pred.rt.hsl.fi",
      "topic" : "gtfsrt/v2/fi/hsl/tu",
      "feedId" : "HSL",
      "fuzzyTripMatching" : true
    }
  ]
}
```

<!-- mqtt-gtfs-rt-updater END -->

### Vehicle Positions

VehiclePositions give the location of some or all vehicles currently in service, in terms of 
geographic coordinates or position relative to their scheduled stops.
The information is downloaded in a single HTTP request and polled regularly.

<!-- vehicle-positions BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter            |       Type      | Summary                                                                    |  Req./Opt. | Default Value | Since |
|-----------------------------|:---------------:|----------------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "vehicle-positions"  |      `enum`     | The type of the updater.                                                   | *Required* |               |  1.5  |
| feedId                      |     `string`    | Feed ID to which the update should be applied.                             | *Required* |               |  2.2  |
| frequency                   |    `duration`   | How often the positions should be updated.                                 | *Optional* | `"PT1M"`      |  2.2  |
| fuzzyTripMatching           |    `boolean`    | Whether to match trips fuzzily.                                            | *Optional* | `false`       |  2.5  |
| url                         |      `uri`      | The URL of GTFS-RT protobuf HTTP resource to download the positions from.  | *Required* |               |  2.2  |
| [features](#u__7__features) |    `enum set`   | Which features of GTFS RT vehicle positions should be loaded into OTP.     | *Optional* |               |  2.5  |
| [headers](#u__7__headers)   | `map of string` | HTTP headers to add to the request. Any header key, value can be inserted. | *Optional* |               |  2.3  |


##### Parameter details

<h4 id="u__7__features">features</h4>

**Since version:** `2.5` ∙ **Type:** `enum set` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[7]   
**Enum values:** `position` | `stop-position` | `occupancy`

Which features of GTFS RT vehicle positions should be loaded into OTP.

<h4 id="u__7__headers">headers</h4>

**Since version:** `2.3` ∙ **Type:** `map of string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[7] 

HTTP headers to add to the request. Any header key, value can be inserted.



##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "vehicle-positions",
      "url" : "https://s3.amazonaws.com/kcm-alerts-realtime-prod/vehiclepositions.pb",
      "feedId" : "1",
      "frequency" : "1m",
      "headers" : {
        "Header-Name" : "Header-Value"
      },
      "fuzzyTripMatching" : false,
      "features" : [
        "position"
      ]
    }
  ]
}
```

<!-- vehicle-positions END -->


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

<!-- vehicle-rental BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                                                      |       Type      | Summary                                                                         |  Req./Opt. | Default Value | Since |
|---------------------------------------------------------------------------------------|:---------------:|---------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "vehicle-rental"                                                               |      `enum`     | The type of the updater.                                                        | *Required* |               |  1.5  |
| [allowKeepingRentedVehicleAtDestination](#u_1_allowKeepingRentedVehicleAtDestination) |    `boolean`    | If a vehicle should be allowed to be kept at the end of a station-based rental. | *Optional* | `false`       |  2.1  |
| frequency                                                                             |    `duration`   | How often the data should be updated.                                           | *Optional* | `"PT1M"`      |  1.5  |
| [geofencingZones](#u_1_geofencingZones)                                               |    `boolean`    | Compute rental restrictions based on GBFS 2.2 geofencing zones.                 | *Optional* | `false`       |  2.3  |
| language                                                                              |     `string`    | TODO                                                                            | *Optional* |               |  2.1  |
| [network](#u_1_network)                                                               |     `string`    | The name of the network to override the one derived from the source data.       | *Optional* |               |  1.5  |
| overloadingAllowed                                                                    |    `boolean`    | Allow leaving vehicles at a station even though there are no free slots.        | *Optional* | `false`       |  2.2  |
| [sourceType](#u_1_sourceType)                                                         |      `enum`     | What source of vehicle rental updater to use.                                   | *Required* |               |  1.5  |
| url                                                                                   |     `string`    | The URL to download the data from.                                              | *Required* |               |  1.5  |
| [headers](#u_1_headers)                                                               | `map of string` | HTTP headers to add to the request. Any header key, value can be inserted.      | *Optional* |               |  1.5  |


##### Parameter details

<h4 id="u_1_allowKeepingRentedVehicleAtDestination">allowKeepingRentedVehicleAtDestination</h4>

**Since version:** `2.1` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /updaters/[1] 

If a vehicle should be allowed to be kept at the end of a station-based rental.

In some cases it may be useful to not drop off the rented vehicle before arriving at the destination.
This is useful if vehicles may only be rented for round trips, or the destination is an intermediate place.

For this to be possible three things need to be configured:

 - In the updater configuration `allowKeepingRentedVehicleAtDestination` should be set to `true`.
 - `allowKeepingRentedVehicleAtDestination` should also be set for each request, either using routing defaults, or per-request.
 - If keeping the vehicle at the destination should be discouraged, then `keepingRentedVehicleAtDestinationCost` (default: 0) may also be set in the routing defaults.


<h4 id="u_1_geofencingZones">geofencingZones</h4>

**Since version:** `2.3` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /updaters/[1] 

Compute rental restrictions based on GBFS 2.2 geofencing zones.

This feature is somewhat experimental and therefore turned off by default for the following reasons:

- It delays start up of OTP. How long is dependent on the complexity of the zones. For example in Oslo it takes 6 seconds to compute while Portland takes 25 seconds.
- It's easy for a malformed or unintended geofencing zone to make routing impossible. If you encounter such a case, please file a bug report.


<h4 id="u_1_network">network</h4>

**Since version:** `1.5` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[1] 

The name of the network to override the one derived from the source data.

GBFS feeds must include a system_id which will be used as the default `network`. These ids are sometimes not helpful so setting this property will override it.

<h4 id="u_1_sourceType">sourceType</h4>

**Since version:** `1.5` ∙ **Type:** `enum` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[1]   
**Enum values:** `gbfs` | `smoove`

What source of vehicle rental updater to use.

<h4 id="u_1_headers">headers</h4>

**Since version:** `1.5` ∙ **Type:** `map of string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[1] 

HTTP headers to add to the request. Any header key, value can be inserted.



##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "vehicle-rental",
      "network" : "socialbicycles_coast",
      "sourceType" : "gbfs",
      "language" : "en",
      "frequency" : "1m",
      "allowKeepingRentedVehicleAtDestination" : false,
      "geofencingZones" : false,
      "url" : "http://coast.socialbicycles.com/opendata/gbfs.json",
      "headers" : {
        "Auth" : "<any-token>",
        "<key>" : "<value>"
      }
    }
  ]
}
```

<!-- vehicle-rental END -->

## Other updaters in sandboxes

- [Vehicle parking](sandbox/VehicleParking.md)
- [Siri over HTTP](sandbox/siri/SiriUpdater.md)
- [Siri over Google Cloud PubSub](sandbox/siri/SiriGooglePubSubUpdater.md)
- [Siri over Azure Message Bus](sandbox/siri/SiriAzureUpdater.md)
- [VehicleRentalServiceDirectory](sandbox/VehicleRentalServiceDirectory.md)

