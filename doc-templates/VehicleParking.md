# Vehicle Parking Updaters

## Contact Info

- For HSL Park and Ride updater: Digitransit team, HSL, Helsinki, Finland
- For Bikely updater: Leonard Ehrenfried, [mail@leonard.io](mailto:mail@leonard.io)


## Documentation

This sandbox contains vehicle parking updaters. Unlike for some other sandbox features, this is not
enabled/disabled through `otp-config.json` but from `router-config.json` updaters.

Currently contains the following updaters:

- [HSL Park and Ride](https://p.hsl.fi/docs/index.html)
- [ParkAPI](https://github.com/offenesdresden/ParkAPI)
- [Bikely](https://www.safebikely.com/)

### Configuration

These sandboxed vehicle parking updaters can be enabled by editing the `updaters` section in
the `router-config.json` according to the following examples.

All updaters have the following parameters in common:

- `type`: this needs to be `"vehicle-parking"`
- `feedId`: this is used as a "prefix" for park ids, entrance ids and sometimes also for tags.

## HSL Park and Ride

<!-- INSERT: hsl-park -->

## ParkAPI 

<!-- INSERT: park-api -->

## Bikely

<!-- INSERT: bikely -->


## Changelog

- Create initial sandbox implementation (January 2022, [#3796](https://github.com/opentripplanner/OpenTripPlanner/pull/3796))
- Add timeZone parameter to hsl and parkapi updaters (September 2022, [#4427](https://github.com/opentripplanner/OpenTripPlanner/pull/4427))
- Added support for HSL parking hubs (October 2022, [#4510](https://github.com/opentripplanner/OpenTripPlanner/pull/4510))
- Add Bikely updater (November 2022, [#4589](https://github.com/opentripplanner/OpenTripPlanner/pull/4589))
