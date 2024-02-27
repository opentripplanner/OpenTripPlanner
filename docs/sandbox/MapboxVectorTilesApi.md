# Mapbox Vector Tiles API

## Contact Info

- HSL, Finland
- Arcadis, US

## Documentation

This API produces [Mapbox vector tiles](https://docs.mapbox.com/vector-tiles/reference/), which are
used by [Digitransit-ui](https://github.com/HSLdevcom/digitransit-ui) and 
[`otp-react-redux`](https://github.com/opentripplanner/otp-react-redux) to show information about
public transit entities on the map.

The tiles can be fetched from `/otp/routers/{routerId}/vectorTiles/{layers}/{z}/{x}/{y}.pbf`,
where `layers` is a comma separated list of layer names from the configuration.

Maplibre/Mapbox GL JS also requires a tilejson.json endpoint which is available at
`/otp/routers/{routerId}/vectorTiles/{layers}/tilejson.json`.

Translatable fields in the tiles are translated based on the `accept-language` header in requests.
Currently, only the language with the highest priority from the header is used.

### Configuration

To enable this you need to add the feature `otp-config.json`.

```json
// otp-config.json
{
  "otpFeatures": {
    "SandboxAPIMapboxVectorTilesApi": true
  }
}
```

The feature must be configured in `router-config.json` as follows

```JSON
{
  "vectorTiles": {
    "basePath": "/only/configure/if/required",
    "layers": [
      {
        "name": "stops",
        "type": "Stop",
        "mapper": "Digitransit",
        "maxZoom": 20,
        "minZoom": 14,
        "cacheMaxSeconds": 600
      },
      // flex zones
      {
        "name": "areaStops",
        "type": "AreaStop",
        "mapper": "OTPRR",
        "maxZoom": 20,
        "minZoom": 14,
        "cacheMaxSeconds": 600
      },
      {
        "name": "stations",
        "type": "Station",
        "mapper": "Digitransit",
        "maxZoom": 20,
        "minZoom": 12,
        "cacheMaxSeconds": 600
      },
      // all rental places: stations and free-floating vehicles
      {
        "name": "citybikes",
        "type": "VehicleRental",
        "mapper": "Digitransit",
        "maxZoom": 20,
        "minZoom": 14,
        "cacheMaxSeconds": 60,
        "expansionFactor": 0.25
      },
      // just free-floating vehicles
      {
        "name": "rentalVehicles",
        "type": "VehicleRentalVehicle",
        "mapper": "DigitransitRealtime",
        "maxZoom": 20,
        "minZoom": 14,
        "cacheMaxSeconds": 60
      },
      // just rental stations
      {
        "name": "rentalStations",
        "type": "VehicleRentalStation",
        "mapper": "Digitransit",
        "maxZoom": 20,
        "minZoom": 14,
        "cacheMaxSeconds": 600
      },
      // Contains just stations and real-time information for them
      {
        "name": "realtimeRentalStations",
        "type": "VehicleRentalStation",
        "mapper": "DigitransitRealtime",
        "maxZoom": 20,
        "minZoom": 14,
        "cacheMaxSeconds": 60
      },
      // This exists for backwards compatibility. At some point, we might want
      // to add a new real-time parking mapper with better translation support
      // and less unnecessary fields.
      {
        "name": "stadtnaviVehicleParking",
        "type": "VehicleParking",
        "mapper": "Stadtnavi",
        "maxZoom": 20,
        "minZoom": 14,
        "cacheMaxSeconds": 60,
        "expansionFactor": 0.25
      },
      // no real-time, translatable fields are translated based on accept-language header
      // and contains less fields than the Stadtnavi mapper
      {
        "name": "vehicleParking",
        "type": "VehicleParking",
        "mapper": "Digitransit",
        "maxZoom": 20,
        "minZoom": 14,
        "cacheMaxSeconds": 600,
        "expansionFactor": 0.25
      },
      {
        "name": "vehicleParkingGroups",
        "type": "VehicleParkingGroup",
        "mapper": "Digitransit",
        "maxZoom": 17,
        "minZoom": 14,
        "cacheMaxSeconds": 600,
        "expansionFactor": 0.25
      }
    ] 
  }
}
```

For each layer, the configuration includes:

- `name` which is used in the url to fetch tiles, and as the layer name in the vector tiles.
- `type` which tells the type of the layer. Currently supported:
    - `Stop`
    - `AreaStop`: Flex zones
    - `Station`
    - `VehicleRental`: all rental places: stations and free-floating vehicles
    - `VehicleRentalVehicle`: free-floating rental vehicles
    - `VehicleRentalStation`: rental stations
    - `VehicleParking`
    - `VehicleParkingGroup`

<!-- parameters BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                               |    Type    | Summary                                                                                    |  Req./Opt. | Default Value | Since |
|----------------------------------------------------------------|:----------:|--------------------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| [attribution](#vectorTiles_attribution)                        |  `string`  | Custom attribution to be returned in `tilejson.json`                                       | *Optional* |               |  2.5  |
| [basePath](#vectorTiles_basePath)                              |  `string`  | The path of the vector tile source URLs in `tilejson.json`.                                | *Optional* |               |  2.5  |
| [layers](#vectorTiles_layers)                                  | `object[]` | Configuration of the individual layers for the Mapbox vector tiles.                        | *Optional* |               |  2.0  |
|       type = "stop"                                            |   `enum`   | Type of the layer.                                                                         | *Required* |               |  2.0  |
|       [cacheMaxSeconds](#vectorTiles_layers_0_cacheMaxSeconds) |  `integer` | Sets the cache header in the response.                                                     | *Optional* | `-1`          |  2.0  |
|       [expansionFactor](#vectorTiles_layers_0_expansionFactor) |  `double`  | How far outside its boundaries should the tile contain information.                        | *Optional* | `0.25`        |  2.0  |
|       [mapper](#vectorTiles_layers_0_mapper)                   |  `string`  | Describes the mapper converting from the OTP model entities to the vector tile properties. | *Required* |               |  2.0  |
|       maxZoom                                                  |  `integer` | Maximum zoom levels the layer is active for.                                               | *Optional* | `20`          |  2.0  |
|       minZoom                                                  |  `integer` | Minimum zoom levels the layer is active for.                                               | *Optional* | `9`           |  2.0  |
|       name                                                     |  `string`  | Used in the url to fetch tiles, and as the layer name in the vector tiles.                 | *Required* |               |  2.0  |


#### Details

<h4 id="vectorTiles_attribution">attribution</h4>

**Since version:** `2.5` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /vectorTiles 

Custom attribution to be returned in `tilejson.json`

By default the, `attribution` property in `tilejson.json` is computed from the names and
URLs of the feed publishers.
If the OTP deployment contains many feeds, this can become very unwieldy.

This configuration parameter allows you to set the `attribution` to any string you wish
including HTML tags,
for example `<a href='https://trimet.org/mod'>Regional Partners</a>`.


<h4 id="vectorTiles_basePath">basePath</h4>

**Since version:** `2.5` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /vectorTiles 

The path of the vector tile source URLs in `tilejson.json`.

This is useful if you have a proxy setup and rewrite the path that is passed to OTP.

If you don't configure this optional value then the path returned in `tilejson.json` is in
the format `/otp/routers/default/vectorTiles/layer1,layer2/{z}/{x}/{x}.pbf`.
If you, for example, set a value of `/otp_test/tiles` then the returned path changes to
`/otp_test/tiles/layer1,layer2/{z}/{x}/{x}.pbf`.

The protocol and host are always read from the incoming HTTP request. If you run OTP behind
a proxy then make sure to set the headers `X-Forwarded-Proto` and `X-Forwarded-Host` to make OTP
return the protocol and host for the original request and not the proxied one.

**Note:** This does _not_ change the path that OTP itself serves the tiles or `tilejson.json`
responses but simply changes the URLs listed in `tilejson.json`. The rewriting of the path
is expected to be handled by a proxy.


<h4 id="vectorTiles_layers">layers</h4>

**Since version:** `2.0` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** /vectorTiles 

Configuration of the individual layers for the Mapbox vector tiles.

<h4 id="vectorTiles_layers_0_cacheMaxSeconds">cacheMaxSeconds</h4>

**Since version:** `2.0` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `-1`   
**Path:** /vectorTiles/layers/[0] 

Sets the cache header in the response.

The lowest value of the layers included is selected.

<h4 id="vectorTiles_layers_0_expansionFactor">expansionFactor</h4>

**Since version:** `2.0` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.25`   
**Path:** /vectorTiles/layers/[0] 

How far outside its boundaries should the tile contain information.

The value is a fraction of the tile size. If you are having problem with icons and shapes being clipped at tile edges, then increase this number.

<h4 id="vectorTiles_layers_0_mapper">mapper</h4>

**Since version:** `2.0` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /vectorTiles/layers/[0] 

Describes the mapper converting from the OTP model entities to the vector tile properties.

Currently `Digitransit` is supported for all layer types.




<!-- parameters END -->

### Extending

If more generic layers are created for this API, the code should be moved out from the sandbox, into 
the core, perhaps potentially leaving specific property mappers in place.

#### Creating a new layer

In order to create a new type of layer, you need to create a new class extending `LayerBuilder<T>`.
You need to implement two methods, `List<Geometry> getGeometries(Envelope query)`, which returns a
list of geometries, with an object of type `T` as their userData in the geometry,
and `double getExpansionFactor()`, which describes how much information outside the tile bounds
should be included. This layer then needs to be added into `VectorTilesResource.layers`, with a
new `LayerType` enum as the key, and the class constructor as the value.

A new mapper needs to be added every time a new layer is added. See below for information.

#### Creating a new mapper

The mapping contains information of what data to include in the vector tiles. The mappers are
defined per layer.

In order to create a new mapper for a layer, you need to create a new class
extending `PropertyMapper<T>`. In that class, you need to implement the
method `Collection<KeyValue<String, Object>> map(T input)`. The type T is dependent on the layer for which
you implement the mapper for. It needs to return a list of attributes, as key-value pairs which will
be written into the vector tile.

The mapper needs to be added to the `mappers` map in the layer, with a new `MapperType` enum as the
key, and a function to create the mapper, with a `Graph` object as a parameter, as the value.

## Changelog

- 2020-07-09: Initial version of Mapbox vector tiles API
- 2021-05-12: Make expansion factor configurable
- 2021-09-07: Rename `BikeRental` to `VehicleRental`
- 2021-10-13: Correctly serialize the vehicle rental name [#3648](https://github.com/opentripplanner/OpenTripPlanner/pull/3648)
- 2022-01-03: Add support for VehicleParking entities
- 2022-04-27: Read the headsign for frequency-only patterns correctly [#4122](https://github.com/opentripplanner/OpenTripPlanner/pull/4122)
- 2022-08-23: Remove patterns and add route gtfsTypes to stop layer [#4404](https://github.com/opentripplanner/OpenTripPlanner/pull/4404)
- 2022-10-11: Added layer for VehicleParkingGroups [#4510](https://github.com/opentripplanner/OpenTripPlanner/pull/4510)
- 2022-10-14: Add separate layers for vehicle rental place types [#4516](https://github.com/opentripplanner/OpenTripPlanner/pull/4516)
- 2022-10-19 [#4529](https://github.com/opentripplanner/OpenTripPlanner/pull/4529):
  * Translatable fields are now translated based on accept-language header
  * Added DigitransitRealtime for vehicle rental stations
  * Changed old vehicle parking mapper to be Stadtnavi
  * Added a new Digitransit vehicle parking mapper with no real-time information and less fields
- 2024-01-22: Make `basePath` configurable [#5627](https://github.com/opentripplanner/OpenTripPlanner/pull/5627)
- 2024-02-27: Add layer for flex zones [#5704](https://github.com/opentripplanner/OpenTripPlanner/pull/5704)
