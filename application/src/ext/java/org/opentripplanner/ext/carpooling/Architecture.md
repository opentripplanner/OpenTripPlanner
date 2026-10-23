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
│   ├── CarpoolCorridor      # Per-trip stops and driving times
│   ├── CorridorBuilder      # Computes a trip's corridor
│   ├── CarpoolStopIndex     # Transit stops with their car-reachable snaps
│   ├── CarpoolStreetRouter  # Goal-directed street routing
│   ├── CorridorRouter       # Access/egress driving times: corridors and passenger trees
│   └── PassengerTrees       # The passenger's two car trees of a request
├── filter/                   # Pre- and post-screening
│   ├── CarpoolingRequest    # Passenger-side request abstraction
│   ├── CarpoolTripFilter    # Pre-filter interface (trips vs. the snapped passenger)
│   ├── SnappedPassenger     # The passenger's snapped pickup and dropoff
│   ├── CarpoolItineraryFilter # Post-filter interface (routed itineraries)
│   ├── TripPreFilters       # Composite of pre-filters (AND)
│   ├── ClosestCandidateTrips # The trips passing closest to the passenger
│   ├── ItineraryPostFilters # Composite of post-filters (AND)
│   ├── TimeTripFilter       # Pre-filter: loose time-window check
│   ├── CapacityTripFilter   # Pre-filter: a free seat on some leg
│   ├── CorridorTripFilter   # Pre-filter: the trip's corridor reaches the passenger
│   └── TimeItineraryFilter  # Post-filter: tight time-window enforcement
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
Fast pre-screening, once the passenger is snapped to the street network, to eliminate incompatible
trips using necessary conditions only (loose bounds); tight enforcement is deferred to the
post-filter once actual times are known:
- **TimeTripFilter**: the trip's time span overlaps the passenger's time window
- **CapacityTripFilter**: the car has a free seat on some leg
- **CorridorTripFilter**: the trip's corridor can reach the snapped passenger
  (`CarpoolCorridor.mayServe`)

### 2. Routing Phase
Optimal insertion point calculation:
- Uses beeline estimates for early rejection
- Every pair of pickup and dropoff legs is evaluated in constant time from the driving times
- Selects the pair with minimum additional travel time that keeps a free seat on every leg the
  passenger rides and fits the deviation budget of every stop it delays

### 3. Post-Filter Phase
Applied to fully-routed itineraries with actual computed times:
- **TimeItineraryFilter**: Tight enforcement of the passenger's depart-after / arrive-by window
  against the itinerary's real start/end times

## Multi-Stop Support

The system handles trips with multiple existing passengers:
- Each stop tracks passenger count changes (pickups and dropoffs)
- Capacity validation ensures vehicle is never over capacity
- Route optimization considers all existing stops when inserting new passengers
- The stops' deviation budgets protect all existing passengers from excessive delays

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
