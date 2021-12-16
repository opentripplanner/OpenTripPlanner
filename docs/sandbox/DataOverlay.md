# Data Overlay

Use grid data in NetCDF format to populate the graph. Also provides custom route endpoint parameters for the data "penalty" and "threshold". This allows route planning to be based on the custom data calculated penalties. Data examples: air qualuty, environmental, and other data types that are tied to certain geographical locations. 


## Contact Info

Developed and maintained by <strong>Metatavu OY</strong>, Finland.

Developers:
<strong>Katja Danilova</strong> - katja.danilova@metatavu.fi
<strong>Simeon Platonov</strong> - simeon.platonov@metatavu.fi
<strong>Daniil Smirnov</strong> - daniil.smirnov@metatavu.fi

In case of any questions please contact any of the people above by emails. We would like to continue developing and improving this feature and would love to hear any ideas from the community.

Company email: info@metatavu.fi

## Changelog
- Initial version (December 2021)


## Documentation

It is a sandbox feature.

Enable the feature by including it to the ```otp-config.json```:

```json
// otp-config.json
{ "otpFeatures": { "DataOverlay" : true } }
```

Please see the configuration part for setup instructions and examples.

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
