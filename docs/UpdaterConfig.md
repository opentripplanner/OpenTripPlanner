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

<!-- real-time-alerts BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter          |    Type   | Summary                  |  Req./Opt. | Default Value | Since |
|---------------------------|:---------:|--------------------------|:----------:|---------------|:-----:|
| type = "REAL_TIME_ALERTS" |   `enum`  | The type of the updater. | *Required* |               |   na  |
| earlyStartSec             | `integer` | TODO                     | *Optional* | `0`           |   na  |
| feedId                    |  `string` | TODO                     | *Optional* |               |   na  |
| frequencySec              | `integer` | TODO                     | *Optional* | `60`          |   na  |
| fuzzyTripMatching         | `boolean` | TODO                     | *Optional* | `false`       |   na  |
| url                       |  `string` | TODO                     | *Required* |               |   na  |


```JSON
// router-config.json
{
    "updaters": [
    {
      "type" : "real-time-alerts",
      "frequencySec" : 30,
      "url" : "http://developer.trimet.org/ws/V1/FeedSpecAlerts/appID/0123456789ABCDEF",
      "feedId" : "TriMet"
    }
  ]

}
```

<!-- real-time-alerts END -->


### TripUpdates

TripUpdates report on the status of scheduled trips as they happen, providing observed and 
predicted arrival and departure times for the remainder of the trip.

