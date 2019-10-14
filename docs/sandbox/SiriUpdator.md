# Statistics API - OTP Sandbox Extension Example

## Contact Info
- Lasse Tyrihjell, Entur, Norway


## Changelog

### October 2019 (in progress)

- Initial version of SIRI updator


## Documentation

TODO 
 
### Configuration
To enable the SIRI updator you need to add it to the updators section of the `router-config.json`.

```
{
    "type": "siri-updater",
    "frequencySec": 60,
    "url": "https://api.updater.com/example-updater"
}
```
