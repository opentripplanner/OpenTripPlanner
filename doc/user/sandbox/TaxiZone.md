# Taxi Zone

## Contact Info

- HSL

## Documentation

The taxi zone module filters and decorates taxi itineraries using spatial zone data
loaded from dedicated GTFS Flex feeds.

For each driving-ish leg in a taxi itinerary:
- If **no zone covers both the pickup and drop-off coordinates on the leg's travel date**, the
  itinerary is flagged for deletion and removed from the response. A zone only matches on dates
  within its resolved GTFS service period (a single contiguous date range, trimmed to the
  configured `transitServiceStart`/`transitServiceEnd` window, same as the rest of the transit
  model).
- If **a matching zone is found**, the generic driving leg is replaced with a `TaxiZoneLeg`
  decorated with the provider's route, agency, and booking information from the matched flex trip.
- Decoration is only applied when the request's access, egress, or direct mode is `TAXI`. It is
  performed directly by `TaxiRouter`, invoked from `TransitRouter` (for access/egress legs) and
  `DirectStreetRouter` (for direct legs) — there is no itinerary filter-chain step involved.

**TODO:**
- Multi-provider support. Currently only the first matching zone is used.

### Taxi Zone Data Files

Taxi zone data is provided as standard GTFS Flex zip files. Files are auto-discovered by
filename: a file whose name contains `taxi_zone`, `taxi-zone`, or `taxizone`
(case-insensitive) is classified as `TAXI_ZONE` type and processed exclusively by this
module. Such files are **not** added to normal transit or flex routing.

Every taxi zone file, whether found by auto-discovery or configured explicitly, must have a
corresponding entry in the `taxiZone.feeds` list in `build-config.json`, providing a `feedId` and the
`source` URI. If a taxi zone file is present but has no matching `taxiZone.feeds` entry, graph
building fails. In practice, this means you must always configure `taxiZone.feeds` explicitly,
listing the exact same `source` that would otherwise be auto-discovered, plus its `feedId`.

Example graph directory layout:

```
graph/
  build-config.json
  HSL-gtfs.zip
  TaxiProvider-taxizone.zip           ← auto-discovered as TAXI_ZONE type
```

```JSON
// build-config.json
{
  "taxiZone": {
    "feeds": [
      {
        "feedId": "TaxiProvider",
        "source": "TaxiProvider-taxizone.zip"
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
7. The trip's `service_id` must run on every day within its service period, i.e. its resolved GTFS
   calendar dates (`calendar.txt` / `calendar_dates.txt`) must form one contiguous run of days with no
   gaps. Trips whose service has no valid dates at all, or only a partial/weekday-only calendar,
   are skipped. This lets OTP store each zone's valid period as a single compact date range
   instead of a full set of individual dates.

A zone built from a trip is only used to decorate a leg on dates within its resolved service
period — a request for a date outside that range will not match the zone, the same as any
other GTFS-scheduled service.

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
cost, emissions, fare offers, etc.) of the original driving leg is preserved by delegating to the
wrapped street leg.

| Field (GTFS GraphQL / Transmodel) | Source                                                       |
|:-----------------------------------|:--------------------------------------------------------------|
| `transitLeg` / n\/a                | Always `false` — `TaxiZoneLeg` is not a `TransitLeg`.          |
| `agency` / `authority`             | Agency from the matched route.                                |
| `route` / `line`                   | Route from the matched flex trip.                              |
| `mode`                              | `TransitMode` from the matched route (e.g. `TAXI`), resolved via an explicit `instanceof TaxiZoneLeg` branch in `LegImpl`/`LegType`, since it isn't a `TransitLeg`. |
| `serviceDate`                      | The leg's own start date, validated against the matched zone's resolved GTFS service period. |
| `boardStopPosInPattern`            | Always `0` (the pickup stop).                                  |
| `alightStopPosInPattern`           | Always `1` (the drop-off stop).                                |
| `pickupBookingInfo`                | Booking info from stop 0 of the matched flex trip.             |
| `dropOffBookingInfo`               | Booking info from stop 1 of the matched flex trip.             |
| `trip`, `tripOnServiceDate`, `alerts`, `stopCalls` | Not applicable — fall back to the `Leg` interface's defaults (`null`/empty), since there is no scheduled trip driving the leg. |

Itineraries where the leg does not match any zone are tagged with the system notice
`no-taxi-zone-available` and removed.

### Configuration

Enable the feature flag in `otp-config.json`:

```json
// otp-config.json
{
  "TaxiZone": true
}
```

## Changelog

### OTP 2.10

- Initial implementation: spatial zone index, itinerary filtering, and leg decoration with
  provider information from GTFS Flex data. Taxi zone files are auto-discovered by filename
  pattern, but each discovered file requires a matching `feedId`/`source` entry in the
  `taxiZone.feeds` build-config field.
