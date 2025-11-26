# Carpooling

## Contact Info

- Entur (Norway)
- Eivind Bakke

## Documentation

The carpooling feature enables passengers to join existing driver journeys by being picked up and dropped off along the driver's route. The system finds optimal insertion points for new passengers while respecting capacity constraints, time windows, and route deviation budgets.

### Configuration

The carpooling extension is a sandbox feature that must be enabled in `otp-config.json`:

```json
{
  "otpFeatures": {
    "CarPooling": true
  }
}
```

To enable receiving carpooling data, add the `SiriETCarpoolingUpdater` to your `router-config.json`:

```json
{
  "updaters": [
    {
      "type": "siri-et-carpooling-updater",
      "feedId": "carpooling",
      "url": "https://example.com/siri-et",
      "frequency": "1m",
      "timeout": "15s",
      "requestorRef": "OTP",
      "blockReadinessUntilInitialized": false,
      "fuzzyTripMatching": false,
      "producerMetrics": false
    }
  ]
}
```

#### Configuration Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `feedId` | `string` | | **Required**. The ID of the feed to apply the updates to. |
| `url` | `string` | | **Required**. The URL to send HTTP requests to for SIRI-ET updates. |
| `frequency` | `duration` | `1m` | How often updates should be retrieved. |
| `timeout` | `duration` | `15s` | HTTP timeout for downloading updates. |
| `requestorRef` | `string` | `null` | The requester reference sent in SIRI requests. |
| `blockReadinessUntilInitialized` | `boolean` | `false` | Whether catching up with updates should block readiness check. |
| `fuzzyTripMatching` | `boolean` | `false` | If the fuzzy trip matcher should be used to match trips. |
| `producerMetrics` | `boolean` | `false` | If failure, success, and warning metrics should be collected per producer. |

### SIRI-ET Data Format

The carpooling system uses SIRI-ET (Estimated Timetable) messages to receive real-time updates about carpool trips. The system maps SIRI-ET data as follows:

- `EstimatedVehicleJourneyCode` → Trip ID
- `EstimatedCalls` → Stops on the carpooling trip. The first and the last are origin and destination stops, intermediate ones represent passenger pickup/dropoff

The system supports multi-stop trips where drivers have already accepted multiple passengers.

## Features

### Trip Matching

The carpooling service matches passengers with compatible carpool trips based on several criteria:

- **Availability**: Checks if seats are available in the vehicle
- **Time Compatibility**: Ensures the trip timing works for the passenger
- **Route Alignment**: Validates that pickup and dropoff locations are reasonably close to the driver's route
- **Direction**: Verifies the passenger's travel direction aligns with the trip route

The system automatically calculates the optimal pickup and dropoff points along the driver's route that minimize additional travel time while respecting all constraints.

### Constraints and Protections

To ensure a good experience for all users, the system enforces several constraints:

- **Vehicle Capacity**: Never exceeds the maximum number of seats
- **Route Logic**: Prevents backtracking or illogical detours
- **Existing Passenger Protection**: Limits additional delay to existing passengers (maximum 5 minutes)
- **Driver Deviation Budget**: Respects the driver's maximum acceptable detour time (currently 15 minutes)

### Multi-Stop Trips

The system supports trips where drivers have already accepted multiple passengers. When matching a new passenger to such a trip, the system:
- Considers all existing pickup and dropoff points
- Ensures the vehicle capacity is never exceeded at any point in the trip
- Protects all existing passengers from excessive delays
- Finds the optimal insertion point for the new passenger

### API Integration

Carpooling results are available through the standard OTP GraphQL API. Carpool legs appear as a distinct mode (`CARPOOL`) in multi-modal itineraries, alongside transit, walking, and biking legs.

## Current Limitations

- **Static deviation budget**: We currently assume a 15 minute budget for carpooling
- **Static capacity**: Available seats are static trip properties; no reservation system
- **Basic time windows**: Only simple departure time compatibility; no "arrive by" constraints

## Future Enhancements

### Short Term
- Improved time window handling (including arrive by constraints)
- Add a street mode for carpooling (car_pool) for filtering carpooling searches
- Access/Egress searches for carpooling in order to integrate with transit searches
- Establish an exchange mechanism for deviation budget and occupancy

### Medium Term
- Improved carpool stop representation
- Stable IDs for trips and stops for use in reservation
- Lookup of specific trips and stops in API, not just routing
- Support for multiple providers

### Long Term
- Driver and passenger preference matching (eg. smoker, talker, pets, front/back seat)
- References to scheduled data (eg. areas in NeTEx)
