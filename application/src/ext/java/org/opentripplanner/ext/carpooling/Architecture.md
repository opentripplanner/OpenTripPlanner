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
├── filter/                   # Pre- and post-screening
│   ├── CarpoolingRequest    # Passenger-side request abstraction
│   ├── CarpoolTripFilter    # Pre-filter interface (raw trips)
│   ├── CarpoolItineraryFilter # Post-filter interface (routed itineraries)
│   ├── TripPreFilters       # Composite of pre-filters (AND)
│   ├── ItineraryPostFilters # Composite of post-filters (AND)
│   ├── TimeTripFilter       # Pre-filter: loose time-window check
│   ├── TimeItineraryFilter  # Post-filter: tight time-window enforcement
│   └── DistanceTripFilter   # Pre-filter: geographic proximity
├── constraints/              # Post-routing constraints
│   └── PassengerDelayConstraints # Protects existing passengers
├── util/                     # Utilities
│   └── BeelineEstimator     # Fast travel time estimates
├── updater/                  # Real-time updates
│   ├── SiriETCarpoolingUpdater # SIRI-ET integration
│   └── CarpoolSiriMapper    # Maps SIRI to domain model
└── service/                  # Service layer
    ├── CarpoolingService    # Main service interface
    └── DefaultCarpoolingService # Service implementation
```

## Trip Matching Algorithm

The carpooling service uses a multi-phase algorithm to match passengers with compatible carpool trips:

### 1. Pre-Filter Phase
Fast pre-screening to eliminate incompatible trips using necessary conditions only (loose bounds);
tight enforcement is deferred to the post-filter once actual times are known:
- **Capacity Filter**: Checks if any seats are available
- **TimeTripFilter**: Trip start/end is loosely compatible with the passenger's depart-after or
  arrive-by window
- **DistanceTripFilter**: Validates pickup/dropoff are within 50km of driver's route

### 2. Routing Phase
Optimal insertion point calculation:
- Uses beeline estimates for early rejection
- Routes baseline segments once and caches results
- Evaluates all viable insertion positions
- Selects position with minimum additional travel time

### 3. Post-Filter Phase
Applied to fully-routed itineraries with actual computed times:
- **TimeItineraryFilter**: Tight enforcement of the passenger's depart-after / arrive-by window
  against the itinerary's real start/end times

### 4. Constraint Validation
- **Capacity constraints**: Ensures vehicle capacity is not exceeded
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

### Time Windows
Both depart-after and arrive-by requests are supported. The pre-filter uses loose bounds
(necessary conditions) to limit routing cost; the post-filter enforces the passenger's window
tightly against the routed itinerary times.
