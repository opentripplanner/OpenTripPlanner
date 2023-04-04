# HSL Legacy GraphQL API

## Contact Info

- Digitransit team, HSL, Helsinki, Finland
- Kyyti, Helsinki, Finland

### Configuration

To enable this you need to add the feature `SandboxAPILegacyGraphQLApi`.

```json
// otp-config.json
{
  "otpFeatures" : {
    "SandboxAPILegacyGraphQLApi": true
  }
}
```

## Documentation

This is a copy of HSL's GraphQL API used by the Digitransit project. The API is used to run OTP2
together with the [digitransit-ui](https://github.com/HSLdevcom/digitransit-ui).

The GraphQL endpoints are available at:

- single query: `http://localhost:8080/otp/routers/default/index/graphql`
- batch query: `http://localhost:8080/otp/routers/default/index/graphql/batch`

A complete example that fetches the list of all stops from OTP is:

```
curl --request POST \
  --url http://localhost:8080/otp/routers/default/index/graphql \
  --header 'Content-Type: application/json' \
  --header 'OTPTimeout: 180000' \
  --data '{"query":"query stops {\n  stops {\n    gtfsId\n    name\n  }\n}\n","operationName":"stops"}'
```

## Built-in API client

A browser based GraphQL API client is available at [http://localhost:8080/graphiql](http://localhost:8080/graphiql)

![GraphiQL](../images/graphiql.png)

### OTP2 Official GraphQL API (Not available)

We **plan** to make a new offical OTP2 API, replacing the REST API. The plan is to base the new API
on this API and the [Legacy GraphQL Api](LegacyGraphQLApi.md). The new API will most likely have 2
"translations": A GTFS version and a Transmodel version, we will try to keep the semantics the same.

## Changelog

- Initial version of Legacy Graph QL API (September 2020)
- Added ids parameter to bikeRentalStations query (May 2021, [#3450](https://github.com/opentripplanner/OpenTripPlanner/pull/3450))
- Added capacity and allowOverloading fields to bike rental stations (not yet properly implemented) (May 2021, [#3450](https://github.com/opentripplanner/OpenTripPlanner/pull/3450))
- Updated documentation and process for generating Java code from GraphQL schema definition (May 2021, [#3450](https://github.com/opentripplanner/OpenTripPlanner/pull/3450))
- Implemented modeWeight and added debugItineraryFilter to plan query. Added systemNotices to itineraries (May 2021, [#3503](https://github.com/opentripplanner/OpenTripPlanner/pull/3503))
- Updated to ignore modes which are not valid in OTP2 (June 2021, [#3464](https://github.com/opentripplanner/OpenTripPlanner/pull/3464))
- Add Leg#walkingBike (June 2021, [#3550](https://github.com/opentripplanner/OpenTripPlanner/pull/3550))
- Add GBFS bike rental URIs to bike rental stations (June 2021, [#3543](https://github.com/opentripplanner/OpenTripPlanner/pull/3543))
- Properly implement all bike rental station fields and add allowPickup, allowPickupNow, allowDropoffNow and operative fields (October 2021, [#3632](https://github.com/opentripplanner/OpenTripPlanner/pull/3632))
- Create RentalVehicle, VehicleRentalStation and VehicleRentalUris types. Deprecate BikeRentalStation and BikeRentalStationUris types (October 2021, [#3632](https://github.com/opentripplanner/OpenTripPlanner/pull/3632))
- Create VehicleParking type. Deprecate BikePark and CarPark types (November 2021, [#3480](https://github.com/opentripplanner/OpenTripPlanner/pull/3480))
- Update and implement Alert type and alerts query. Add ACCESSIBILITY_ISSUE to AlertEffectType enum (November 2021, [#3747](https://github.com/opentripplanner/OpenTripPlanner/pull/3747))
- Add geometries for stops (December 2021, [#3757](https://github.com/opentripplanner/OpenTripPlanner/pull/3757))
- Add RouteType and Unknown entities and implement alerts fields (add add alerts field to Feed) (December 2021, [#3780](https://github.com/opentripplanner/OpenTripPlanner/pull/3780))
- Take free-floating vehicles into account when computing state (February 2022, [#3857](https://github.com/opentripplanner/OpenTripPlanner/pull/3857))
- Fix issue with GraphQL code generator (February 2022, [#3881](https://github.com/opentripplanner/OpenTripPlanner/pull/3881))
- Add GBFS form factors for `rentalVehicle` (April 2022, [#4062](https://github.com/opentripplanner/OpenTripPlanner/pull/4062))
- Implement allowedBikeRentalNetworks while deprecating it and add allowedVehicleRentalNetworks and bannedVehicleRentalNetworks. (July 2022, [#4279](https://github.com/opentripplanner/OpenTripPlanner/pull/4279))
- Filters place types in legacy GraphQL API so that a bike park type is not returned if a vehicle parking has no bicycle spaces and car park type is not returned if a parking has no car spaces. (July 2022, [#4296](https://github.com/opentripplanner/OpenTripPlanner/pull/4296))
- Include departures with skipped stops in the Stop type's stopTimesForPattern query. (July 2022, [#4299](https://github.com/opentripplanner/OpenTripPlanner/pull/4299))
- Add built-in GraphQL client. (October 2022, [#4499](https://github.com/opentripplanner/OpenTripPlanner/pull/4499))
- Implement support for omitCanceled parameter in some stop's stoptime queries (October 2022, [#4504]([#4504](https://github.com/opentripplanner/OpenTripPlanner/pull/4504)))
- Rename unpreferredRouteCost to unpreferredCost (October 2022, [#4543](https://github.com/opentripplanner/OpenTripPlanner/pull/4543))
- Make plan fetcher async (December 2022, [#4676](https://github.com/opentripplanner/OpenTripPlanner/pull/4676))
- Fix alerts query severity, effect and cause filters (February 2023, [#4909](https://github.com/opentripplanner/OpenTripPlanner/pull/4909))
- Use accept-language header instead of the default route request locale in the plan query (March 2023, [#4971](https://github.com/opentripplanner/OpenTripPlanner/pull/4971))
