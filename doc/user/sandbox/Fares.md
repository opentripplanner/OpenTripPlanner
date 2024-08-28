# Fares

## Contact Info

- Leonard Ehrenfried ([mail@leonard.io](mailto:mail@leonard.io))

## Documentation

The code in this sandbox used to be part of OTP core but to allow more experimentation - in 
particular regarding GTFS Fares V2 - it was moved into a sandbox.

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

| class                                      | maintainer                                                 |
|--------------------------------------------|------------------------------------------------------------|
| HighestFareInFreeTransferWindowFareService | IBI Group ([David Emory](mailto:david.emory@ibigroup.com)) |
| AtlantaFareService                         | IBI Group ([David Emory](mailto:david.emory@ibigroup.com)) |
| CombinedInterlinedLegsFareService          | IBI Group ([David Emory](mailto:david.emory@ibigroup.com)) |
| HSLFareServiceImpl                         | HSL ([Viljami Nurminen](mailto:viljami.nurminen@cgi.com))  |
| OrcaFareService                            | IBI Group ([Daniel Heppner](mailto:daniel.heppner@ibigroup.com))|



## Fares configuration

By default OTP will compute fares according to the GTFS specification if fare data is provided in
your GTFS input. It is possible to turn off this by setting the fare to "off". For more complex
scenarios or to handle vehicle rental fares, it is necessary to manually configure fares using the
`fares` section in `build-config.json`. You can combine different fares (for example transit and
vehicle-rental) by defining a `combinationStrategy` parameter, and a list of sub-fares to combine
(all fields starting with `fare` are considered to be sub-fares).

```JSON
// build-config.json
{
  // Select the custom fare "seattle"
  "fares": "seattle"
}
```

Or this alternative form that could allow additional configuration

```JSON
// build-config.json
{
  "fares": {
	"type": "seattle"
  }
}
```

Turning the fare service _off_, this will ignore any fare data in the provided GTFS data.

```JSON
// build-config.json
{
  "fares": "off"
}
```

The current list of custom fare type is:

- `highest-fare-in-free-transfer-window` Will apply the highest observed transit fare (across all
  operators) within a free transfer window, adding to the cost if a trip is boarded outside the free
  transfer window. It accepts the following parameters:
    - `freeTransferWindow` the duration (in ISO8601-ish notation) that free transfers are
      possible after the board time of the first transit leg. Default: `2h30m`.
    - `analyzeInterlinedTransfers` If true, will treat interlined transfers as actual transfers.
      This is merely a work-around for transit agencies that choose to code their fares in a
      route-based fashion instead of a zone-based fashion. Default: `false`
- `atlanta` (no parameters)
- `combine-interlined-legs` Will treat two interlined legs (those with a stay-seated transfer in
  between them) as a single leg for the purpose of fare calculation.
  It has a single parameter `mode` which controls when exactly the combination should happen:
    - `ALWAYS`: All interlined legs are combined. (default)
    - `SAME_ROUTE`: Only interlined legs whose route ID are identical are combined.
- `orca` (no parameters)
- `off` (no parameters)

## Removed fare calculators

The following calculators used to be part of the OTP codebase but since their maintainership
was unclear and no-one [offered to maintain](https://groups.google.com/g/opentripplanner-users/c/ZPzx1lhZ9HU),
they were [removed](https://github.com/opentripplanner/OpenTripPlanner/pull/4273) in July 2022.

- SeattleFareServiceImpl
- DutchFareServiceImpl

The NYC fare calculator was removed in [#4694](https://github.com/opentripplanner/OpenTripPlanner/pull/4694).

The `MultipleFareService` was removed in [#5100](https://github.com/opentripplanner/OpenTripPlanner/pull/5100).

The `SFBayFareServiceImpl` and `TimeBasedVehicleRentalFareService` were removed in [#5145](https://github.com/opentripplanner/OpenTripPlanner/pull/5145).

If you were using these calculators, you're welcome to re-add them to the code base and become their
maintainer.

## Changelog

- Initial move into sandbox [#4241](https://github.com/opentripplanner/OpenTripPlanner/pull/4241)
- Add HighestFareInFreeTransferWindowFareService [#4267](https://github.com/opentripplanner/OpenTripPlanner/pull/4267)
- Add Fares V2 [#4338](https://github.com/opentripplanner/OpenTripPlanner/pull/4338)
- Add CombineInterlinedLegsFareService [#4509](https://github.com/opentripplanner/OpenTripPlanner/pull/4509)
