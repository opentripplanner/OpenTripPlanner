<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc/templates
   - Generated directory is: /doc/user
-->

OTP can also fetch real-time data about vehicle rental networks including the number of vehicles and
free parking spaces at each station. We support vehicle rental systems that use the
[GBFS](https://github.com/NABSA/gbfs) standard, which can describe a variety of shared mobility
services.

OTP has partial support for both GBFS v1, v2.3 and v3.0
([list of known GBFS feeds](https://github.com/NABSA/gbfs/blob/master/systems.csv)). Furthermore,
support is limited to the following form factors:

- bicycle
- scooter
- car

<!-- INSERT: vehicle-rental -->

## Shared network configuration

The [vehicle rental service directory](sandbox/VehicleRentalServiceDirectory.md) discovers its feeds
from a GBFS manifest and takes its per-network settings from the `gbfs` section of
`otp-config.json`, keyed by the GBFS `system_id`.

This section lives in `otp-config.json` because it is the only configuration file read both when the
graph is built and when it is served. Note that it is _not_ embedded in the graph, so it must be
present in the deployment directory in both phases.

`defaults` is applied per field: a listed network overrides only the fields it names and inherits
the rest. `includeUnlistedNetworks` is a separate switch so that adding defaults to avoid repetition
cannot silently widen which networks OTP loads.

```JSON
// otp-config.json
{
  "gbfs" : {
    "defaults" : {
      "geofencingZones" : "off",
      "requireDropOffInsideBusinessArea" : true,
      "allowKeepingVehicleAtDestination" : false
    },
    "includeUnlistedNetworks" : true,
    "networks" : [
      { "network" : "oslobysykkel", "geofencingZones" : "realtime", "allowKeepingVehicleAtDestination" : true }
    ]
  }
}
```

<!-- INSERT: gbfs-networks -->
