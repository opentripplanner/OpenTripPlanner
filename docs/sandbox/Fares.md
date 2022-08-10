# Fares

## Contact Info

- Leonard Ehrenfried ([mail@leonard.io](mailto:mail@leonard.io))

## Changelog

- Initial move into sandbox [#4241](https://github.com/opentripplanner/OpenTripPlanner/pull/4241)
- Add HighestFareInFreeTransferWindowFareService [#4267](https://github.com/opentripplanner/OpenTripPlanner/pull/4267)
- Add Fares V2 [#4338](https://github.com/opentripplanner/OpenTripPlanner/pull/4338)

## Documentation

The code in this sandbox used to be part of OTP core but to allow more experimentation - in 
particular regarding GTFS Fares V2 - it was moved into a sandbox.

The main documentation is located at [Build configuration](../BuildConfiguration.md#fares-configuration).

## Fares V2

In 2022 the GTFS spec was extended to contain a powerful new model, called Fares V2, to describe fares, prices and
products for public transport tickets. A baseline effort was [merged into the main
spec](https://github.com/google/transit/pull/286) in May.

OTP experimentally supports the merged baseline plus a few extensions from the [larger, unmerged spec](http://bit.ly/gtfs-fares).

To enable Fares V2 support, add the following to `otp-config.json`:

```json
{
  "otpFeatures" : {
    "FaresV2" : true
  }
}
```

### Supported Fares V2 fields

A full list of the fields that OTP supports is available in the [Fares V2 Adoption Google Sheet](https://docs.google.com/spreadsheets/d/1jpKjz6MbCD2XPhmIP11EDi-P2jMh7x2k-oHS-pLf2vI).

## Custom fare calculators

When the GTFS Fares V1 spec was not enough, some organizations have developed their own calculators
which are also part of the sandbox code.

The classes and their maintainers are as follows:

| class                                                          | maintainer                                                |
|----------------------------------------------------------------|-----------------------------------------------------------|
| HighestFareInFreeTransferWindowFareServiceDutchFareServiceImpl | IBI Group ([David Emory](mailto:david.emory@ibigroup.com) |
| NycFareServiceImpl                                             | unmaintained                                              |
| SFBayFareServiceImpl                                           | unmaintained                                              |

## Removed fare calculators

The following calculators used to be part of the OTP codebase but since their maintainership
was unclear and no-one [offered to maintain](https://groups.google.com/g/opentripplanner-users/c/ZPzx1lhZ9HU), 
they were [removed](https://github.com/opentripplanner/OpenTripPlanner/pull/4273) in July 2022.

- SeattleFareServiceImpl
- DutchFareServiceImpl

If you were using these calculators, you're welcome to re-add them to the code base.