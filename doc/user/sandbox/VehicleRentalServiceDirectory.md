# Vehicle Rental Service Directory API support

This adds support for the GBFS service directory endpoint component
[Lamassu](https://github.com/entur/lamassu). OTP uses the service directory to lookup and connects
to all GBFS endpoints registered in the directory. This simplifies the management of the GBFS
endpoints, since multiple services/components like OTP can connect to the directory and get the
necessary configuration from it.

## Contact Info

- Entur, Norway

## Changelog

- Initial implementation of bike share updater API support
- Make json tag names configurable
  [#3447](https://github.com/opentripplanner/OpenTripPlanner/pull/3447)
- Enable GBFS geofencing with VehicleRentalServiceDirectory
  [#5324](https://github.com/opentripplanner/OpenTripPlanner/pull/5324)
- Enable `allowKeepingVehicleAtDestination`
  [#5944](https://github.com/opentripplanner/OpenTripPlanner/pull/5944)
- Rewrite to use manifest.json from GBFS v3 as the service directory
  [#6900](https://github.com/opentripplanner/OpenTripPlanner/pull/6900)

## Configuration

To enable this you need to specify a url for the `vehicleRentalServiceDirectory` in the
`router-config.json`

### Parameter Summary

<!-- PARAMETERS-TABLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                  |       Type      | Summary                                                                    |  Req./Opt. | Default Value | Since |
|---------------------------------------------------|:---------------:|----------------------------------------------------------------------------|:----------:|---------------|:-----:|
| language                                          |     `string`    | Language code for GBFS feeds.                                              | *Optional* |               |  2.1  |
| [url](#vehicleRentalServiceDirectory_url)         |      `uri`      | URL or file path to the GBFS v3 manifest.json                              | *Required* |               |  2.1  |
| [headers](#vehicleRentalServiceDirectory_headers) | `map of string` | HTTP headers to add to the request. Any header key, value can be inserted. | *Optional* |               |  2.1  |

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


<!-- PARAMETERS-DETAILS END -->

### Example

<!-- JSON-EXAMPLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

```JSON
// router-config.json
{
  "vehicleRentalServiceDirectory" : {
    "url" : "https://example.com/gbfs/v3/manifest.json",
    "language" : "en",
    "headers" : {
      "ET-Client-Name" : "otp"
    }
  }
}
```

<!-- JSON-EXAMPLE END -->
