# Taxi Zone

## Contact Info

- HSL

## Documentation

The taxi zone module filters and decorates taxi itineraries using spatial zone data
loaded from dedicated GTFS Flex feeds.

For each driving-ish leg in a taxi itinerary:
- If **no zone covers both the pickup and drop-off coordinates**, the itinerary is removed from
  the response.
- If **a matching zone is found**, the generic driving leg is replaced with a `TaxiZoneLeg`
  decorated with the provider's route, agency, and booking information from the matched flex trip.
- Decoration is only applied when the request's access, egress, or direct mode is `TAXI`, and only
  when the feature flag is on and a `TaxiZoneService` is configured (see Configuration). It is
  performed by `TaxiRouter` via `TaxiZoneService.decorateAndFilter(...)`, invoked directly by
  `TransitRouter` (for access/egress legs) and by `RoutingWorker.routeDirectTaxi()` →
  `TaxiZoneService.routeDirect(...)` (for direct legs, which internally reuses the same
  taxi-agnostic `DirectStreetRouter` used for all other direct street routing) — there is no
  itinerary filter-chain step involved. If the flag is off or no service is configured, direct
  `TAXI` requests return no itineraries rather than falling back to undecorated street routing.

**TODO:**
- Multi-provider support. Currently only the first matching zone is used.
- Calendar/service-date validation?

### Taxi Zone Data Files

Taxi zone data is provided as standard GTFS Flex zip files, configured explicitly in the
`transitFeeds.gtfsFeeds` list in `build-config.json` like any other GTFS feed, but with
`taxiZoneProvider` set to `true`. Such feeds are **not** added to normal transit or flex
routing — they are processed exclusively by this module.

Example graph directory layout:

```
graph/
  build-config.json
  HSL-gtfs.zip
  TaxiProvider-gtfs.zip
```

```JSON
// build-config.json
{
  "transitFeeds": {
    "gtfsFeeds": [
      {
        "source": "HSL-gtfs.zip"
      },
      {
        "source": "TaxiProvider-gtfs.zip",
        "feedId": "TaxiProvider",
        "taxiZoneProvider": true
      }
    ]
  }
}
```

### GTFS Data Requirements

Each flex trip in the feed must satisfy all of the following, otherwise the trip is skipped with
a warning in the build report:

1. The trip must be an unscheduled (demand-responsive) flex trip. Scheduled-deviated trips are
   not supported.
2. The trip's route must have `route_type` `1500`-`1599` (the GTFS "Taxi Service" family, mapped
   to OTP's `TAXI` transit mode). Trips on any other route type are skipped.
3. No stop may have a meaningful time restriction (`start_pickup_dropoff_window` /
   `end_pickup_dropoff_window`). A full-day window (`0:00:00`–`24:00:00`) is accepted and treated
   as "always available". Any other bounded window causes the trip to be skipped.
4. The trip must have exactly 2 stop times: stop 0 is the pickup stop and stop 1 is the
   drop-off stop.
5. Both stop times must reference the same GTFS Flex area (`location_id`) and that area must
   have a geometry. Trips with separate departure and arrival zones are not supported.
6. Stop 0 must have `pickup_type` `2` (CALL_AGENCY) and stop 1 must have `drop_off_type` `2`
   (CALL_AGENCY). `0` (SCHEDULED) and `3` (COORDINATE_WITH_DRIVER) are not accepted.

### GTFS API Modes

To opt into taxi zone matching, requests must use the `TAXI` mode
(`PlanAccessMode`, `PlanEgressMode`, or `PlanDirectMode`) for the relevant part of the journey.
Internally this maps to the `TAXI` street mode, which behaves identically to
`CAR_PICKUP` for routing purposes but additionally triggers taxi zone decoration (see above).

### Decorated Leg Fields

When a taxi leg matches a zone, it is replaced by a `TaxiZoneLeg`. It implements the plain `Leg`
interface directly (**not** `TransitLeg`), even though it carries route/agency/booking
information from the matched provider's flex trip. This means `transitLeg`/`isTransit` is
`false` in the API for a taxi leg, and the itinerary's own `isTransit`-based fields are
unaffected by it. The physical street route (geometry, distance, elevation, steps, generalized
cost, emissions, etc.) of the original driving leg is preserved by delegating to the wrapped
street leg. Some fields e.g. `mode`, `serviceDate`,
`accessibilityScore`, `fareOffers`, and the vehicle-rental fields are not delegated.

| Field (GTFS GraphQL / Transmodel) | Source                                                       |
|:-----------------------------------|:--------------------------------------------------------------|
| `transitLeg` / n\/a                | Always `false` — `TaxiZoneLeg` is not a `TransitLeg`.          |
| `agency` / `authority`             | Agency from the matched route.                                |
| `route` / `line`                   | Route from the matched flex trip.                              |
| `mode`                              | `TAXI`, resolved via an explicit `instanceof TaxiZoneLeg` branch in `LegImpl`/`LegType`, since it isn't a `TransitLeg`. |
| `serviceDate`                      | Always `null` (not meaningful for a taxi leg). |
| `boardStopPosInPattern`            | Always `0` (the pickup stop).                                  |
| `alightStopPosInPattern`           | Always `1` (the drop-off stop).                                |
| `pickupBookingInfo`                | Booking info from stop 0 of the matched flex trip.             |
| `dropOffBookingInfo`               | Booking info from stop 1 of the matched flex trip.             |
| `accessibilityScore`               | Always `null` (not meaningful for a taxi leg). |
| `fareOffers`                       | Always empty (same as for a plain driving leg). |
| `rentedBike` and related vehicle-rental fields | Always `false`/`null` (not applicable to a taxi leg). |
| `trip`, `tripOnServiceDate`, `alerts`, `stopCalls` | Not applicable — fall back to the `Leg` interface's defaults (`null`/empty), since there is no scheduled trip driving the leg. |

Itineraries where the leg does not match any zone are removed from the response.

### Configuration

Enable the feature flag in `otp-config.json`:

```json
// otp-config.json
{
  "TaxiZone": true
}
```

## Changelog

### OTP 2.11

- Initial implementation: spatial zone index, itinerary filtering, and leg decoration with
  provider information from GTFS Flex data. Taxi zone feeds are configured explicitly in
  `transitFeeds.gtfsFeeds` with `taxiZoneProvider: true`.
