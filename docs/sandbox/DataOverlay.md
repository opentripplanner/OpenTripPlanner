# Data Overlay
TODO DataOverlay


## Contact Info


## Changelog
- Initial version (December 2021)


## Documentation
TODO


### Configuration 

```json
// otp-config.json
{ "otpFeatures": { "DataOverlay" : true } }
```

```json
// build-config.json
{ "dataOverlay" : {
  "fileName": "graphs/data-file.nc4",
  "latitudeVariable": "lat",
  "longitudeVariable": "lon",
  "timeVariable": "time",
  "timeFormat": "HOURS",
  "indexVariables": [
    {
      "name": "harmfulMicroparticlesPM2_5",
      "displayName": "Harmful micro particles pm 2.5",
      "variable": "cnc_PM2_5"
    },
    {
      "name": "carbonMonoxide",
      "displayName": "Carbon monoxide",
      "variable": "cnc_CO_gas"
    }
  ],
  "requestParameters": [
    {
      "name": "carbon_monoxide",
      "variable": "carbonMonoxide",
      "formula": "(VALUE + 1 - THRESHOLD) * PENALTY"
    }
  ]
}
}
```


TODO - set some meaningful values into the example
```json
// otp-config.json
{ 
  "routingDefaults": {
    "dataOverlay" : {
      "carbon_monoxide_penalty" : 0.2,
      "carbon_monoxide_threshold" : 0.2
    }
  }
}
```
