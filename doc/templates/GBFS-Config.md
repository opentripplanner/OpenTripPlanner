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

Both the [vehicle rental service directory](sandbox/VehicleRentalServiceDirectory.md) and the
`vehicleRentalGeofencing` discover their feeds from a GBFS manifest and need the same per-network
settings. Those are configured once in the `gbfs` section of `otp-config.json`, keyed by the GBFS
`system_id`.

These values are _not_ embedded in the graph, so `otp-config.json` must be present in the deployment
directory when the graph is served as well as when it is built.

`defaults` is applied per field: a listed network overrides only the fields it names and inherits
the rest. `includeUnlistedNetworks` is a separate switch so that adding defaults to avoid repetition
cannot silently widen which networks OTP loads.

`applyGeofencingZones` names when a network's zones are computed and applied, so the two phases are
mutually exclusive and zones cannot be applied twice.

```JSON
// otp-config.json
{
  "gbfs" : {
    "defaults" : {
      "applyGeofencingZones" : "off",
      "requireDropOffInsideBusinessArea" : true,
      "allowKeepingVehicleAtDestination" : false
    },
    "includeUnlistedNetworks" : false,
    "networks" : [
      { "network" : "tier", "applyGeofencingZones" : "permanent" },
      { "network" : "voi", "applyGeofencingZones" : "permanent", "requireDropOffInsideBusinessArea" : false },
      { "network" : "oslobysykkel", "applyGeofencingZones" : "realtime", "allowKeepingVehicleAtDestination" : true },
      { "network" : "noisy-operator" }
    ]
  }
}
```

Given a manifest listing `tier`, `voi`, `oslobysykkel`, `noisy-operator` and `ryde`:

| Network          | Graph build                                      | Runtime                                         |
| ---------------- | ------------------------------------------------ | ----------------------------------------------- |
| `tier`           | zones applied, drop-off required inside the area | updater created, no zone computation            |
| `voi`            | zones applied, no business area enforcement      | updater created, no zone computation            |
| `oslobysykkel`   | skipped                                          | updater computes zones, may keep at destination |
| `noisy-operator` | skipped (inherits `"off"`)                       | updater created, no zones                       |
| `ryde`           | skipped, not listed                              | skipped with a warning                          |

<!-- INSERT: gbfs-networks -->
