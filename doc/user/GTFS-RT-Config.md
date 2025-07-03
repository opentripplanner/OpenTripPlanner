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

## TripUpdates via HTTP(S)

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
| [forwardsDelayPropagationType](#u__5__forwardsDelayPropagationType)   |      `enum`     | How forwards propagation should be handled.                                | *Optional* | `"default"`          |  2.8  |
| frequency                                                             |    `duration`   | How often the data should be downloaded.                                   | *Optional* | `"PT1M"`             |  1.5  |
| fuzzyTripMatching                                                     |    `boolean`    | If the trips should be matched fuzzily.                                    | *Optional* | `false`              |  1.5  |
| [url](#u__5__url)                                                     |     `string`    | The URL of the GTFS-RT resource.                                           | *Required* |                      |  1.5  |
| [headers](#u__5__headers)                                             | `map of string` | HTTP headers to add to the request. Any header key, value can be inserted. | *Optional* |                      |  2.3  |


##### Parameter details

<h4 id="u__5__backwardsDelayPropagationType">backwardsDelayPropagationType</h4>

**Since version:** `2.2` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"required-no-data"`   
**Path:** /updaters/[5]   
**Enum values:** `none` | `required-no-data` | `required` | `always`

How backwards propagation should be handled.

  NONE:
  Do not propagate delays backwards. Reject real-time updates if the times are not specified
  from the beginning of the trip.

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


<h4 id="u__5__forwardsDelayPropagationType">forwardsDelayPropagationType</h4>

**Since version:** `2.8` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"default"`   
**Path:** /updaters/[5]   
**Enum values:** `none` | `default`

How forwards propagation should be handled.

  NONE:
  Do not propagate delays forwards. Reject real-time updates if not all arrival / departure times
  are specified until the end of the trip.

  Note that this will also reject all updates containing NO_DATA, or all updates containing
  SKIPPED stops without a time provided. Only use this value when you can guarantee that the
  real-time feed contains all departure and arrival times for all future stops, including
  SKIPPED stops.

  DEFAULT:
  Default value. Propagate delays forwards for stops without arrival / departure times given.

  For NO_DATA stops, the scheduled time is used unless a previous delay fouls the scheduled time
  at the stop, in such case the minimum amount of delay is propagated to make the times
  non-decreasing.

  For SKIPPED stops without time given, interpolate the estimated time using the ratio between
  scheduled and real times from the previous to the next stop.


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

## Streaming TripUpdates via MQTT

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
| [forwardsDelayPropagationType](#u__6__forwardsDelayPropagationType)   |   `enum`  | How forwards propagation should be handled.  | *Optional* | `"default"`          |  2.8  |
| fuzzyTripMatching                                                     | `boolean` | Whether to match trips fuzzily.              | *Optional* | `false`              |  2.0  |
| qos                                                                   | `integer` | QOS level.                                   | *Optional* | `0`                  |  2.0  |
| topic                                                                 |  `string` | The topic to subscribe to.                   | *Required* |                      |  2.0  |
| url                                                                   |  `string` | URL of the MQTT broker.                      | *Required* |                      |  2.0  |


##### Parameter details

<h4 id="u__6__backwardsDelayPropagationType">backwardsDelayPropagationType</h4>

**Since version:** `2.2` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"required-no-data"`   
**Path:** /updaters/[6]   
**Enum values:** `none` | `required-no-data` | `required` | `always`

How backwards propagation should be handled.

  NONE:
  Do not propagate delays backwards. Reject real-time updates if the times are not specified
  from the beginning of the trip.

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


<h4 id="u__6__forwardsDelayPropagationType">forwardsDelayPropagationType</h4>

**Since version:** `2.8` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"default"`   
**Path:** /updaters/[6]   
**Enum values:** `none` | `default`

How forwards propagation should be handled.

  NONE:
  Do not propagate delays forwards. Reject real-time updates if not all arrival / departure times
  are specified until the end of the trip.

  Note that this will also reject all updates containing NO_DATA, or all updates containing
  SKIPPED stops without a time provided. Only use this value when you can guarantee that the
  real-time feed contains all departure and arrival times for all future stops, including
  SKIPPED stops.

  DEFAULT:
  Default value. Propagate delays forwards for stops without arrival / departure times given.

  For NO_DATA stops, the scheduled time is used unless a previous delay fouls the scheduled time
  at the stop, in such case the minimum amount of delay is propagated to make the times
  non-decreasing.

  For SKIPPED stops without time given, interpolate the estimated time using the ratio between
  scheduled and real times from the previous to the next stop.




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

## Vehicle Positions

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


