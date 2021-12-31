# Vehicle Parking Updaters - OTP Sandbox Extension

## Contact Info
- For HSL Park and Ride updater: Digitransit team, HSL, Helsinki, Finland

## Changelog
- Create initial sandbox implementation (January 2022, https://github.com/opentripplanner/OpenTripPlanner/pull/3796)

## Documentation
This sandbox contains vehicle parking updaters. Unlike for some other sandbox features,
this is not enabled/disabled through `otp-config.json` but from `router-config.json` updaters.

Currently contains the following updaters:
- HSL Park and Ride (https://p.hsl.fi/docs/index.html)
- ParkAPI (https://github.com/offenesdresden/ParkAPI)
- KML (Keyhole Markup language) placemark parks. Use name as bike park name and point coordinates.

### Configuration
These sandboxed vehicle parking updaters can be enabled by editing the `updaters` section
in the `router-config.json` according to the following examples.

All updaters have the following parameters in common:
- `type`: this needs to be `"vehicle-parking"`
- `feedId`: this is used as a "prefix" for park ids, entrance ids and sometimes also for tags.

<b>To use HSL park updater:</b>
```
{
    "type": "vehicle-parking",
    "sourceType": "hsl-park",
    "feedId": "hslpark",
    "facilitiesFrequencySec": 3600,
    "facilitiesUrl": "https://p.hsl.fi/api/v1/facilities.json?limit=-1",
    "utilizationsFrequencySec": 600,
    "utilizationsUrl": "https://p.hsl.fi/api/v1/utilizations.json?limit=-1"
}
```
- `sourceType`: needs to be `"hsl-park"`
- `facilitiesUrl`: URL that contains the basic information for the parks
- `facilitiesFrequencySec`: how often should the basic information for parks be refetched.
Should be more than `utilizationsFrequencySec` and if it's <= 0, parks are only fetched once. Default `600`.
- `utilizationsUrl`: URL that contains realtime updates to parks
- `utilizationsFrequencySec`: how often should the basic information for parks be refetched.
Should be less than `facilitiesFrequencySec` and if it's < 0,
realtime information is never refetched. Default `3600`.

<b>To use KML park updater:</b>
```
{
    "type": "vehicle-parking",
    "sourceType": "kml",
    "feedId": "kml",
    "frequencySec": 600,
    "url": "https://foo.bar",
    "namePrefix": "foo",
    "zip": true
}
```
- `sourceType`: needs to be `"kml"`
- `url`: URL that contains the park data in KML format
- `frequencySec`: how often park data is refetched. Default `60`.
- `namePrefix`: Adds this prefix to park names
- `zip`: Tells if the data is zipped or not.

<b>To use ParkAPI updater:</b>
```
{
    "type": "vehicle-parking",
    "sourceType": "park-api",
    "feedId": "parkapi",
    "frequencySec": 600,
    "url": "https://foo.bar",
    "headers": {"Cache-Control": "max-age=604800"},
    "tags": ["source:parkapi"]
}
```
- `sourceType`: needs to be `"park-api"` if car parks are fetched, `"bicycle-park-api"` if bicycle parks.
- `url`: URL that contains the park data in KML format
- `frequencySec`: how often park data is refetched. Default `60`.
- `headers`: Use these headers for requests
- `tags`: Add these tags to all parks.

