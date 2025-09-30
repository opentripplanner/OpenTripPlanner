# Vehicle Rental Service Directory API support

This adds support for the GBFS service directory endpoint component
[Lamassu](https://github.com/entur/lamassu). 
OTP uses the service directory to lookup and connects to all GBFS endpoints registered in the 
directory. This simplifies the management of the GBFS endpoints, since multiple services/components 
like OTP can connect to the directory and get the necessary configuration from it.


## Contact Info

- Entur, Norway


## Changelog

- Initial implementation of bike share updater API support
- Make json tag names configurable [#3447](https://github.com/opentripplanner/OpenTripPlanner/pull/3447)
- Enable GBFS geofencing with VehicleRentalServiceDirectory [#5324](https://github.com/opentripplanner/OpenTripPlanner/pull/5324)
- Enable `allowKeepingVehicleAtDestination` [#5944](https://github.com/opentripplanner/OpenTripPlanner/pull/5944)
- Rewrite to use manifest.json from GBFS v3 as the service directory [#6900](https://github.com/opentripplanner/OpenTripPlanner/pull/6900)

## Configuration

To enable this you need to specify a url for the `vehicleRentalServiceDirectory` in
the `router-config.json`

### Parameter Summary

<!-- PARAMETERS-TABLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                                                                                     |       Type      | Summary                                                                         |  Req./Opt. | Default Value | Since |
|----------------------------------------------------------------------------------------------------------------------|:---------------:|---------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| language                                                                                                             |     `string`    | Language code for GBFS feeds.                                                   | *Optional* |               |  2.1  |
| [url](#vehicleRentalServiceDirectory_url)                                                                            |      `uri`      | URL or file path to the GBFS v3 manifest.json                                   | *Required* |               |  2.1  |
| [headers](#vehicleRentalServiceDirectory_headers)                                                                    | `map of string` | HTTP headers to add to the request. Any header key, value can be inserted.      | *Optional* |               |  2.1  |
| [networks](#vehicleRentalServiceDirectory_networks)                                                                  |    `object[]`   | List all networks to include. Use "network": "default-network" to set defaults. | *Optional* |               |  2.4  |
|       [allowKeepingVehicleAtDestination](#vehicleRentalServiceDirectory_networks_0_allowKeepingVehicleAtDestination) |    `boolean`    | Enables `allowKeepingVehicleAtDestination` for the given network.               | *Optional* | `false`       |  2.5  |
|       [geofencingZones](#vehicleRentalServiceDirectory_networks_0_geofencingZones)                                   |    `boolean`    | Enables geofencingZones for the given network                                   | *Optional* | `false`       |  2.4  |
|       network                                                                                                        |     `string`    | The network name                                                                | *Required* |               |  2.4  |

<!-- PARAMETERS-TABLE END -->


### Parameter Details

<!-- PARAMETERS-DETAILS BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

<h4 id="vehicleRentalServiceDirectory_url">url</h4>

**Since version:** `2.1` ∙ **Type:** `uri` ∙ **Cardinality:** `Required`   
**Path:** /vehicleRentalServiceDirectory 

URL or file path to the GBFS v3 manifest.json

Can be either a remote URL (http/https) or a local file path (file://). The manifest must conform to the GBFS v3.0 specification.

<h4 id="vehicleRentalServiceDirectory_headers">headers</h4>

**Since version:** `2.1` ∙ **Type:** `map of string` ∙ **Cardinality:** `Optional`   
**Path:** /vehicleRentalServiceDirectory 

HTTP headers to add to the request. Any header key, value can be inserted.

<h4 id="vehicleRentalServiceDirectory_networks">networks</h4>

**Since version:** `2.4` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** /vehicleRentalServiceDirectory 

List all networks to include. Use "network": "default-network" to set defaults.

If no default network exists only the listed networks are used. Configure a network with
name "default-network" to include all unlisted networks. If not present, all unlisted
networks are dropped. Note! The values in the "default-network" are not used to set
missing field values in networks listed.


<h4 id="vehicleRentalServiceDirectory_networks_0_allowKeepingVehicleAtDestination">allowKeepingVehicleAtDestination</h4>

**Since version:** `2.5` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /vehicleRentalServiceDirectory/networks/[0] 

Enables `allowKeepingVehicleAtDestination` for the given network.

Configures if a vehicle rented from a station must be returned to another one or can
be kept at the end of the trip.

See the regular [GBFS documentation](../GBFS-Config.md) for more information.


<h4 id="vehicleRentalServiceDirectory_networks_0_geofencingZones">geofencingZones</h4>

**Since version:** `2.4` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /vehicleRentalServiceDirectory/networks/[0] 

Enables geofencingZones for the given network

See the regular [GBFS documentation](../GBFS-Config.md) for more information.


<!-- PARAMETERS-DETAILS END -->


### Example

<!-- JSON-EXAMPLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

```JSON
// router-config.json
{
  "vehicleRentalServiceDirectory" : {
    "url" : "https://example.com/gbfs/v3/manifest.json",
    "headers" : {
      "ET-Client-Name" : "otp"
    },
    "networks" : [
      {
        "network" : "oslo-by-sykkel",
        "geofencingZones" : true
      }
    ]
  }
}
```

<!-- JSON-EXAMPLE END -->
