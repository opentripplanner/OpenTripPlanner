# Geocoder API

## Contact Info

- realCity, Hungary

## Changelog

- Initial version (June 2021)
- Updated to use Lucene (March 2022)

## Documentation

This adds the required `geocode` API required for Stop and From/To searches in the debug client
using Lucene to index and search.

To enable this you need to add the feature `SandboxAPIGeocoder` in `otp-config.json`.

The API endpoint is available at `/otp/routers/{routerId}/geocode`, and supports the following query
string parameters:

| Parameter      | Description                                                      |
|----------------|------------------------------------------------------------------|
| `query`        | The query string we want to geocode                              |
| `autocomplete` | Whether we should use the query string to do a prefix match      |
| `stops`        | Search for stops, either by name or stop code                    |
| `clusters`     | Search for clusters by their name                                |
| `corners`      | Search for street corners using at least one of the street names |
