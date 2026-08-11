<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc/templates
   - Generated directory is: /doc/user
-->

OTP can also fetch real-time data about vehicle rental networks including the number of vehicles and
free parking spaces at each station. We support vehicle rental systems that use the
[GBFS](https://github.com/NABSA/gbfs) standard, which can describe a variety of shared mobility
services.

OTP has partial support for both GBFS v1, v2.3 and v3.0
([list of known GBFS feeds](https://github.com/NABSA/gbfs/blob/master/systems.csv)). Furthermore,
support is limited to the following form factors:

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
| language                                                                              |     `string`    | TODO                                                                                                                                                           | *Optional* |               |  2.1  |
| [network](#u_1_network)                                                               |     `string`    | The name of the network to override the one derived from the source data.                                                                                      | *Optional* |               |  1.5  |
| overloadingAllowed                                                                    |    `boolean`    | Allow leaving vehicles at a station even though there are no free slots.                                                                                       | *Optional* | `false`       |  2.2  |
| [sourceType](#u_1_sourceType)                                                         |      `enum`     | What source of vehicle rental updater to use.                                                                                                                  | *Required* |               |  1.5  |
| [startupRetryPeriod](#u_1_startupRetryPeriod)                                         |    `duration`   | How long to retry loading the vehicle rental data source on startup if it initially fails.                                                                     | *Optional* | `"PT0S"`      |  2.10 |
| url                                                                                   |     `string`    | The URL to download the data from.                                                                                                                             | *Required* |               |  1.5  |
| geofencing                                                                            |     `object`    | Configuration for GBFS geofencing-based rental restrictions.                                                                                                   | *Optional* |               |  2.10 |
|    [businessAreaBorders](#u_1_geofencing_businessAreaBorders)                         |    `boolean`    | Infer an operational area from permissive GBFS geofencing zones and enforce drop-off at its boundary.                                                          | *Optional* | `true`        |  2.10 |
|    [enabled](#u_1_geofencing_enabled)                                                 |    `boolean`    | Compute rental restrictions based on GBFS 2.2 geofencing zones.                                                                                                | *Optional* | `false`       |  2.10 |
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


<h4 id="u_1_network">network</h4>

**Since version:** `1.5` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[1] 

The name of the network to override the one derived from the source data.

GBFS feeds must include a system_id which will be used as the default `network`. These ids are sometimes not helpful so setting this property will override it.

<h4 id="u_1_sourceType">sourceType</h4>

**Since version:** `1.5` ∙ **Type:** `enum` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[1]   
**Enum values:** `gbfs`

What source of vehicle rental updater to use.

<h4 id="u_1_startupRetryPeriod">startupRetryPeriod</h4>

**Since version:** `2.10` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT0S"`   
**Path:** /updaters/[1] 

How long to retry loading the vehicle rental data source on startup if it initially fails.

The first time the data source is loaded, OTP will retry for this duration every
5 seconds before giving up. This is useful to handle temporary network failures during
OTP startup. Set to `PT0S` to disable retries.


<h4 id="u_1_geofencing_businessAreaBorders">businessAreaBorders</h4>

**Since version:** `2.10` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `true`   
**Path:** /updaters/[1]/geofencing 

Infer an operational area from permissive GBFS geofencing zones and enforce drop-off at its boundary.

When enabled, GBFS geofencing zones that have no restrictions (no traversal or drop-off bans)
are treated as business areas. The router will force a vehicle drop-off when exiting such an
area, preventing routes that leave the operator's service area with a rented vehicle.

Requires `enabled` to also be true.


<h4 id="u_1_geofencing_enabled">enabled</h4>

**Since version:** `2.10` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /updaters/[1]/geofencing 

Compute rental restrictions based on GBFS 2.2 geofencing zones.

This feature is somewhat experimental and therefore turned off by default for the following reasons:

- It delays start up of OTP. How long is dependent on the complexity of the zones. For example in Oslo it takes 6 seconds to compute while Portland takes 25 seconds.
- It's easy for a malformed or unintended geofencing zone to make routing impossible. If you encounter such a case, please file a bug report.


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
      "geofencing" : {
        "enabled" : false
      },
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

## Shared network configuration

The [vehicle rental service directory](sandbox/VehicleRentalServiceDirectory.md) discovers its feeds
from a GBFS manifest and takes its per-network settings from the `gbfs` section of
`otp-config.json`, keyed by the GBFS `system_id`.

This section lives in `otp-config.json` because it is the only configuration file read both when the
graph is built and when it is served. Note that it is _not_ embedded in the graph, so it must be
present in the deployment directory in both phases.

`defaults` is applied per field: a listed network overrides only the fields it names and inherits
the rest. `includeUnlistedNetworks` is a separate switch so that adding defaults to avoid repetition
cannot silently widen which networks OTP loads.

```JSON
// otp-config.json
{
  "gbfs" : {
    "defaults" : {
      "geofencingZones" : "off",
      "requireDropOffInsideBusinessArea" : true,
      "allowKeepingVehicleAtDestination" : false
    },
    "includeUnlistedNetworks" : true,
    "networks" : [
      { "network" : "oslobysykkel", "geofencingZones" : "realtime", "allowKeepingVehicleAtDestination" : true }
    ]
  }
}
```

<!-- gbfs-networks BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                                                            |    Type    | Summary                                                                        |  Req./Opt. | Default Value | Since |
|---------------------------------------------------------------------------------------------|:----------:|--------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| [includeUnlistedNetworks](#gbfs_includeUnlistedNetworks)                                    |  `boolean` | Whether networks in the GBFS manifest but absent from `networks` are loaded.   | *Optional* | `false`       |  2.10 |
| [defaults](#gbfs_defaults)                                                                  |  `object`  | Values applied to every network that does not set them itself.                 | *Optional* |               |  2.10 |
|    [allowKeepingVehicleAtDestination](#gbfs_defaults_allowKeepingVehicleAtDestination)      |  `boolean` | Whether a vehicle rented from a station may be kept at the destination.        | *Optional* | `false`       |  2.10 |
|    [geofencingZones](#gbfs_defaults_geofencingZones)                                        |   `enum`   | Which phase computes and applies this network's geofencing zones.              | *Optional* | `"off"`       |  2.10 |
|    [requireDropOffInsideBusinessArea](#gbfs_defaults_requireDropOffInsideBusinessArea)      |  `boolean` | Whether a rented vehicle must be dropped off before leaving the business area. | *Optional* | `true`        |  2.10 |
| [networks](#gbfs_networks)                                                                  | `object[]` | Per-network overrides, keyed by the GBFS `system_id`.                          | *Optional* |               |  2.10 |
|       [allowKeepingVehicleAtDestination](#gbfs_networks_0_allowKeepingVehicleAtDestination) |  `boolean` | Whether a vehicle rented from a station may be kept at the destination.        | *Optional* | `false`       |  2.10 |
|       [geofencingZones](#gbfs_networks_0_geofencingZones)                                   |   `enum`   | Which phase computes and applies this network's geofencing zones.              | *Optional* | `"off"`       |  2.10 |
|       network                                                                               |  `string`  | The GBFS `system_id` of the network these values apply to.                     | *Required* |               |  2.10 |
|       [requireDropOffInsideBusinessArea](#gbfs_networks_0_requireDropOffInsideBusinessArea) |  `boolean` | Whether a rented vehicle must be dropped off before leaving the business area. | *Optional* | `true`        |  2.10 |


##### Parameter details

<h4 id="gbfs_includeUnlistedNetworks">includeUnlistedNetworks</h4>

**Since version:** `2.10` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /gbfs 

Whether networks in the GBFS manifest but absent from `networks` are loaded.

When `false` such a network is skipped with a warning, so `networks` acts as a whitelist.
When `true` it is loaded with `defaults` applied.


<h4 id="gbfs_defaults">defaults</h4>

**Since version:** `2.10` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /gbfs 

Values applied to every network that does not set them itself.

A network listed in `networks` overrides only the fields it names and inherits the rest
from here. Setting defaults does not by itself widen which networks are loaded - see
`includeUnlistedNetworks`.


<h4 id="gbfs_defaults_allowKeepingVehicleAtDestination">allowKeepingVehicleAtDestination</h4>

**Since version:** `2.10` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /gbfs/defaults 

Whether a vehicle rented from a station may be kept at the destination.

When disabled a vehicle rented from a station must be returned to another station, so an
itinerary can only end with the vehicle parked at one.


<h4 id="gbfs_defaults_geofencingZones">geofencingZones</h4>

**Since version:** `2.10` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"off"`   
**Path:** /gbfs/defaults   
**Enum values:** `realtime` | `off`

Which phase computes and applies this network's geofencing zones.

- `realtime` - the vehicle rental updater loads and applies the zones.
- `off` - the zones are not processed.


<h4 id="gbfs_defaults_requireDropOffInsideBusinessArea">requireDropOffInsideBusinessArea</h4>

**Since version:** `2.10` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `true`   
**Path:** /gbfs/defaults 

Whether a rented vehicle must be dropped off before leaving the business area.

A business area is inferred from geofencing zones whose ride and traversal rules are all
permissive. When enabled, the router forces a drop-off at the border of that area,
preventing itineraries that leave the operator's service area with a rented vehicle.

Has no effect when `geofencingZones` is `off`.


<h4 id="gbfs_networks">networks</h4>

**Since version:** `2.10` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** /gbfs 

Per-network overrides, keyed by the GBFS `system_id`.

<h4 id="gbfs_networks_0_allowKeepingVehicleAtDestination">allowKeepingVehicleAtDestination</h4>

**Since version:** `2.10` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /gbfs/networks/[0] 

Whether a vehicle rented from a station may be kept at the destination.

When disabled a vehicle rented from a station must be returned to another station, so an
itinerary can only end with the vehicle parked at one.


<h4 id="gbfs_networks_0_geofencingZones">geofencingZones</h4>

**Since version:** `2.10` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"off"`   
**Path:** /gbfs/networks/[0]   
**Enum values:** `realtime` | `off`

Which phase computes and applies this network's geofencing zones.

- `realtime` - the vehicle rental updater loads and applies the zones.
- `off` - the zones are not processed.


<h4 id="gbfs_networks_0_requireDropOffInsideBusinessArea">requireDropOffInsideBusinessArea</h4>

**Since version:** `2.10` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `true`   
**Path:** /gbfs/networks/[0] 

Whether a rented vehicle must be dropped off before leaving the business area.

A business area is inferred from geofencing zones whose ride and traversal rules are all
permissive. When enabled, the router forces a drop-off at the border of that area,
preventing itineraries that leave the operator's service area with a rented vehicle.

Has no effect when `geofencingZones` is `off`.





<!-- gbfs-networks END -->
