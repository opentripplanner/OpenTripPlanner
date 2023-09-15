# Transmodel GraphQL API

## Contact Info

- Entur, Norway

## Changelog

- Initial version of Transmodel Graph QL API (September 2019)
- Added support for multimodal StopPlaces (November 2019)
- Fix bug querying stopPlaces [#3591](https://github.com/opentripplanner/OpenTripPlanner/pull/3591)
- Fix the field bikesAllowed [#3586](https://github.com/opentripplanner/OpenTripPlanner/pull/3586)
- Add triangle factors for bicycle
  routing [#3585](https://github.com/opentripplanner/OpenTripPlanner/pull/3585)
- Fix correct type for BookingArrangementType#latestBookingDay
- Fix NPE in BookingArrangementType data
  fetchers [#3649](https://github.com/opentripplanner/OpenTripPlanner/pull/3649)
- Add BookingInfo to TimetabledPassingTime and
  EstimatedCall [#3666](https://github.com/opentripplanner/OpenTripPlanner/pull/3666)
- Use correct capitalization for GraphQL
  fields [#3707](https://github.com/opentripplanner/OpenTripPlanner/pull/3707)
- Allow filtering by a list of
  ids [#3738](https://github.com/opentripplanner/OpenTripPlanner/pull/3738)
- Don't filter out stops who don't have multimodal parents in the nearest
  query [#3752](https://github.com/opentripplanner/OpenTripPlanner/pull/3752)
- Restore ability to filter by private
  code [#3764](https://github.com/opentripplanner/OpenTripPlanner/pull/3764)
- Narrow down non-null types
  type [#3803](https://github.com/opentripplanner/OpenTripPlanner/pull/3803)
- Fix issue with fetching parent StopPlaces in nearest query in Transmodel
  API [#3807](https://github.com/opentripplanner/OpenTripPlanner/pull/3807)
- Fix invalid cast in situations resolver for line
  type [#3810](https://github.com/opentripplanner/OpenTripPlanner/pull/3810)
- Deduce enum for bookWhen in Transmodel
  API [#3854](https://github.com/opentripplanner/OpenTripPlanner/pull/3854)
- Fix coercion of default parameter for maximumDistance in
  nearest [#3846](https://github.com/opentripplanner/OpenTripPlanner/pull/3846)
- Expose stopPositionInPattern on
  EstimatedCall [#3846](https://github.com/opentripplanner/OpenTripPlanner/pull/3846)
- Allow selecting first or last quays in a
  ServiceJourney [#3846](https://github.com/opentripplanner/OpenTripPlanner/pull/3846)
- Make language nullable in MultilingualString, as it is not set in NonLocalizedString
  [#4074](https://github.com/opentripplanner/OpenTripPlanner/pull/4074)
- Transmodel API transport mode not present or null is all transport modes
  [#4123](https://github.com/opentripplanner/OpenTripPlanner/pull/4123)
- Expose datedServiceJourney from EstimatedCall 
  [#4128](https://github.com/opentripplanner/OpenTripPlanner/pull/4128)
- Expose stop-to-stop journey pattern geometries
  [#4161](https://github.com/opentripplanner/OpenTripPlanner/pull/4161)
- Add possibility to filter dated service journeys by replacementFor
  [#4198](https://github.com/opentripplanner/OpenTripPlanner/pull/4198)
- Add support for groupOfLines in top level query
  [#4232](https://github.com/opentripplanner/OpenTripPlanner/pull/4232)
- Fix issue when ServiceJourney is created by an updater and expose necessary information via DSJ
  [#4365](https://github.com/opentripplanner/OpenTripPlanner/pull/4365)
- Allow specifying allowed/banned rental networks in trip query
  [#4459](https://github.com/opentripplanner/OpenTripPlanner/pull/4459)
- Add flexible stops
  [#4485](https://github.com/opentripplanner/OpenTripPlanner/pull/4485)

## Documentation

This is the official Entur OTP2 API. The terminology is based on the Transmodel(NeTEx) with some
limitations/simplification. It provides both a routing API (trip query) and index API for transit
data.

Entur provide a [GraphQL explorer](https://api.entur.io/graphql-explorer) where you may browse the
GraphQL schema and try your own queries.

After enabling this feature (see below), the endpoint is available
at: `http://localhost:8080/otp/routers/default/transmodel/index/graphql`

### OTP2 Official GraphQL API (Not available)

We **plan** to make a new offical OTP2 API, replacing the REST API. The plan is to base the new API
on this API and the [GTFS GraphQL API](GtfsGraphQlApi.md). The new API will most likely have two
"translations": A GTFS version and a Transmodel version, we will try to keep the semantics the same.

### Configuration

To enable this you need to add the feature `SandboxAPITransmodelApi`.
 
 
