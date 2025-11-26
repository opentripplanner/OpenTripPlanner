# Carpooling Extension for OpenTripPlanner

The carpooling extension enables OpenTripPlanner to find carpool trip options by matching passenger requests with active driver journeys. Passengers can be dynamically inserted into existing driver routes at optimal pickup and dropoff points while respecting capacity constraints, timing windows, and driver deviation budgets.

## Quick Overview

**What it does**: Matches passengers with drivers offering their vehicle journey for ride-sharing.

**Why it exists**: Provides flexible, demand-responsive carpooling as a complement to fixed-route transit.

**How it works**: Three-phase algorithm (filter → pre-screen → route → validate) finds optimal passenger insertion points in driver routes using A* street routing with intelligent position pre-screening and segment caching.

## Key Features

- **Real-time matching**: Finds compatible carpool trips from active driver pool
- **Optimal insertion**: Computes best pickup/dropoff positions using A* street routing
- **Flexible constraints**: Respects capacity, time windows, driver deviation budgets
- **Performance optimized**: Fast filtering eliminates 70-90% of trips before routing
- **SIRI-ET integration**: Real-time trip updates from external carpooling platforms

## Architecture

### High-Level Flow

```
┌─────────────────┐
│ Passenger       │
│ Routing Request │
└────────┬────────┘
         │
         v
┌────────────────────────────────────────────┐
│ DefaultCarpoolingService                   │
│                                            │
│  1. Filter Phase (FilterChain)            │
│     - Capacity check                       │
│     - Time window check                    │
│     - Direction check                      │
│     - Distance check                       │
│                                            │
│  2. Insertion Phase                        │
│     2a. Position Pre-screening             │
│         (InsertionPositionFinder)          │
│         - Capacity check                   │
│         - Directional check                │
│         - Beeline delay heuristic          │
│                                            │
│     2b. Routing & Selection                │
│         (OptimalInsertionStrategy)         │
│         - Route baseline segments (cached) │
│         - Route viable positions           │
│         - Endpoint-matching segment reuse  │
│         - Select minimum additional time   │
│                                            │
│  3. Validation Phase (CompositeValidator) │
│     - Capacity timeline check              │
│     - Directional consistency check        │
│     - Deviation budget check               │
│                                            │
└────────┬───────────────────────────────────┘
         │
         v
┌────────────────────┐
│ Itinerary Results  │
│ (CarpoolLeg)       │
└────────────────────┘
```

### Package Structure

```
org.opentripplanner.ext.carpooling/
├── CarpoolingService.java          # Main API interface
├── CarpoolingRepository.java       # Trip data management
│
├── model/                           # Domain models
│   ├── CarpoolTrip.java            # Driver's journey with stops
│   ├── CarpoolStop.java            # Waypoint along route
│   ├── CarpoolLeg.java             # Itinerary leg for results
│   └── CarpoolTripBuilder.java     # Builder for trip construction
│
├── service/                         # Service implementation
│   └── DefaultCarpoolingService.java  # Main service orchestration
│
├── filter/                          # Pre-screening filters
│   ├── FilterChain.java            # Composite filter
│   ├── CapacityFilter.java         # Seat availability check
│   ├── TimeBasedFilter.java        # Time window check
│   ├── DirectionalCompatibilityFilter.java  # Direction check
│   └── DistanceBasedFilter.java    # Distance check
│
├── routing/                         # Insertion optimization
│   ├── OptimalInsertionStrategy.java  # Main insertion algorithm
│   ├── InsertionPositionFinder.java   # Viable position pre-screening
│   ├── InsertionPosition.java      # Position pair (pickup, dropoff)
│   └── InsertionCandidate.java     # Result of insertion computation
│
├── validation/                      # Constraint validation
│   ├── CompositeValidator.java     # Composite validator
│   ├── CapacityValidator.java      # Capacity timeline check
│   └── DirectionalValidator.java   # Backtracking check
│
├── internal/                        # Implementation details
│   ├── DefaultCarpoolingRepository.java  # In-memory repository
│   └── CarpoolItineraryMapper.java # Maps insertions to itineraries
│
├── updater/                         # Real-time updates
│   └── SiriETCarpoolingUpdater.java  # SIRI-ET message processing
│
├── util/                            # Utilities
│   ├── BeelineEstimator.java       # Straight-line distance estimation
│   └── DirectionalCalculator.java  # Bearing and direction calculations
│
├── constraints/                     # Constraint definitions
│   └── PassengerDelayConstraints.java  # Delay limits for passengers
│
└── configure/                       # Dependency injection
    └── CarpoolingModule.java       # Dagger module
```

## Algorithm Explanation

### Phase 1: Filtering (Fast Pre-screening)

Filters eliminate obviously incompatible trips **without any street routing**:

1. **CapacityFilter**: Does the vehicle have available seats?
2. **TimeBasedFilter**: Is the trip timing compatible with passenger request?
3. **DirectionalCompatibilityFilter**: Are driver and passenger heading the same direction?
4. **DistanceBasedFilter**: Is the passenger's journey within reasonable distance of driver route?

