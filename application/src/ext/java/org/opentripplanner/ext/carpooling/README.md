# Carpooling Extension for OpenTripPlanner

The carpooling extension enables OpenTripPlanner to find carpool trip options by matching passenger requests with active driver journeys. Passengers can be dynamically inserted into existing driver routes at optimal pickup and dropoff points while respecting capacity constraints, timing windows, and driver deviation budgets.

## Quick Overview

**What it does**: Matches passengers with drivers offering their vehicle journey for ride-sharing.

**Why it exists**: Provides flexible, demand-responsive carpooling as a complement to fixed-route transit.

**How it works**: Pre-filters the driver trips, evaluates the best insertion of the passenger into each from street driving times (searched per request for direct trips, precomputed per trip for access and egress), and validates the result against the passenger's time window.

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
│  2. Insertion Phase (InsertionEvaluator)   │
│     - Driving times per leg and point      │
│       (street search, or the trip's       │
│       corridor for access/egress)          │
│     - Every pickup/dropoff pair of legs    │
│       checked against budgets + capacity   │
│     - Select minimum additional time       │
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
├── CarpoolingParameters.java       # The routing limits, in one place
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
│   ├── TimeTripFilter.java   # Pre-filter: depart-after & arrive-by time checks
│   ├── TimeItineraryFilter.java # Post-filter: depart-after & arrive-by enforcement
│   └── DistanceTripFilter.java # Distance check
│
├── routing/                         # Insertion optimization
│   ├── InsertionEvaluator.java     # Best insertion per trip
│   ├── InsertionCandidate.java     # Result of insertion computation
│   ├── CarpoolCorridor.java        # Per-trip stops and driving times, built at ingest
│   ├── CorridorBuilder.java        # Builds corridors from ellipse-bounded car trees
│   ├── CarpoolStopIndex.java       # Transit stops with their car-reachable snaps
│   ├── CompactCarTree.java         # One-to-many car search over flat arrays
│   ├── CarpoolTreeStreetRouter.java # Tree-backed router (passenger's trees)
│   ├── CorridorRouter.java         # Access/egress router over corridor + passenger trees
│   ├── CarpoolStreetRouter.java    # Goal-directed street router (direct mode)
│   └── PerStopCandidateCap.java    # Bounds the access/egress candidates per stop
│
├── internal/                        # Implementation details
│   ├── DefaultCarpoolingRepository.java  # In-memory repository with a spatial index
│   └── CarpoolItineraryMapper.java # Maps insertions to itineraries
│
├── updater/                         # Real-time updates
│   ├── SiriETCarpoolingUpdater.java  # SIRI-ET message processing
│   └── CarpoolTripResolutionQueue.java # Background resolution of incoming trips
│
├── util/                            # Utilities
│   └── BeelineEstimator.java       # Straight-line distance estimation
│
└── configure/                       # Dependency injection
    └── CarpoolingModule.java       # Dagger module
```

## Algorithm Explanation

### Phase 1: Filtering (Fast Pre-screening)

Filters eliminate obviously incompatible trips **without any street routing**:

1. **TimeTripFilter**: Is the trip timing compatible with the passenger's request (depart-after or arrive-by)?
2. **DistanceTripFilter**: Is the passenger's journey within reasonable distance of driver route?

**Performance**: O(n) where n = number of active trips.

### Phase 2: Insertion Optimization (Finding Best Position)

For every candidate trip the `InsertionEvaluator` picks the pair of legs (one for the pickup,
one for the dropoff, possibly the same) that adds the least driving time while every later stop
stays within its deviation budget and the car has a free seat on every leg the passenger rides.

The detour of putting a point into a leg is `drive(leg start -> point) + drive(point -> leg end)
- leg + dwell` and does not depend on where the other point goes, so each pair of legs is a few
additions and comparisons once the driving times per leg and point are known. Only the winning
route is assembled; street paths are built only for the insertions that end up in an itinerary.

Where the driving times come from:

- **Direct**: goal-directed street searches (`CarpoolStreetRouter`). A leg whose beeline detour
  already exceeds the following stop's budget is not searched. At most the 50 trips passing closest
  to the passenger are evaluated (`ClosestCandidateTrips`).
- **Access/egress**: when a trip arrives, `CorridorBuilder` computes its `CarpoolCorridor`: the
  routed baseline legs and, per leg, every transit stop inside the leg's feasibility ellipse with
  the driving times to serve it. A request then builds only the passenger's two street trees
  (`CompactCarTree`) and reads everything else from the corridors of the trips found near the
  passenger by the repository's spatial index. A transit stop with more candidates than Raptor can
  use hands over the first, the last and one per slot of the search window (`PerStopCandidateCap`).

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
@Inject CarpoolTripVertexResolver tripVertexResolver;

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

// Resolve route points to street vertices and add to the repository. Resolution returns null
// when a route point has no car-reachable street vertex; such a trip is not routable, so drop it.
var resolved = tripVertexResolver.resolve(trip);
if (resolved != null) {
  repository.upsertCarpoolTrip(resolved);
}
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
1. **Too many candidate trips near the passenger**: at most 50 are evaluated per request, but each
   still costs a few street searches in direct mode
2. **Large route deviation budgets**: widen the feasibility ellipses, so corridors hold more stops
   and the passenger's trees grow
3. **Complex street networks**: street searches take longer

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
  public boolean isCandidateTrip(CarpoolTrip trip, CarpoolingRequest request) {
    // Custom logic
    return true;
  }
}

// Add to filter chain
var preFilters = new TripPreFilters(
  List.of(new TimeTripFilter(), new DistanceTripFilter(), new CustomFilter())
);
```

## Testing

### Unit Testing

Test individual components in isolation:

```java
@Test
void testTimeTripFilter() {
  var filter = new TimeTripFilter();
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
  var resolved = tripVertexResolver.resolve(testTrip);
  assertNotNull(resolved);  // a seeded test trip is expected to resolve on the test graph
  repository.upsertCarpoolTrip(resolved);

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