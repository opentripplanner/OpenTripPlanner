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
`vehicleRentalGraphBuilder` discover their feeds from a GBFS manifest and need the same per-network
settings. Those are configured once in the `gbfs` section of `otp-config.json`, keyed by the GBFS
`system_id`.

This section lives in `otp-config.json` because it is the only configuration file read both when the
graph is built and when it is served. Note that it is _not_ embedded in the graph, so it must be
present in the deployment directory in both phases.

`defaults` is applied per field: a listed network overrides only the fields it names and inherits
the rest. `includeUnlistedNetworks` is a separate switch so that adding defaults to avoid repetition
cannot silently widen which networks OTP loads.

`geofencingZones` names the phase that computes and applies a network's zones, so the two phases are
mutually exclusive and zones cannot be applied twice.

```JSON
// otp-config.json
{
  "gbfs" : {
    "defaults" : {
      "geofencingZones" : "off",
      "requireDropOffInsideBusinessArea" : true,
      "allowKeepingVehicleAtDestination" : false
    },
    "includeUnlistedNetworks" : false,
    "networks" : [
      { "network" : "tier", "geofencingZones" : "permanent" },
      { "network" : "voi", "geofencingZones" : "permanent", "requireDropOffInsideBusinessArea" : false },
      { "network" : "oslobysykkel", "geofencingZones" : "realtime", "allowKeepingVehicleAtDestination" : true },
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
