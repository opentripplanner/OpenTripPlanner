# Vehicle Rental Street — Geofencing Enforcement

This package contains the geofencing zone enforcement system for vehicle rental routing.
It intercepts edge traversals to enforce zone restrictions (no-drop-off, no-traversal,
business area boundaries) during A* street searches.

## Architecture

The system uses an interceptor pattern with strategy-based enforcement:

```
StreetEdge.traverse()
  └─ GeofencingInterceptor.apply()
       ├─ DeferredForkHandler        — completes renting branches deferred from prior edge
       ├─ GeofencingEnforcement      — strategy interface, dispatched per zone type
       │    ├─ RestrictedZoneEnforcement  — no-drop-off and no-traversal zones
       │    └─ BusinessAreaEnforcement   — business area exit enforcement
       └─ NetworkCommitmentHandler   — forks generic arrive-by states into per-network branches
```

**GeofencingInterceptor** is the entry point, called from `StreetEdge.traverse()`. It returns
`State[]` to override normal traversal, or `null` to let it proceed. Forward enforcement checks
**tov** boundaries (approaching/inside a zone). ArriveBy enforcement checks **fromv** boundaries
(exiting a zone in real-time terms).

**GeofencingEnforcement** is the strategy interface. Implementations are stateless singletons
resolved via `GeofencingEnforcement.forZone(zone)`. Each receives an `EdgeTraversal` lambda
wrapping `StreetEdge.doTraverse()`, allowing it to traverse the edge in any mode without
depending on StreetEdge.

**DeferredForkHandler** works around the backEdge/itinerary-builder contract: in arrive-by,
renting branches are created one edge after the zone boundary so their backEdge points outside
the zone when the itinerary is reversed to forward time.

**NetworkCommitmentHandler** handles generic (null-network) RENTING_FLOATING states that only
exist in arrive-by searches. When crossing a zone boundary, it forks committed branches for
each applicable network and continues the generic state with updated `committedNetworks`.

## Boundary Infrastructure

- **GeofencingBoundaryExtension** — `record(zone, entering)` placed on boundary-crossing vertices
  by `GeofencingZoneApplier`. `entering=true` means traversing FROM this vertex enters the zone.
- **GeofencingZoneIndex** — STRtree spatial index for zone containment queries.
- **GeofencingZoneApplier** — applies boundary extensions and builds the spatial index at GBFS
  update time.

## Zone State Tracking

Zone state lives on routing `State` (via `StateData.currentGeofencingZones`), updated at
boundary-crossing edges by `StateEditor.updateGeofencingZones()`. Per-field enforcement
(`isDropOffBannedByCurrentZones`, `isTraversalBannedByCurrentZones`) resolves restrictions
via priority-based precedence across overlapping zones.
