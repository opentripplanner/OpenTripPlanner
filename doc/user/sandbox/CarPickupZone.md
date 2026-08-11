# Car Pickup Zone

## Contact Info

- HSL

## Documentation

The car pickup zone module filters and decorates car pickup itineraries using spatial zone data
loaded from dedicated GTFS Flex feeds.

For each driving-ish leg in a car pickup itinerary:
- If **no zone covers both the pickup and drop-off coordinates**, the itinerary is flagged for
  deletion and removed from the response.
- If **a matching zone is found**, the generic driving leg is replaced with a `CarPickupZoneLeg`
  decorated with the provider's route, agency, and booking information from the matched flex trip.
- The filter is only enabled when the request's access or egress mode is `CAR_PICKUP` (see
  `RouteRequestToFilterChainMapper`).

**TODO:**
- Multi-provider support. Currently only the first matching zone is used.
- Possibly add CAR_PICKUP direct mode to GTFS API for enabling the car pickup zone decorator with only direct mode.

### Car Pickup Zone Data Files

Car pickup zone data is provided as standard GTFS Flex zip files. Files are auto-discovered by
filename: a file whose name contains `car_pickup_zone`, `car-pickup-zone`, or `carpickupzone`
(case-insensitive) is classified as `CAR_PICKUP_ZONE` type and processed exclusively by this
module. Such files are **not** added to normal transit or flex routing.

Example graph directory layout:

```
graph/
  build-config.json
  HSL-gtfs.zip
  Taxi-carpickupzone.zip   ← auto-discovered as CAR_PICKUP_ZONE type
```

### GTFS Data Requirements

Each flex trip in the feed must satisfy all of the following, otherwise the trip is skipped with
a warning in the build report:

1. The trip must be an unscheduled (demand-responsive) flex trip. Scheduled-deviated trips are
   not supported.
2. No stop may have a meaningful time restriction (`start_pickup_dropoff_window` /
   `end_pickup_dropoff_window`). A full-day window (`0:00:00`–`24:00:00`) is accepted and treated
   as "always available". Any other bounded window causes the trip to be skipped.
3. The trip must have exactly 2 stop times: stop 0 is the pickup stop and stop 1 is the
   drop-off stop.
4. Both stop times must reference the same GTFS Flex area (`location_id`) and that area must
   have a geometry. Trips with separate departure and arrival zones are not supported.
5. Stop 0 must have `pickup_type` `2` (CALL_AGENCY) or `3` (COORDINATE_WITH_DRIVER), and stop 1
   must have `drop_off_type` `2` or `3`. `0` (SCHEDULED) is not accepted.

### Decorated Leg Fields

When a car pickup leg matches a zone, it is replaced by a `CarPickupZoneLeg`
with the following fields from the matched provider:

| Field                | Source                                                    |
|:---------------------|:----------------------------------------------------------|
| `agency`             | Agency from the matched route.                            |
| `route`              | Route from the matched flex trip.                         |
| `mode`               | `TransitMode` from the matched route (e.g. `TAXI`).       |
| `pickupBookingInfo`  | Booking info from stop 0 of the matched flex trip.        |
| `dropOffBookingInfo` | Booking info from stop 1 of the matched flex trip.        |

Itineraries where the leg does not match any zone are tagged with the system notice
`no-car-pickup-zone-available` and removed.

### Configuration

Enable the feature flag in `otp-config.json`:

```json
// otp-config.json
{
  "CarPickupZone": true
}
```

## Changelog

### OTP 2.10

- Initial implementation: spatial zone index, itinerary filtering, and leg decoration with
  provider information from GTFS Flex data. Auto-discovery by filename pattern.