**Performance**: O(n) where n = number of active trips.

### Phase 2: Insertion Optimization (Finding Best Position)

For trips that pass filtering, computes optimal pickup/dropoff positions using a two-stage approach:

#### Stage 1: Position Pre-screening (InsertionPositionFinder)

Fast heuristic checks eliminate impossible positions **before any A* routing**:

```
For each remaining trip:
  1. Generate all position combinations (pickup, dropoff) where:
     - Pickup: between any two consecutive stops (1-indexed)
     - Dropoff: after pickup position

  2. For each position pair, check:
     a. Capacity: Does insertion exceed vehicle capacity at any point?
     b. Direction: Does insertion cause backtracking or U-turns?
     c. Beeline delay: Do straight-line estimates exceed delay threshold?

  3. Return only "viable" positions that pass all checks
```

**Key optimizations**:
- **Capacity validation**: Uses `CarpoolTrip.hasCapacityForInsertion()` to check entire journey range
- **Directional filtering**: Prevents insertions that deviate >90° from route bearing
- **Beeline heuristic**: Optimistic straight-line estimates eliminate positions early
- **No routing yet**: All checks use geometric calculations only

#### Stage 2: Routing and Selection (OptimalInsertionStrategy)

For viable positions from Stage 1, perform A* routing to find the optimal insertion:

```
For each trip with viable positions:
  1. Route baseline segments (driver's original route) and cache results

  2. For each viable position:
     a. Build modified route with passenger inserted
     b. Route only segments with changed endpoints
     c. Reuse cached segments where endpoints match exactly
     d. Calculate total duration and additional time vs. baseline
     e. Check passenger delay constraints

  3. Select insertion with minimum additional time
  4. Ensure additional time ≤ driver's deviation budget
```

**Critical optimization - Endpoint-matching segment reuse**:
- Baseline segments are cached after first routing
- For modified routes, segments are reused **only if both endpoints match exactly**
- Endpoint matching uses `WgsCoordinate.equals()` with 7-decimal precision (~1cm)
- Only segments with changed endpoints are re-routed
- Prevents incorrect reuse when passenger insertion splits existing segments

### Phase 3: Validation (Constraint Satisfaction)

Ensures the proposed insertion satisfies all constraints:

1. **CapacityValidator**: Verifies sufficient capacity throughout passenger's journey
   - Tracks passenger count at each stop
   - Ensures capacity never exceeds vehicle limit

2. **DirectionalValidator**: Ensures no backtracking
   - Computes bearings between consecutive stops
   - Rejects if bearing changes > threshold (indicates backtracking)

3. **Deviation Budget Check**: Ensures additional time ≤ driver's stated willingness

**All validators must pass** for an insertion to be considered valid.

## Usage Examples

### Basic Carpooling Query

```java
// Injected via Dagger
@Inject CarpoolingService carpoolingService;

// Create routing request
RouteRequest request = new RouteRequest();
request.setFrom(new GenericLocation(59.9, 10.7));   // Passenger pickup
request.setTo(new GenericLocation(59.95, 10.75));   // Passenger dropoff
request.setDateTime(Instant.now());

// Find carpool options
List<Itinerary> carpoolOptions = carpoolingService.route(request);

// Process results
for (Itinerary itinerary : carpoolOptions) {
  // Each itinerary contains a CarpoolLeg with:
  // - Pickup time and location
  // - Dropoff time and location
  // - Journey duration
  // - Route geometry
}
```

### Adding Driver Trips via SIRI-ET

Trips are typically added via the SIRI-ET updater, but can also be added programmatically:

```java
@Inject CarpoolingRepository repository;

// Build a trip using the builder
CarpoolTrip trip = CarpoolTrip.builder()
  .withId(FeedScopedId.parse("PROVIDER:trip123"))
  .withBoardingArea(boardingArea)
  .withAlightingArea(alightingArea)
  .withStartTime(ZonedDateTime.now())
  .withEndTime(ZonedDateTime.now().plusMinutes(35))  // 30 min journey + 5 min buffer
  .withDeviationBudget(Duration.ofMinutes(5))        // Willing to deviate 5 minutes
  .withAvailableSeats(3)
  .withProvider("PROVIDER")
  .withStops(List.of(
    // Add intermediate stops if any
  ))
  .build();

// Add to repository (makes immediately available for routing)
repository.upsertCarpoolTrip(trip);
```

## Configuration

The carpooling extension is a sandbox feature that must be enabled:

```json
// router-config.json
{
  "otpFeatures": {
    "CarPooling": true
  }
}
```

### SIRI-ET Real-time Updates

Configure the SIRI-ET updater to receive trip updates:

```json
// router-config.json
{
  "updaters": [
    {
      "type": "siri-et-carpooling-updater",
      "url": "https://api.carpooling-provider.com/siri-et",
      "feedId": "PROVIDER",
      "frequencySec": 30
    }
  ]
}
```

## Data Model

### CarpoolTrip

Represents a driver's journey offering carpool seats:

