# Carpooling Extension Architecture

## Overview

The carpooling extension enables passengers to join existing driver journeys by being picked up and dropped off along the driver's route. The system finds optimal insertion points for new passengers while respecting capacity constraints, time windows, and route deviation budgets.

## Package Structure

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
└── service/                  # Service layer
    ├── CarpoolingService    # Main service interface
    └── DefaultCarpoolingService # Service implementation
```

## Trip Matching Algorithm

The carpooling service uses a multi-phase algorithm to match passengers with compatible carpool trips:

### 1. Filter Phase
Fast pre-screening to eliminate incompatible trips:
- **Capacity Filter**: Checks if any seats are available
- **Time-Based Filter**: Ensures departure time compatibility
- **Distance-Based Filter**: Validates pickup/dropoff are within 50km of driver's route
- **Directional Compatibility Filter**: Verifies passenger direction aligns with trip route

### 2. Routing Phase
Optimal insertion point calculation:
- Uses beeline estimates for early rejection
- Routes baseline segments once and caches results
- Evaluates all viable insertion positions
- Selects position with minimum additional travel time

### 3. Constraint Validation
- **Capacity constraints**: Ensures vehicle capacity is not exceeded
- **Directional constraints**: Prevents backtracking (90° tolerance)
- **Passenger delay constraints**: Protects existing passengers (max 5 minutes additional delay)
- **Deviation budget**: Respects driver's maximum acceptable detour time

## Multi-Stop Support

The system handles trips with multiple existing passengers:
- Each stop tracks passenger count changes (pickups and dropoffs)
- Capacity validation ensures vehicle is never over capacity
- Route optimization considers all existing stops when inserting new passengers
- Passenger delay constraints protect all existing passengers from excessive delays

## Integration Points

### GraphQL API
Carpooling results are integrated into the standard OTP GraphQL API. Carpool legs appear as a distinct leg mode (`CARPOOL`) in multi-modal itineraries, similar to how transit, walking, and biking legs are represented.

### SIRI-ET Updater
The `SiriETCarpoolingUpdater` receives real-time updates about carpool trips via SIRI-ET (Estimated Timetable) messages. The `CarpoolSiriMapper` maps SIRI-ET data to the internal domain model:
- `EstimatedVehicleJourneyCode` → Trip ID
- `EstimatedCalls` → Stops on the carpooling trip

## Design Decisions

### Static Deviation Budget
Currently assumes a 15 minute budget for carpooling. Future versions will support configurable or dynamically negotiated deviation budgets.

### Static Capacity
Available seats are static trip properties. There is no reservation system yet.

### Basic Time Windows
Only simple departure time compatibility is implemented. "Arrive by" constraints are planned for future versions.
