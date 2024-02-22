# Flexible transit routing

## Contact Info

- Kyyti Group Oy, Finland
- Entur, Norway
- Hannes Junnila

## Documentation

To enable this turn on `FlexRouting` as a feature in `otp-config.json`.

The GTFS feeds should conform to the 
[GTFS-Flex v2 draft PR](https://github.com/google/transit/pull/388)

## Configuration

This feature allows a limited number of config options. To change the configuration, add the 
following to `router-config.json`.

<!-- INSERT: config -->

## Changelog

### OTP 2.1

- Initial implementation of Flexible transit routing
- Use one-to-many search in order to make the performance of the StreetFlexPathCalculator
  acceptable. (April 2021)
- Also link transit stops used by flex trips to the closest car traversable edge. This allows flex
  street routing all the way to the stop. (April 2021)
- Fix performance issues with the
  StreetFlexPathCalculator [#3460](https://github.com/opentripplanner/OpenTripPlanner/pull/3460)
- Improve performance of flex access/egress
  routing [#3661](https://github.com/opentripplanner/OpenTripPlanner/pull/3661)
- Allow getting on and off at the same flex stop
  time [#3720](https://github.com/opentripplanner/OpenTripPlanner/pull/3720)
- Calculate fare for flex
  routes [#3743](https://github.com/opentripplanner/OpenTripPlanner/pull/3743)

### OTP 2.3
- Enable configuration of `maxFlexTripDuration` and change of type of `maxTransferDuration`
  routes [#4642](https://github.com/opentripplanner/OpenTripPlanner/pull/4642)
