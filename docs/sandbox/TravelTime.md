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
- `time` Departure time as a ISO-8601 time and date (example `2023-04-24T15:40:12+02:00`). The default value is the current time.
- `cutoff` The maximum travel duration as a ISO-8601 duration. The `PT` can be dropped to simplify the value. 
  This parameter can be given multiple times to include multiple isochrones in a single request.
  The default value is one hour.
- `modes` A list of travel modes. WALK is not implemented, use `WALK, TRANSIT` instead.
- `arriveBy` Set to `false` when searching from the location and `true` when searching to the 
  location

### Isochrone API

`/otp/traveltime/isochrone`

Results is the travel time boundaries at the `cutoff` travel time.

### Travel time surface API

`/otp/traveltime/surface`

The travel time as a GeoTIFF raster file. The file has a single 32-bit int band, which contains the 
travel time in seconds.

### Example Request

```
http://localhost:8080/otp/traveltime/isochrone?batch=true&location=52.499959,13.388803&time=2023-04-12T10:19:03%2B02:00&modes=WALK,TRANSIT&arriveBy=false&cutoff=30M17S
```
