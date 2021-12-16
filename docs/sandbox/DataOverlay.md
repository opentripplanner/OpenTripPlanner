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

We have been working with OTP since version 1 mainly for producing the Air Quality affected routing for the city of Helsinki, Finland. That project required us to modify the original OTP quite a lot so we didn't propose our efforts for the community.

With the OTP2 release we decided to create a dedicated layer on top of OTP2 which not only leaves the initial structure of the Open Trip Planner intact, but also brings some additional features for those, who actually need them. This layer's main feature is populating the graph with a grid data (i.e air quality, temperature, humidity, pressure, wind speed and direction, and e.t.c). For this to work two files are required: the actual data file (i.e in NetCDF format) and a .json settings file which describes the contents of the data file. Please refer to the diagram for more information.

It is a sandbox feature.

Please see the configuration part for setup instructions and examples.

### Configuration 

Enable the feature by including it to the ```otp-config.json```:

```json
// otp-config.json
{ "otpFeatures": { "DataOverlay" : true } }
```

TODO - Katja, please provide other instructions here

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