- **id**: Unique trip identifier
- **boardingArea**: Start zone for driver journey
- **alightingArea**: End zone for driver journey
- **startTime**: When driver departs
- **endTime**: When driver arrives (includes deviation budget)
- **deviationBudget**: Extra time driver is willing to spend for passengers
- **availableSeats**: Current remaining capacity
- **stops**: Ordered list of waypoints (includes booked passenger stops)
- **provider**: Source system identifier

### CarpoolStop

Waypoint along a carpool route:

- **coordinate**: Geographic location
- **sequenceNumber**: Order in route (0-indexed)
- **estimatedArrivalTime**: When driver expects to arrive
- **stopType**: PICKUP or DROPOFF
- **passengerDelta**: Change in passenger count (+1 for pickup, -1 for dropoff)

### InsertionPosition

Represents a viable pickup/dropoff position pair:

- **pickupPos**: Position to insert passenger pickup (1-indexed)
- **dropoffPos**: Position to insert passenger dropoff (1-indexed)

Note: Positions are 1-indexed to match insertion semantics (insert between existing points).

### InsertionCandidate

Result of finding optimal passenger insertion:

- **trip**: The original carpool trip
- **pickupPosition**: Where to insert passenger pickup (index)
- **dropoffPosition**: Where to insert passenger dropoff (index)
- **segments**: Routed path segments for modified route
- **baselineDuration**: Original trip duration
- **totalDuration**: Modified trip duration (with passenger)
- **additionalDuration**: Extra time added (= totalDuration - baselineDuration)

## Performance Characteristics

### Performance Bottlenecks

If performance degrades:
1. **Too many active trips**: Filter more aggressively
2. **Large route deviation budgets**: Increases insertion positions to test
3. **Complex street networks**: A* routing takes longer

## Thread Safety

All components are designed for concurrent access:

- **CarpoolingService**: Stateless, fully thread-safe
- **CarpoolingRepository**: Uses ConcurrentHashMap for thread-safe reads/writes
- **Filters & Validators**: Stateless, fully thread-safe

Multiple routing requests can execute concurrently without coordination.

## Extension Points

### Custom Filters

Add domain-specific filters by implementing `TripFilter`:

```java
public class CustomFilter implements TripFilter {
  @Override
  public boolean accepts(CarpoolTrip trip, WgsCoordinate pickup,
                        WgsCoordinate dropoff, Instant requestTime) {
    // Custom logic
    return true;
  }

  @Override
  public String name() {
    return "CustomFilter";
  }
}

// Add to filter chain
FilterChain chain = FilterChain.of(
  new CapacityFilter(),
  new TimeBasedFilter(),
  new CustomFilter()
);
```

### Custom Validators

Add constraint validation by implementing `InsertionValidator`:

```java
public class CustomValidator implements InsertionValidator {
  @Override
  public ValidationResult validate(ValidationContext context) {
    // Custom validation logic
    if (violatesConstraint) {
      return ValidationResult.invalid("Constraint violated");
    }
    return ValidationResult.valid();
  }
}
```

## Testing

### Unit Testing

Test individual components in isolation:

```java
@Test
void testCapacityFilter() {
  var filter = new CapacityFilter();
  var trip = createTripWithSeats(2);  // 2 available seats

  // Should pass - within capacity
  assertTrue(filter.accepts(trip, pickup, dropoff, now()));

  var fullTrip = createTripWithSeats(0);  // No seats

  // Should fail - no capacity
  assertFalse(filter.accepts(fullTrip, pickup, dropoff, now()));
}
```

### Integration Testing

Test full routing flow with graph:

```java
@Test
void testCarpoolingRouting() {
  // Build test graph with carpool trips
  Graph graph = buildTestGraph();
  repository.upsertCarpoolTrip(testTrip);

  // Enable feature
  OTPFeature.enableFeatures(Map.of(OTPFeature.CarPooling, true));

  // Execute routing
  RouteRequest request = createRequest(from, to);
  List<Itinerary> results = carpoolingService.route(request);

  // Verify
  assertFalse(results.isEmpty());
  assertTrue(results.get(0).getLegs().get(0) instanceof CarpoolLeg);
}
```

## Troubleshooting

### No carpool results returned

1. **Check feature toggle**: Ensure `CarPooling` is enabled in `router-config.json`
2. **Verify trip data**: Use `repository.getCarpoolTrips()` to check active trips
3. **Check filters**: Enable DEBUG logging to see which filters reject trips
4. **Time windows**: Ensure passenger request time matches trip timing

### Poor performance

1. **Too many active trips**: Consider cleanup of expired trips
2. **Enable logging**: Set `org.opentripplanner.ext.carpooling` to DEBUG
3. **Profile filters**: Check which filters are rejecting trips
4. **Reduce deviation budget**: Limits insertion positions to test

### Routing failures

1. **Street network connectivity**: Ensure OSM data covers pickup/dropoff areas
2. **Car routing enabled**: Verify street mode CAR is allowed
3. **Check routing logs**: Look for "Routing failed" warnings
4. **Verify coordinates**: Ensure pickup/dropoff are valid coordinates
5. 