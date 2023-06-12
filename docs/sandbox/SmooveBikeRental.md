# HSL Smoove Bike Rental Updater Support - OTP Sandbox Extension

## Contact Info

- Digitransit team, HSL, Helsinki, Finland

## Changelog

- Move this functonality into a sandbox
- Add allowOverloading through updater config to the stations and isRenting, isReturning and
  capacity from the data to stations (October
  2021, https://github.com/opentripplanner/OpenTripPlanner/pull/3632)
- Rename `allowOverloading` to `overloadingAllowed`

## Documentation

TODO

### Configuration

An example updater configuration:

```
{
    "type": "bike-rental",
    "sourceType": "smoove",
    "network": "smoove-network-1",
    "url": "https://helsinki-fi.smoove.pro/api-public/stations",
    "frequency": 10,
    "overloadingAllowed": true
}
```

`network` (optional) allows defining custom network id

`overloadingAllowed` (optional) defines if the stations in the network allow overloading (ignoring
available spaces)
