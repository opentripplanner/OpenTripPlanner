# Mapbox Vector Tiles API

## Contact Info

- HSL, Finland
- Kyyti Group Oy, Finland
- Hannes Junnila

## Documentation

This API produces [Mapbox vector tiles](https://docs.mapbox.com/vector-tiles/reference/), which are
used by eg. [Digitransit-ui](https://github.com/HSLdevcom/digitransit-ui) to show information about
public transit entities on the map.

The tiles can be fetched from `/otp/routers/{routerId}/vectorTiles/{layers}/{z}/{x}/{y}.pbf`,
where `layers` is a comma separated list of layer names from the configuration.

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
  "vectorTileLayers": [
    {
      "name": "stops",
      "type": "Stop",
      "mapper": "Digitransit",
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
    // Contains just stations and realtime information for them
    {
      "name": "realtimeRentalStations",
      "type": "VehicleRentalStation",
      "mapper": "DigitransitRealtime",
      "maxZoom": 20,
      "minZoom": 14,
      "cacheMaxSeconds": 60
    },
    // This exists for backwards compatibility. At some point, we might want
    // to add a new realtime parking mapper with better translation support
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
    // no realtime, translatable fields are translated based on accept-language header
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
```

For each layer, the configuration includes:

- `name` which is used in the url to fetch tiles, and as the layer name in the vector tiles.
- `type` which tells the type of the layer. Currently supported:
    - `Stop`
    - `Station`
    - `VehicleRental`: all rental places: stations and free-floating vehicles
    - `VehicleRentalVehicle`: free-floating rental vehicles
    - `VehicleRentalStation`: rental stations
    - `VehicleParking`
    - `VehicleParkingGroup`
- `mapper` which describes the mapper converting the properties from the OTP model entities to the
  vector tile properties. Currently `Digitransit` is supported for all layer types.
- `minZoom` and `maxZoom` which describe the zoom levels the layer is active for.
- `cacheMaxSeconds` which sets the cache header in the response. The lowest value of the layers
  included is selected.
- `expansionFactor` How far outside its boundaries should the tile contain information. The value is
  a fraction of the tile size. If you are having problem with icons and shapes being clipped at tile
  edges, then increase this number.

### Extending

If more generic layers are created for this API, it should be moved out from the sandbox, into the
core code, with potentially leaving specific property mappers in place.

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
method `Collection<T2<String, Object>> map(T input)`. The type T is dependent on the layer for which
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
  * Added a new Digitransit vehicle parking mapper with no realtime information and less fields
