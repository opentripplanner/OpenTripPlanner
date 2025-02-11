# Vehicle Parking Updaters

## Contact Info

- For Liipi updater: Digitransit team, HSL, Helsinki, Finland
- For Bikely, Bikeep and SIRI-FM updater: Leonard Ehrenfried, [mail@leonard.io](mailto:mail@leonard.io)


## Documentation

This sandbox contains vehicle parking updaters. Unlike for some other sandbox features, this is not
enabled/disabled through `otp-config.json` but from `router-config.json` updaters.

Currently contains the following updaters:

- [Liipi](https://parking.fintraffic.fi/docs/index.html)
- [ParkAPI](https://github.com/offenesdresden/ParkAPI)
- [Bikely](https://www.safebikely.com/)
- SIRI-FM

### Configuration

These sandboxed vehicle parking updaters can be enabled by editing the `updaters` section in
the `router-config.json` according to the following examples.

All updaters have the following parameters in common:

- `type`: this needs to be `"vehicle-parking"`
- `feedId`: this is used as a "prefix" for park ids, entrance ids and sometimes also for tags.

## Liipi

<!-- INSERT: liipi -->

## ParkAPI 

<!-- INSERT: park-api -->

## Bikely

<!-- INSERT: bikely -->

## Bikeep

<!-- INSERT: bikeep -->

## SIRI-FM

The SIRI-FM updater works slightly differently from the others in that it only updates the availability
of parking but does not create new lots in realtime.

The data source must conform to the [Italian SIRI-FM](https://github.com/5Tsrl/siri-italian-profile) profile
which requires SIRI 2.1.

<!-- INSERT: siri-fm -->

## Changelog

- Create initial sandbox implementation (January 2022, [#3796](https://github.com/opentripplanner/OpenTripPlanner/pull/3796))
- Add timeZone parameter to Liipi updater (September 2022, [#4427](https://github.com/opentripplanner/OpenTripPlanner/pull/4427))
- Added support for Liipi parking hubs (October 2022, [#4510](https://github.com/opentripplanner/OpenTripPlanner/pull/4510))
- Add Bikely updater (November 2022, [#4589](https://github.com/opentripplanner/OpenTripPlanner/pull/4589))
