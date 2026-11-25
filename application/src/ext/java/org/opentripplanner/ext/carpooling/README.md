# Carpooling Extension for OpenTripPlanner

The carpooling extension enables OpenTripPlanner to find carpool trip options by matching passenger requests with active driver journeys. Passengers can be dynamically inserted into existing driver routes at optimal pickup and dropoff points while respecting capacity constraints, timing windows, and driver deviation budgets.

Configuration and the SIRI-ET data format are described in `doc/user/sandbox/Carpooling.md`.

## High-Level Flow

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
│     (after snapping the passenger)         │
│     - Time window check                    │
│     - Free seat check                      │
│     - Corridor reach check                 │
│                                            │
│  2. Insertion Phase (InsertionEvaluator)   │
│     - Driving times via the pickup and     │
│       via the dropoff, for every leg       │
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

## Package Structure

```
org.opentripplanner.ext.carpooling/
├── CarpoolingService       # Main API interface
├── CarpoolingRepository    # Trip data management
├── CarpoolingParameters    # The routing limits and heuristics, in one place
├── model/                  # A driver's trip and its stops; the carpool leg of an itinerary
├── updater/                # Reads the driver trips from SIRI-ET
├── street/                 # Car searches on the street graph; snapping to where a car can stop
├── corridor/               # What a trip can serve, computed when the trip arrives
├── filter/                 # The filters screening trips and itineraries
├── routing/                # The request; insertion of the passenger into its candidate trips
├── mapping/                # The rides as Raptor access/egress legs and as itineraries
├── internal/               # The service and the repository implementations
├── config/                 # The `carpooling` section of router-config.json
└── configure/              # Dagger module
```

## Algorithm Explanation

### Phase 1: Filtering (Fast Pre-screening)

Once the passenger is snapped to the street network, filters eliminate obviously incompatible trips **without any further street routing**. They are necessary conditions only: they never reject a trip that could serve the passenger inside the passenger's window.

1. **TimeTripFilter**: Can the trip be at the passenger's pickup or dropoff inside the passenger's window (depart-after or arrive-by)? The trip's span is taken as the insertion evaluation times it: from its start to its routed arrival at the destination plus the destination's deviation budget.
2. **CapacityTripFilter**: Does the car have a free seat on some leg? A car that is full all the way cannot take the passenger.
3. **CorridorTripFilter** (`CarpoolCorridor.mayServe`): Can the trip's corridor reach the snapped passenger at all? A leg can only serve a point whose beeline detour, driven at the graph's maximum car speed, fits the leg's limit; direct requests check both the pickup and the dropoff.

**Performance**: O(n) where n = number of active trips.

### Phase 2: Insertion Optimization (Finding Best Position)

For every candidate trip the `InsertionEvaluator` picks the pair of legs (one for the pickup,
one for the dropoff, possibly the same) that adds the least driving time while every later stop
stays within its deviation budget and the car has a free seat on every leg the passenger rides.

Where the driving times come from:

- **Direct**: the street trees of the passenger's pickup and dropoff (`CarpoolTreeStreetRouter`)
  and the candidate trips' corridors, as for access/egress below but without the stops.
- **Access/egress**: when a trip arrives, `CorridorBuilder` computes its `CarpoolCorridor`: the
  routed baseline legs and, per leg, every transit stop inside the leg's feasibility ellipse with
  the driving times to serve it. A request then builds only the passenger's two street trees
  (`CarpoolTreeStreetRouter`) and reads everything else from the candidate trips' corridors.

In both modes, at most the 30 trips passing closest to the passenger are evaluated
(`ClosestCandidateTrips`); for access and egress, those without a stop the passenger can use are
then left out.

### Phase 3: Post-Filtering (Direct Routing Only)

**TimeItineraryFilter** enforces the passenger's depart-after / arrive-by window against the
itinerary's real start and end times, which the loose pre-filter could only bound.

## Performance Bottlenecks

If performance degrades:
1. **Too many candidate trips near the passenger**: at most 30 are evaluated per request, but the
   passenger's trees have to reach all of their waypoints
2. **Large route deviation budgets**: widen the feasibility ellipses, so corridors hold more stops
   and the passenger's trees grow
3. **Complex street networks**: the passenger's trees take longer to build
