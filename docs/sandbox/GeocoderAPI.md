# Geocoder API

## Contact Info

- realCity, Hungary
- Leonard Ehrenfried, [mail@leonard.io](mailto:mail@leonard.io)

## Documentation

This sandbox feature implements geocoding endpoints for a number of use cases.

To enable this you need to add the feature to `otp-config.json`. 

```json
// otp-config.json
{
  "otpFeatures": {
    "SandboxAPIGeocoder": true
  }
}
```

### Endpoints

#### Debug UI

The required geocode API for Stop and From/To searches in the debug client.

Path: `/otp/geocode`

It supports the following URL parameters:

| Parameter      | Description                                                      |
|----------------|------------------------------------------------------------------|
| `query`        | The query string we want to geocode                              |
| `autocomplete` | Whether we should use the query string to do a prefix match      |
| `stops`        | Search for stops, either by name or stop code                    |
| `clusters`     | Search for clusters by their name                                |

#### Stop clusters

A stop cluster is a deduplicated groups of stops. This means that for any stop that has a parent
station only the parent is returned and for stops that have _identical_ names and are very close
to each other, only one is returned.

This is useful for a general purpose fuzzy stop search.

Path: `/otp/geocode/stopClusters`

It supports the following URL parameters:

| Parameter      | Description                                                      |
|----------------|------------------------------------------------------------------|
| `query`        | The query string we want to geocode                              |

## Changelog

- Initial version (June 2021)
- Updated to use Lucene (March 2022)
- Add stop clusters (May 2023)
