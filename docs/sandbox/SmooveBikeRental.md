# HSL Smoove Bike Rental Updater Support - OTP Sandbox Extension

## Contact Info
- Digitransit team, HSL, Helsinki, Finland

## Changelog
- Move this functonality into a sandbox

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
    "frequencySec": 10,
    "allowOverloading": true
}
```

`network` (optional) allows defining custom network id

`allowOverloading` (optional) defines if the stations in the network allow overloading (ignoring available spaces)
