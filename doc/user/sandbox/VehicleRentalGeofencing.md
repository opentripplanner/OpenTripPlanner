# Vehicle Rental Geofencing

Loads [GBFS geofencing zones](https://github.com/MobilityData/gbfs/blob/master/gbfs.md#geofencing_zonesjson)
into the graph during the graph build, rather than through the runtime updater.

The zones of a large operator take a noticeable time to apply, because every zone boundary has to be
matched against the street edges it crosses. Doing that during the build means the work happens once
and is stored in the graph, instead of on every startup and on every subsequent update of an
unchanged feed.

Vehicles and stations remain real-time data, so a
[vehicle rental updater](../GBFS-Config.md) is still required. Only the zones move to the build.

## Contact Info

- Entur, Norway

## Configuration

There is no feature flag: the sandbox is activated by the presence of `vehicleRentalGeofencing` in
`build-config.json`, matching
[Vehicle Rental Service Directory](VehicleRentalServiceDirectory.md).

```JSON
// build-config.json
{
  "vehicleRentalGeofencing" : {
    "url" : "https://example.com/gbfs/v3/manifest.json",
    "headers" : {
      "ET-Client-Name" : "otp"
    }
  }
}
```

The parameters are documented in full under
[`vehicleRentalGeofencing`](../BuildConfiguration.md#vehicleRentalGeofencing).

### Selecting the networks

The networks to load are discovered from the GBFS v3 `manifest.json`. A dataset is loaded when both
of these hold:

- the shared [`gbfs` configuration](../GBFS-Config.md) puts it in the permanent phase, with
  `"applyGeofencingZones": "permanent"`, and
- its feed actually publishes a `geofencing_zones` feed, checked against the feed list in
  `gbfs.json` before anything is fetched.

The second rule means a blanket `permanent` default in `defaults` picks up only the systems that
have zones, so operators without them cost nothing but the feed list lookup.

`applyGeofencingZones` is what decides which phase applies a network's zones, and the phases are
mutually exclusive — a network set to `permanent` is skipped by the updater, and one set to
`realtime` is skipped here, so zones are never applied twice.

## Limitations

A GBFS updater configured directly under `updaters` in `router-config.json` does not read the shared
`gbfs` section. Enabling `geofencing.enabled` on such an updater for a network that is also built
here applies the zones twice.

Because the zones become part of the graph, changing an operator's zones requires a new graph build.
Networks whose zones change often should stay in the `realtime` phase.

## Changelog

- Initial implementation
  [#7887](https://github.com/opentripplanner/OpenTripPlanner/pull/7887)
