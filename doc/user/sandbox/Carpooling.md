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

### Trip Matching Algorithm

The carpooling service uses a multi-phase algorithm to match passengers with compatible carpool trips:

1. **Filter Phase** - Fast pre-screening to eliminate incompatible trips:
   - **Capacity Filter**: Checks if any seats are available
   - **Time-Based Filter**: Ensures departure time compatibility
   - **Distance-Based Filter**: Validates pickup/dropoff are within 50km of driver's route
   - **Directional Compatibility Filter**: Verifies passenger direction aligns with trip route

2. **Routing Phase** - Optimal insertion point calculation:
   - Uses beeline estimates for early rejection
   - Routes baseline segments once and caches results
   - Evaluates all viable insertion positions
   - Selects position with minimum additional travel time

3. **Constraint Validation**:
   - **Capacity constraints**: Ensures vehicle capacity is not exceeded
   - **Directional constraints**: Prevents backtracking (90° tolerance)
   - **Passenger delay constraints**: Protects existing passengers (max 5 minutes additional delay)
   - **Deviation budget**: Respects driver's maximum acceptable detour time

### Multi-Stop Support

The system handles trips with multiple existing passengers:
- Each stop tracks passenger count changes (pickups and dropoffs)
- Capacity validation ensures vehicle is never over capacity
- Route optimization considers all existing stops when inserting new passengers
- Passenger delay constraints protect all existing passengers from excessive delays

### Integration with GraphQL API

Carpooling results are integrated into the standard OTP GraphQL API. Carpool legs appear as a distinct leg mode (`CARPOOL`) in multi-modal itineraries, similar to how transit, walking, and biking legs are represented.

## Architecture

### Package Structure

```
org.opentripplanner.ext.carpooling/
├── model/                    # Domain models
│   ├── CarpoolTrip          # Represents a carpool trip offer
│   ├── CarpoolStop          # Intermediate stops with passenger delta
│   └── CarpoolLeg           # Carpool segment in an itinerary
├── routing/                  # Routing and insertion algorithms
│   ├── InsertionEvaluator   # Finds optimal passenger insertion
│   ├── InsertionCandidate   # Represents a viable insertion
│   └── CarpoolStreetRouter  # Street routing for carpooling
├── filter/                   # Trip pre-filtering
│   ├── TripFilter           # Filter interface
│   ├── CapacityFilter       # Checks available capacity
│   ├── TimeBasedFilter      # Time window filtering
│   ├── DistanceBasedFilter  # Geographic distance checks
│   └── DirectionalCompatibilityFilter # Directional alignment
├── constraints/              # Post-routing constraints
│   └── PassengerDelayConstraints # Protects existing passengers
├── util/                     # Utilities
│   ├── BeelineEstimator     # Fast travel time estimates
│   └── DirectionalCalculator # Geographic bearing calculations
├── updater/                  # Real-time updates
│   ├── SiriETCarpoolingUpdater # SIRI-ET integration
│   └── CarpoolSiriMapper    # Maps SIRI to domain model
└── CarpoolingService         # Main service interface
```

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
