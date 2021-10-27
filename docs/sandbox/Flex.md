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

## Documentation
To enable this turn on `FlexRouting` as a feature in `otp-config.json`. The GTFS feeds should conform to the [GTFS-Flex v2.1 draft](https://docs.google.com/document/d/1PyYK6JVzz52XEx3FXqAJmoVefHFqZTHS4Mpn20dTuKE/)

## Configuration

This features allows a limited number of config options (currently just one). To change the
configuration, add the following to `router-config.json`.

```
{
  "flex": {
    "maxTransferMeters": 300
  }
}
```

*Config parameters*

- `maxTransferMeters`: how far should you a passenger be able to walk after getting out of a flex
                       vehicle and transferring to another one. 
                       This was mainly introduced to improve performance - a lower value means that 
                       the routing is faster.