<!-- stop-time-updater BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                                      |    Type   | Summary                                             |  Req./Opt. | Default Value        | Since |
|-----------------------------------------------------------------------|:---------:|-----------------------------------------------------|:----------:|----------------------|:-----:|
| type = "STOP_TIME_UPDATER"                                            |   `enum`  | The type of the updater.                            | *Required* |                      |   na  |
| [backwardsDelayPropagationType](#u__5__backwardsDelayPropagationType) |   `enum`  | How backwards propagation should be handled.        | *Optional* | `"required-no-data"` |  2.2  |
| feedId                                                                |  `string` | Which feed the updates apply to.                    | *Optional* |                      |   na  |
| frequencySec                                                          | `integer` | How often the data should be downloaded in seconds. | *Optional* | `60`                 |   na  |
| fuzzyTripMatching                                                     | `boolean` | If the trips should be matched fuzzily.             | *Optional* | `false`              |   na  |
| url                                                                   |  `string` | The URL of the GTFS-RT resource.                    | *Required* |                      |   na  |


#### Details

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




```JSON
// router-config.json
{
    "updaters": [
    {
      "type" : "stop-time-updater",
      "frequencySec" : 60,
      "backwardsDelayPropagationType" : "REQUIRED_NO_DATA",
      "url" : "http://developer.trimet.org/ws/V1/TripUpdate/appID/0123456789ABCDEF",
      "feedId" : "TriMet"
    }
  ]

}
```

<!-- stop-time-updater END -->


### TripUpdates Websocket GTFS RT

<!-- websocket-gtfs-rt-updater BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                                      |    Type   | Summary                  |  Req./Opt. | Default Value        | Since |
|-----------------------------------------------------------------------|:---------:|--------------------------|:----------:|----------------------|:-----:|
| type = "WEBSOCKET_GTFS_RT_UPDATER"                                    |   `enum`  | The type of the updater. | *Required* |                      |   na  |
| [backwardsDelayPropagationType](#u__7__backwardsDelayPropagationType) |   `enum`  | TODO                     | *Optional* | `"required-no-data"` |   na  |
| feedId                                                                |  `string` | TODO                     | *Optional* |                      |   na  |
| reconnectPeriodSec                                                    | `integer` | TODO                     | *Optional* | `60`                 |   na  |
| url                                                                   |  `string` | TODO                     | *Optional* |                      |   na  |


#### Details

<h4 id="u__7__backwardsDelayPropagationType">backwardsDelayPropagationType</h4>

**Since version:** `na` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"required-no-data"`   
**Path:** /updaters/[7]   
**Enum values:** `required-no-data` | `required` | `always`

TODO



```JSON
// router-config.json
{
    "updaters": [
    {
      "type" : "websocket-gtfs-rt-updater"
    }
  ]

}
```

<!-- websocket-gtfs-rt-updater END -->


### Vehicle Positions

VehiclePositions give the location of some or all vehicles currently in service, in terms of 
geographic coordinates or position relative to their scheduled stops.

<!-- vehicle-positions BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter           |    Type   | Summary                                                                   |  Req./Opt. | Default Value | Since |
|----------------------------|:---------:|---------------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "VEHICLE_POSITIONS" |   `enum`  | The type of the updater.                                                  | *Required* |               |   na  |
| feedId                     |  `string` | Feed ID to which the update should be applied.                            | *Required* |               |  2.2  |
| frequencySec               | `integer` | How often the positions should be updated.                                | *Optional* | `60`          |  2.2  |
| url                        |   `uri`   | The URL of GTFS-RT protobuf HTTP resource to download the positions from. | *Required* |               |  2.2  |


```JSON
// router-config.json
{
    "updaters": [
    {
      "type" : "vehicle-positions",
      "url" : "https://s3.amazonaws.com/kcm-alerts-realtime-prod/vehiclepositions.pb",
      "feedId" : "1",
      "frequencySec" : 60
    }
  ]

}
```

<!-- vehicle-positions END -->


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


<!-- vehicle-rental BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                                                      |       Type      | Summary                                                                         |  Req./Opt. | Default Value | Since |
|---------------------------------------------------------------------------------------|:---------------:|---------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "VEHICLE_RENTAL"                                                               |      `enum`     | The type of the updater.                                                        | *Required* |               |   na  |
| [allowKeepingRentedBicycleAtDestination](#u_1_allowKeepingRentedBicycleAtDestination) |    `boolean`    | If a vehicle should be allowed to be kept at the end of a station-based rental. | *Optional* | `false`       |   na  |
| frequencySec                                                                          |    `integer`    | How often the data should be updated in seconds.                                | *Optional* | `60`          |   na  |
| language                                                                              |     `string`    | TODO                                                                            | *Optional* |               |   na  |
| [network](#u_1_network)                                                               |     `string`    | The name of the network to override the one derived from the source data.       | *Optional* |               |   na  |
| [sourceType](#u_1_sourceType)                                                         |      `enum`     | What source of vehicle rental updater to use.                                   | *Required* |               |   na  |
| url                                                                                   |     `string`    | The URL to download the data from.                                              | *Required* |               |   na  |
| [headers](#u_1_headers)                                                               | `map of string` | HTTP headers to add to the request. Any header key, value can be inserted.      | *Optional* |               |   na  |


#### Details

<h4 id="u_1_allowKeepingRentedBicycleAtDestination">allowKeepingRentedBicycleAtDestination</h4>

**Since version:** `na` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /updaters/[1] 

If a vehicle should be allowed to be kept at the end of a station-based rental.

This behaviour is useful in towns that have only a single rental station. Without it you would need see any results as you would have to always bring it back to the station.

<h4 id="u_1_network">network</h4>

**Since version:** `na` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[1] 

The name of the network to override the one derived from the source data.

GBFS feeds must include a system_id which will be used as the default `network`. These ids are sometimes not helpful so setting this property will override it.

<h4 id="u_1_sourceType">sourceType</h4>

**Since version:** `na` ∙ **Type:** `enum` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[1]   
**Enum values:** `gbfs` | `smoove` | `vilkku`

What source of vehicle rental updater to use.

<h4 id="u_1_headers">headers</h4>

**Since version:** `na` ∙ **Type:** `map of string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[1] 

HTTP headers to add to the request. Any header key, value can be inserted.



```JSON
// router-config.json
{
    "updaters": [
    {
      "type" : "vehicle-rental",
      "network" : "socialbicycles_coast",
      "sourceType" : "gbfs",
      "language" : "en",
      "frequencySec" : 60,
      "allowKeepingRentedBicycleAtDestination" : true,
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

