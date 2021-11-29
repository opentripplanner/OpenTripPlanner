# Flexible transit routing

## Contact Info

- Kyyti Group Oy, Finland
- Entur, Norway
- Hannes Junnila


## Changelog

### OTP 2.1
- Initial implementation of Flexible transit routing
- Use one-to-many search in order to make the performance of the StreetFlexPathCalculator acceptable. (April 2021)
- Also link transit stops used by flex trips to the closest car traversable edge. This allows flex street routing all the way to the stop. (April 2021)
- Fix performance issues with the StreetFlexPathCalculator [#3460](https://github.com/opentripplanner/OpenTripPlanner/pull/3460)
- Improve performance of flex access/egress routing [#3661](https://github.com/opentripplanner/OpenTripPlanner/pull/3661)
- Allow getting on and off at the same flex stop time [#3720](https://github.com/opentripplanner/OpenTripPlanner/pull/3720)
- Calculate fare for flex routes [#3743](https://github.com/opentripplanner/OpenTripPlanner/pull/3743)

## Documentation
To enable this turn on `FlexRouting` as a feature in `otp-config.json`. 

The GTFS feeds should conform to the [GTFS-Flex v2.1 draft](https://github.com/MobilityData/gtfs-flex/blob/master/spec/reference.md)

## Configuration

This features allows a limited number of config options (currently just one). To change the
configuration, add the following to `router-config.json`.

```
{
  "flex": {
    "maxTransferDurationSeconds": 300
  }
}
```

*Config parameters*

### `maxTransferDurationSeconds`

Default: 300

How long should a passenger be allowed to walk after getting out of a flex vehicle and transferring 
to a flex or transit one. 

This was mainly introduced to improve performance which is also the reason for not using the existing 
value with the same name: fixed schedule transfers are computed during the graph build but flex 
ones are calculated at request time and are more sensitive to slowdown.

A lower value means that the routing is faster.
