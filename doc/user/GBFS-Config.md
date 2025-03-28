<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc/templates
   - Generated directory is: /doc/user 
-->

OTP can also fetch real-time data about vehicle rental networks
including the number of vehicles and free parking spaces at each station. We support vehicle rental
systems that use the [GBFS](https://github.com/NABSA/gbfs) standard, which can describe a variety of 
shared mobility services.

OTP has partial support for both GBFS v1 and v2.2 ([list of known GBFS feeds](https://github.com/NABSA/gbfs/blob/master/systems.csv)). 
Furthermore, support is limited to the following form factors:

- bicycle
- scooter
- car

<!-- vehicle-rental BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                                                      |       Type      | Summary                                                                                                                                                        |  Req./Opt. | Default Value | Since |
|---------------------------------------------------------------------------------------|:---------------:|----------------------------------------------------------------------------------------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "vehicle-rental"                                                               |      `enum`     | The type of the updater.                                                                                                                                       | *Required* |               |  1.5  |
| [allowKeepingRentedVehicleAtDestination](#u_1_allowKeepingRentedVehicleAtDestination) |    `boolean`    | If a vehicle should be allowed to be kept at the end of a station-based rental.                                                                                | *Optional* | `false`       |  2.1  |
| frequency                                                                             |    `duration`   | How often the data should be updated.                                                                                                                          | *Optional* | `"PT1M"`      |  1.5  |
| [geofencingZones](#u_1_geofencingZones)                                               |    `boolean`    | Compute rental restrictions based on GBFS 2.2 geofencing zones.                                                                                                | *Optional* | `false`       |  2.3  |
| language                                                                              |     `string`    | TODO                                                                                                                                                           | *Optional* |               |  2.1  |
| [network](#u_1_network)                                                               |     `string`    | The name of the network to override the one derived from the source data.                                                                                      | *Optional* |               |  1.5  |
| overloadingAllowed                                                                    |    `boolean`    | Allow leaving vehicles at a station even though there are no free slots.                                                                                       | *Optional* | `false`       |  2.2  |
| [sourceType](#u_1_sourceType)                                                         |      `enum`     | What source of vehicle rental updater to use.                                                                                                                  | *Required* |               |  1.5  |
| url                                                                                   |     `string`    | The URL to download the data from.                                                                                                                             | *Required* |               |  1.5  |
| [headers](#u_1_headers)                                                               | `map of string` | HTTP headers to add to the request. Any header key, value can be inserted.                                                                                     | *Optional* |               |  1.5  |
| [rentalPickupTypes](#u_1_rentalPickupTypes)                                           |    `enum set`   | This is temporary and will be removed in a future version of OTP. Use this to specify the type of rental data that is allowed to be read from the data source. | *Optional* |               |  2.7  |


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

<h4 id="u_1_rentalPickupTypes">rentalPickupTypes</h4>

**Since version:** `2.7` ∙ **Type:** `enum set` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[1]   
**Enum values:** `station` | `free-floating`

This is temporary and will be removed in a future version of OTP. Use this to specify the type of rental data that is allowed to be read from the data source.

 - `station` Stations are imported.
 - `free-floating` Free-floating vehicles are imported.




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
