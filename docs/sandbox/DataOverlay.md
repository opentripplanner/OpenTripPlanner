# Data Overlay

Use grid data in NetCDF format to populate the graph. Also provides custom route endpoint parameters
for the data "penalty" and "threshold". This allows route planning to be based on the custom data
calculated penalties. Data examples: air quality, environmental, and other data types that are tied
to certain geographical locations.

## Contact Info

Developed and maintained by <strong>Metatavu OY</strong>, Finland.

Developers:

<strong>Katja Danilova</strong> - katja.danilova@metatavu.fi\
<strong>Simeon Platonov</strong> - simeon.platonov@metatavu.fi\
<strong>Daniil Smirnov</strong> - daniil.smirnov@metatavu.fi

In case of any questions please contact any of the people above by emails. We would like to continue
developing and improving this feature and would love to hear any ideas from the community.

Company email: info@metatavu.fi

## Changelog

- Initial version (December 2021)

## Documentation

We have been working with OTP since version 1 mainly for producing the Air Quality affected routing
for the city of Helsinki, Finland. That project required us to modify the original OTP quite a lot
so we didn't propose our efforts for the community.

With the OTP2 release we decided to create a dedicated layer on top of OTP2 which not only leaves
the initial structure of the OpenTripPlanner intact, but also brings some additional features for
those, who actually need them. This layer's main feature is populating the graph with a grid data (
i.e air quality, temperature, humidity, pressure, wind speed and direction, and e.t.c). For this to
work two files are required: the actual data file (i.e in NetCDF format) and a .json settings file
which describes the contents of the data file. Please refer to the diagram for more information.

It is a sandbox feature.

Please see the configuration part for setup instructions and examples.

### Configuration

Enable the feature by including it to the ```otp-config.json```:

```json
// otp-config.json
{ "otpFeatures": { "DataOverlay" : true } }
```

Plugin configuration should explain the NetCDF data file and request parameters that use the data
file.

* _fileName_ points to the data file
* _latitudeVariable_, _longitudeVariable_ and _timeVariable_ should be equal to the corresponding
  variable names of the data file
* _timeFormat_ options: MS_EPOCH, SECONDS, HOURS
* _indexVariables_ contain a list of variables of data file that will affect the routing.
    * _name_ can have any value and exists to act as a reference for _requestPatameters_ (see below)
    * _displayName_ is a variable name in human-readable form that should make it more
      understandable
    * _variable_ is the actual name of the variable from data file
* _requestParameters_ contains the list of REST request parameters that affects the cost
  calculation.
    * _name_ should be chosen from the list of enums:
      org.opentripplanner.ext.dataoverlay.api.ParameterName
    * _variable_ should correspond to the _name_ of one of the entries from _indexVariables_ list
      and explain which data field this parameter corresponds to
    * _formula_ should use the keywords VALUE and THRESHOLD and describe the way the penalty is
      calculated. Note: if the result of the formula is negative it is ignored.

Example of build-config.json that includes the dataOverlay plugin configuration:

```json
// build-config.json
{
  "dataOverlay" :
  {
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
        "name": "harmfulMicroparticlesPM10",
        "displayName": "Harmful micro particles pm 10",
        "variable": "cnc_PM10"
      }
    ],
    "requestParameters": [
      {
        "name": "PARTICULATE_MATTER_2_5",
        "variable": "harmfulMicroparticlesPM2_5",
        "formula": "(VALUE + 1 - THRESHOLD) * PENALTY"
      },
      {
        "name": "PARTICULATE_MATTER_10",
        "variable": "harmfulMicroparticlesPM10",
        "formula": "(VALUE + 1 - THRESHOLD) * PENALTY"
      }
    ]
  }

}
```

Default values for Data overlay plugin can also be included in router-config instead of being sent
with each request. If any Data overlay parameters are passed in user query, all the default values
from router-config are ignored.

```json
// router-config.json
{
  "routingDefaults": {
    "dataOverlay" : {
      "particulate_matter_10_threshold" : 100,
      "particulate_matter_10_penalty" : 19
    }
  }
}
```
