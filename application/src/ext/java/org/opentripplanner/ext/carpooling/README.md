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
│  1. Filter Phase (TripPreFilters)         │
│     - Time window check                    │
│     - Distance check                       │
│     - Direction check                      │
│                                            │
│  2. Insertion Phase                        │
│     2a. Position Pre-screening             │
│         (InsertionPositionFinder)          │
│         - Capacity check                   │
│         - Beeline delay heuristic          │
│                                            │
│     2b. Routing & Selection                │
│         (InsertionEvaluator)               │
│         - Route baseline segments (cached) │
│         - Route viable positions           │
│         - Endpoint-matching segment reuse  │
│         - Select minimum additional time   │
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
├── filter/                          # Pre- and post-screening filters
│   ├── TripPreFilters.java         # Pre-filter composite (AND, short-circuit)
│   ├── ItineraryPostFilters.java   # Post-filter composite (AND, short-circuit)
│   ├── DepartAfterTripFilter.java  # Pre-filter: depart-after time check
│   ├── ArriveByTripFilter.java     # Pre-filter: arrive-by time check
│   ├── DepartAfterItineraryFilter.java  # Post-filter: depart-after enforcement
│   ├── ArriveByItineraryFilter.java     # Post-filter: arrive-by enforcement
│   └── DistanceBasedFilter.java    # Distance check
│
├── routing/                         # Insertion optimization
│   ├── InsertionEvaluator.java        # Routing evaluation and selection
│   ├── InsertionPositionFinder.java   # Viable position pre-screening
│   ├── InsertionPosition.java      # Position pair (pickup, dropoff)
│   └── InsertionCandidate.java     # Result of insertion computation
│
├── internal/                        # Implementation details
│   ├── DefaultCarpoolingRepository.java  # In-memory repository
│   └── CarpoolItineraryMapper.java # Maps insertions to itineraries
│
├── updater/                         # Real-time updates
│   └── SiriETCarpoolingUpdater.java  # SIRI-ET message processing
│
├── util/                            # Utilities
│   └── BeelineEstimator.java       # Straight-line distance estimation
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

1. **DepartAfterTripFilter / ArriveByTripFilter**: Is the trip timing compatible with the passenger's request?
2. **DistanceBasedFilter**: Is the passenger's journey within reasonable distance of driver route?

**Performance**: O(n) where n = number of active trips.

### Phase 2: Insertion Optimization (Finding Best Position)

For trips that pass filtering, computes optimal pickup/dropoff positions using a two-stage approach:

#### Stage 1: Position Pre-screening (InsertionPositionFinder)

Fast heuristic checks eliminate impossible positions **before any A* routing**:

```
For each remaining trip:
  1. Generate all position combinations (pickup, dropoff) where:
     - Pickup: between any two consecutive stops (0-based index in modified route)
     - Dropoff: after pickup position

  2. For each position pair, check:
     a. Capacity: Does insertion exceed vehicle capacity at any point?
     b. Beeline delay: Do straight-line estimates exceed delay threshold?

  3. Return only "viable" positions that pass all checks
```

**Key optimizations**:
- **Capacity validation**: Uses `CarpoolTrip.hasCapacityForInsertion()` to check entire journey range
- **Beeline heuristic**: Optimistic straight-line estimates eliminate positions early
- **No routing yet**: All checks use geometric calculations only

#### Stage 2: Routing and Selection (InsertionEvaluator)

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
List<Itinerary> carpoolOptions = carpoolingService.routeDirect(request, linkingContext);

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
- **startTime**: When the driver departs
- **endTime**: When the driver arrives
- **totalCapacity**: Number of seats in the car, including the driver seat
- **stops**: Ordered list of waypoints; the first stop is the origin, the last is the destination, and booked passenger stops are inserted in between
- **provider**: Source system identifier

### CarpoolStop

Waypoint along a carpool route:

- **coordinate**: Geographic location
- **aimedArrivalTime**: Planned arrival time (null for the origin stop)
- **expectedArrivalTime**: Currently expected arrival time, updated via real-time (null for the origin stop)
- **latestExpectedArrivalTime**: Latest arrival time the driver commits to (null if not provided); used to derive `deviationBudget`
- **aimedDepartureTime**: Planned departure time (null for the destination stop)
- **expectedDepartureTime**: Currently expected departure time (null for the destination stop)
- **deviationBudget**: Extra time the driver is willing to spend on deviations before reaching this stop
- **onboardCount**: Number of passengers onboard (including the driver) when departing this stop

### InsertionPosition

Represents a viable pickup/dropoff position pair:

- **pickupPos**: 0-based index of the passenger's pickup in the modified route
- **dropoffPos**: 0-based index of the passenger's dropoff in the modified route

### InsertionCandidate

Result of finding optimal passenger insertion:

- **trip**: The original carpool trip
- **pickupPosition**: 0-based index of the passenger's pickup in the modified route
- **dropoffPosition**: 0-based index of the passenger's dropoff in the modified route
- **routeSegments**: Routed path segments forming the complete modified route
- **stopDuration**: Dwell time added at each intermediate stop (from the car routing preferences' `pickupTime`)
- **transitStop**: Passenger's access/egress stop, if any
- **totalTripDuration**: Total trip duration including driving and stop delays, computed from `routeSegments` and `stopDuration`

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

Add domain-specific filters by implementing `CarpoolTripFilter`:

```java
public class CustomFilter implements CarpoolTripFilter {
  @Override
  public boolean isCandidateTrip(CarpoolTrip trip, CarpoolingRequest request, Duration searchWindow) {
    // Custom logic
    return true;
  }
}

// Add to filter chain
var preFilters = new TripPreFilters(
  List.of(new DepartAfterTripFilter(), new ArriveByTripFilter(), new DistanceBasedFilter(), new CustomFilter())
);
```

## Testing

### Unit Testing

Test individual components in isolation:

```java
@Test
void testDepartAfterTripFilter() {
  var filter = new DepartAfterTripFilter();
  var trip = createSimpleTrip(origin, destination);
  var request = new CarpoolingRequestBuilder().withRequestedDateTime(now()).build();

  assertTrue(filter.isCandidateTrip(trip, request, Duration.ofMinutes(30)));
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
  List<Itinerary> results = carpoolingService.routeDirect(request, linkingContext);

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