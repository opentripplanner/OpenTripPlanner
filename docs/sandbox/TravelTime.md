# Travel Time (Isochrone & Surface) API

## Contact Info

- Entur, Norway

## Changelog

- 2022-05-09 Initial implementation

## Documentation

The API produces a snapshot of travel time form a single place to places around it. The results can
be fetched either as a set of isochrones or a raster map.

### Configuration

The feature must be enabled in otp-config.json as follows:

```JSON
// otp-config.json
{
    "otpFeatures" : {
        "SandboxAPITravelTime" : true
    }
}
```

### API parameters

- `location` Origin of the search, can be either `latitude,longitude` or a stop id
- `time` Departure time as a ISO-8601 time and date. The dafault value is the current time.
- `cutoff` The maximum travel duration as a ISO-8601 duration. The `PT` can be dropped to simplify the value. 
  This parameter can be given multiple times to include multiple isochrones in a single request.
  The default value is one hour.
- `modes` A list of travel modes.
- `arriveBy` Set to `false` when searching from the location and `true` when searching to the 
  location

### Isochrone API

`/otp/traveltime/isochrone`

Results is the travel time boundaries at the `cutoff` travel time.

### Travel time surface API

`/otp/traveltime/surface`

The travel time as a GeoTIFF raster file. The file has a single 32-bit int band, which contains the 
travel time in seconds.