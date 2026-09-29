# Taxi Routing

## Contact Info

- HSL

## Documentation

The taxi module filters and decorates taxi itineraries using spatial provider data
loaded from dedicated GTFS Flex feeds.

For a transit itinerary with a `TAXI` access and/or egress leg:
- Before the transit search runs, each candidate access/egress stop is checked against
  the request's origin/destination coordinate: if **no provider covers both**, the candidate is
  dropped and never reaches the transit search. Access and egress are filtered independently of
  each other.
- Once the transit search has picked a surviving access/egress candidate and the itinerary's legs
  are built, the plain driving leg is decorated with the matched provider's route, agency, and
  booking information. Because the candidate has already passed the pre-search check, a matching
  provider is expected to always be found here.

For a direct (non-transit) `TAXI` itinerary, the same two-phase approach is used:
- Before the street search runs, OTP checks whether the request's origin and destination share a
  common provider; if not, an empty result is returned immediately without running a street
  search.
- Otherwise, once the itinerary has been built, the driving leg is decorated the same way,
  looked up using those same request origin/destination coordinates rather than the leg's own
  local coordinates.

Decoration is only applied when the request's access, egress, or direct mode is `TAXI`, and only
when the feature flag is on and taxi provider data is configured (see Configuration). If the flag
is off or no taxi provider data is configured, `TAXI` requests return no itineraries rather than
falling back to undecorated street routing.

**TODO:**
- Multi-provider support. Currently only the first matching provider is used.
- Calendar/service-date validation?

### Taxi Provider Data Files

Taxi provider data is provided as standard GTFS Flex zip files, configured explicitly in the
`transitFeeds` list in `build-config.json` like any other GTFS feed (with `"type": "gtfs"`), but
with `isTaxiData` set to `true` (this can also be set as a default for all GTFS feeds via
the top-level `gtfsDefaults.isTaxiData`, overridable per-feed). Such feeds are **not** added
to normal transit or flex routing — they are processed exclusively by this module.

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
  "transitFeeds": [
    {
      "type": "gtfs",
      "source": "HSL-gtfs.zip"
    },
    {
      "type": "gtfs",
      "source": "TaxiProvider-gtfs.zip",
      "feedId": "TaxiProvider",
      "isTaxiData": true
    }
  ]
}
```

### GTFS Data Requirements

Each flex trip in the feed must satisfy all of the following, otherwise the trip is skipped with
a warning in the build report:

1. The trip must be an unscheduled (demand-responsive) flex trip. Scheduled-deviated trips are
   not supported.
2. The trip's route must have `route_type` `1500`-`1599` (the GTFS "Taxi Service" family, mapped
   to OTP's `TAXI` transit mode). Trips on any other route type are skipped.
3. At most one trip is kept per route. If multiple trips otherwise satisfy all of these
   requirements for the same route, only the first one is kept and the rest are skipped.
4. No stop may have a meaningful time restriction (`start_pickup_dropoff_window` /
   `end_pickup_dropoff_window`). A full-day window (`0:00:00`–`24:00:00`) is accepted and treated
   as "always available". Any other bounded window causes the trip to be skipped.
5. The trip must have exactly 2 stop times: stop 0 is the pickup stop and stop 1 is the
   drop-off stop.
6. Both stop times must reference the same GTFS Flex area (`location_id`) and that area must
   have a geometry. Trips with separate departure and arrival zones are not supported.
7. Stop 0 must have `pickup_type` `2` (CALL_AGENCY) and stop 1 must have `drop_off_type` `2`
   (CALL_AGENCY). `0` (SCHEDULED) and `3` (COORDINATE_WITH_DRIVER) are not accepted.

### GTFS API Modes

To opt into taxi provider matching, requests must use the `TAXI` mode
(`PlanAccessMode`, `PlanEgressMode`, or `PlanDirectMode`) for the relevant part of the journey.
Internally this maps to the `TAXI` street mode, which behaves identically to
`CAR_PICKUP` for routing purposes but additionally triggers taxi decoration (see above).

### Decorated Leg Fields

A decorated taxi leg is **not** considered a transit leg (`transitLeg`/`isTransit` is `false` in
the API), even though it carries operator and booking information from the matched provider.
Everything else about the leg (geometry, distance, duration, generalized cost, etc.) is the same
as for a plain driving leg.

| Field (GTFS GraphQL / Transmodel)  | Description                                       |
|:------------------------------------|:--------------------------------------------------|
| `agency` / `authority`              | Operator of the matched taxi provider.            |
| `route` / `line`                    | Route of the matched taxi provider.               |
| `pickupBookingInfo` / `bookingArrangements` | Booking info for the pickup.               |
| `dropOffBookingInfo` (GTFS GraphQL only) | Booking info for the drop-off.               |

A `TAXI` request whose origin and destination (for direct routing) or whose logical
access/egress endpoints (for transit routing) don't share a common provider never reaches leg
decoration at all — it is filtered out before routing runs (see above), rather than being
built and then discarded.

### Configuration

Enable the feature flag in `otp-config.json`:

```json
// otp-config.json
{
  "TaxiRouting": true
}
```

## Changelog

### OTP 2.11

- Initial implementation: spatial route index, itinerary filtering, and leg decoration with
  provider information from GTFS Flex data. Taxi provider feeds are configured explicitly in
  `transitFeeds` with `isTaxiData: true`.
- Moved route checking before routing runs (for both transit access/egress and direct routing),
  filtering out non-matching requests instead of discarding built itineraries afterward, and
  decorate using request-level origin/destination coordinates rather than a leg's own local
  coordinates.
