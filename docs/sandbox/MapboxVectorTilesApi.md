# Mapbox Vector Tiles API

## Contact Info
- HSL, Finland
- Kyyti Group Oy, Finland
- Hannes Junnila


## Changelog
- 2020-07-09: Initial version of Mapbox vector tiles API
- 2021-05-12: Make expansion factor configurable

## Documentation

This API produces [Mapbox vector tiles](https://docs.mapbox.com/vector-tiles/reference/), which are used by eg. [Digitransit-ui](https://github.com/HSLdevcom/digitransit-ui) to show information about public transit entities on the map.

The tiles can be fetched from `/otp/routers/{routerId}/vectorTiles/{layers}/{z}/{x}/{y}.pbf`, where `layers` is a comma separated list of layer names from the configuration.

### Configuration
To enable this you need to add the feature `SandboxAPIMapboxVectorTilesApi` in `otp-config.json`.

The feature must be configured in `router-config.json` as follows
 
```
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
    {
      "name": "citybikes",
      "type": "BikeRental",
      "mapper": "Digitransit",
      "maxZoom": 20,
      "minZoom": 14,
      "cacheMaxSeconds": 60,
      "expansionFactor": 0.25
    }
  ]
}
```

For each layer, the configuration includes:

 - `name` which is used in the url to fetch tiles, and as the layer name in the vector tiles.
 - `type` which tells the type of the layer. Currently `Stop`, `Station` and `BikeRental` are supported.
 - `mapper` which describes the mapper converting the properties from the OTP model entities to the vector tile properties. Currently `Digitransit` is supported for all layer types.
 - `minZoom` and `maxZoom` which describe the zoom levels the layer is active for.
 - `cacheMaxSeconds` which sets the cache header in the response. The lowest value of the layers included is selected.
 - `expansionFactor` How far outside its boundaries should the tile contain information. The value is a fraction of the tile size. 
    If you are having problem with icons and shapes being clipped at tile edges, then increase this number.

### Extending

If more generic layers are created for this API, it should be moved out from the sandbox, into the core code, with potentially leaving specific property mappers in place.

#### Creating a new layer

In order to create a new type of layer, you need to create a new class extending `LayerBuilder<T>`. 
You need to implement two methods, `List<Geometry> getGeometries(Envelope query)`, which returns a list of geometries, with an object of type `T` as their userData in the geometry, and `double getExpansionFactor()`, which describes how much information outside the tile bounds should be included.
This layer then needs to be added into `VectorTilesResource.layers`, with a new `LayerType` enum as the key, and the class constructor as the value.

A new mapper needs to be added every time a new layer is added. See below for information.

#### Creating a new mapper

The mapping contains information of what data to include in the vector tiles. The mappers are defined per layer.

In order to create a new mapper for a layer, you need to create a new class extending `PropertyMapper<T>`. 
In that class, you need to implement the method `Collection<T2<String, Object>> map(T input)`. 
The type T is dependent on the layer for which you implement the mapper for. 
It needs to return a list of attributes, as key-value pairs which will be written into the vector tile.

The mapper needs to be added to the `mappers` map in the layer, with a new `MapperType` enum as the key, and a function to create the mapper, with a `Graph` object as a parameter, as the value.

