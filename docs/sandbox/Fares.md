# Fares

## Contact Info

- Leonard Ehrenfried ([mail@leonard.io](mailto:mail@leonard.io))

## Changelog

- Initial move into sandbox [#4241](https://github.com/opentripplanner/OpenTripPlanner/pull/4241)
- Add HighestFareInFreeTransferWindowFareService [#4267](https://github.com/opentripplanner/OpenTripPlanner/pull/4267)

## Documentation

The code in this sandbox used to be part of OTP core but to allow more experimentation - in 
particular with regards to GTFS Fares V2 - it was moved into a sandbox.

The main documentation is located at [Build configuration](../BuildConfiguration.md#fares-configuration).

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