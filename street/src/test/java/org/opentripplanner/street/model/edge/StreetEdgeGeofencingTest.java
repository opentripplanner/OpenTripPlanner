package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelFactory.streetEdge;
import static org.opentripplanner.street.search.TraverseMode.SCOOTER;
import static org.opentripplanner.street.search.TraverseMode.WALK;
import static org.opentripplanner.street.search.state.VehicleRentalState.HAVE_RENTED;
import static org.opentripplanner.street.search.state.VehicleRentalState.RENTING_FLOATING;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType.PropulsionType;
import org.opentripplanner.service.vehiclerental.model.TestGeofencingZoneBuilder;
import org.opentripplanner.service.vehiclerental.street.geofencing.GeofencingBoundaryExtension;
import org.opentripplanner.street.geometry.Polygons;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

class StreetEdgeGeofencingTest {

  static String NETWORK_TIER = "tier-oslo";
  static String NETWORK_BIRD = "bird-oslo";

  static GeofencingZone NO_DROP_OFF_ZONE_TIER = TestGeofencingZoneBuilder.of(NETWORK_TIER, "a-park")
    .withGeometry(Polygons.OSLO)
    .noDropOff()
    .build();

  static GeofencingZone NO_DROP_OFF_ZONE_BIRD = TestGeofencingZoneBuilder.of(NETWORK_BIRD, "a-park")
    .withGeometry(Polygons.OSLO)
    .noDropOff()
    .build();

  static GeofencingZone NO_DROP_OFF_ZONE_TIER_2 = TestGeofencingZoneBuilder.of(
    NETWORK_TIER,
    "b-park"
  )
    .withGeometry(Polygons.OSLO)
    .noDropOff()
    .build();

  static GeofencingZone NO_TRAVERSAL_ZONE = TestGeofencingZoneBuilder.of(
    NETWORK_TIER,
    "no-traverse"
  )
    .withGeometry(Polygons.OSLO)
    .noTraversal()
    .build();

  static GeofencingZone BUSINESS_AREA_ZONE = TestGeofencingZoneBuilder.of(
    NETWORK_TIER,
    "business-area"
  )
    .withGeometry(Polygons.OSLO)
    .asBusinessArea()
    .build();

  StreetVertex V1 = intersectionVertex("V1", 0, 0);
  StreetVertex V2 = intersectionVertex("V2", 1, 1);
  StreetVertex V3 = intersectionVertex("V3", 2, 2);
  StreetVertex V4 = intersectionVertex("V4", 3, 3);

  @Nested
  class Forward {

    @Test
    public void finishInEdgeWithoutRestrictions() {
      var edge = streetEdge(V1, V2);
      var result = traverseFromV1(edge)[0];
      assertTrue(result.isFinal());
    }

    @Test
    public void leavingBusinessAreaFromBoundaryVertexIsBlocked() {
      // V1 is inside business area, V2 is outside → exiting at V1→V2 boundary.
      // Fallback path: state starts AT the boundary vertex with no prior edge to fire
      // forwardApproachingExit. The renting branch is blocked; the walking continuation
      // comes from the corresponding BEFORE_RENTING branch.
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(BUSINESS_AREA_ZONE, false));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(BUSINESS_AREA_ZONE, true));

      var edge = streetEdge(V1, V2);
      var state = initialStateWithZones(V1, NETWORK_TIER, false, Set.of(BUSINESS_AREA_ZONE));

      var results = edge.traverse(state);

      assertEquals(0, results.length);
    }

    /**
     * When the rider starts AT the boundary vertex and traverses into a no-drop-off zone,
     * they continue riding normally. No drop-off is offered inside the zone — the
     * pre-traversal fork on the approach edge handles the drop-at-boundary case.
     */
    @Test
    public void continueRidingWhenStartingAtNoDropOffZoneBoundary() {
      // Set up boundary: V1 is outside, V2 is inside the no-drop-off zone
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, true));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, false));

      var edge = streetEdge(V1, V2);

      var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
      var editor = new StateEditor(V1, req);
      editor.beginFloatingVehicleRenting(
        RentalFormFactor.SCOOTER,
        PropulsionType.ELECTRIC,
        NETWORK_TIER,
        false
      );

      var results = edge.traverse(editor.makeState());
      assertEquals(1, results.length);
      assertEquals(RENTING_FLOATING, results[0].getVehicleRentalState());
      assertEquals(SCOOTER, results[0].getBackMode());
    }

    /**
     * When the rider approaches a no-drop-off zone boundary from one edge before, the
     * pre-traversal check forks: one branch drops at the boundary vertex (outside zone),
     * the other continues riding into the zone. The A* picks the best option based on
     * whether the destination is inside or outside the zone.
     */
    @Test
    public void forkWhenApproachingNoDropOffZone() {
      // V1→V2→V3, boundary at V2/V3. Rider approaches from V1.
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, true));
      V3.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, false));

      var approachEdge = streetEdge(V1, V2);

      var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
      var editor = new StateEditor(V1, req);
      editor.beginFloatingVehicleRenting(
        RentalFormFactor.SCOOTER,
        PropulsionType.ELECTRIC,
        NETWORK_TIER,
        false
      );

      var results = approachEdge.traverse(editor.makeState());
      assertEquals(2, results.length);

      var droppedOff = results[0];
      assertEquals(HAVE_RENTED, droppedOff.getVehicleRentalState());
      assertEquals(SCOOTER, droppedOff.getBackMode());

      var continueRiding = results[1];
      assertEquals(RENTING_FLOATING, continueRiding.getVehicleRentalState());
      assertEquals(SCOOTER, continueRiding.getBackMode());
    }

    /**
     * A committed rider approaching a no-traversal zone boundary forks: one branch drops
     * the vehicle (for walking into the zone), the other continues riding (for destinations
     * reachable without entering the zone). The fork fires on the approach edge where
     * tov is the boundary vertex (entering=true).
     */
    @Test
    public void forwardNoTraversalBoundaryForksForCommittedRider() {
      // V2 is the boundary (entering=true), V3 is inside zone (entering=false)
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, true));
      V3.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, false));

      // Approach edge: V1→V2, where tov=V2 is the boundary vertex
      var edge = streetEdge(V1, V2);
      var state = initialState(V1, NETWORK_TIER, false);

      var results = edge.traverse(state);
      assertEquals(2, results.length);

      var dropped = Arrays.stream(results)
        .filter(s -> s.getVehicleRentalState() == HAVE_RENTED)
        .findFirst();
      assertTrue(dropped.isPresent(), "should have a drop branch");

      var riding = Arrays.stream(results)
        .filter(s -> s.getVehicleRentalState() == RENTING_FLOATING)
        .findFirst();
      assertTrue(riding.isPresent(), "should have a continue-riding branch");
    }

    /**
     * The no-traversal pre-traversal should fork (drop + continue riding), not force drop.
     * The boundary vertex V2 has outgoing edges that don't enter the zone (V2→V4). The
     * rider should be able to continue riding from V2 toward V4 without dropping.
     *
     */
    @Test
    public void noTraversalBoundaryShouldForkNotForceDrop() {
      // V2 is on the no-traversal zone boundary (entering=true toward V3)
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, true));
      V3.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, false));

      var approachEdge = streetEdge(V1, V2);
      // V2→V4 exists: an outgoing edge from V2 that does NOT enter the zone
      streetEdge(V2, V4);

      var state = initialState(V1, NETWORK_TIER, false);
      var results = approachEdge.traverse(state);

      // Should fork like the no-drop-off case: one drop branch + one ride-through branch
      assertEquals(2, results.length);

      var dropped = Arrays.stream(results)
        .filter(s -> s.getVehicleRentalState() == HAVE_RENTED)
        .findFirst();
      assertTrue(dropped.isPresent(), "should have a drop branch");

      var riding = Arrays.stream(results)
        .filter(s -> s.getVehicleRentalState() == RENTING_FLOATING)
        .findFirst();
      assertTrue(riding.isPresent(), "should have a continue-riding branch");
    }

    /**
     * When a RENTING_FLOATING state starts AT a no-traversal boundary vertex (e.g., from a
     * VehicleRentalEdge pickup) and rides into the zone, the crossing edge succeeds (state lands
     * inside with the zone in currentZones) and the next edge is blocked by enforceInside.
     */
    @Test
    public void ridingIntoNoTraversalZoneIsBlockedOnNextEdge() {
      // V2 is the boundary vertex (entering=true), V3 is inside the zone (entering=false)
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, true));
      V3.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, false));

      var boundaryEdge = streetEdge(V2, V3);
      var nextEdge = streetEdge(V3, V4);
      // Start RENTING_FLOATING at V2 (as if from VehicleRentalEdge pickup)
      var state = initialState(V2, NETWORK_TIER, false);

      var crossed = boundaryEdge.traverse(state);
      assertEquals(1, crossed.length);
      assertEquals(0, nextEdge.traverse(crossed[0]).length);
    }

    /**
     * When a rider is inside a no-drop-off zone and hits a no-traversal boundary, they
     * cannot drop (drop-off banned) and cannot continue (traversal banned). This branch
     * is a dead end — the A* should use the branch that dropped outside the no-drop-off zone.
     */
    @Test
    public void noTraversalBoundaryInsideNoDropOffZoneIsDeadEnd() {
      // V2 is the no-traversal boundary (entering=true)
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, true));
      V3.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, false));

      var edge = streetEdge(V1, V2);
      // Rider is inside a no-drop-off zone (zone already in state)
      var state = initialStateWithZones(V1, NETWORK_TIER, false, Set.of(NO_DROP_OFF_ZONE_TIER));

      var results = edge.traverse(state);
      // Dead end: can't traverse (no-traversal ahead) and can't drop (inside no-drop-off zone)
      assertEquals(0, results.length);
    }

    /**
     * When a rider is inside a no-drop-off zone and has a no-traversal zone already in
     * state (from a prior boundary crossing), the traversal-ban-in-state drop is blocked.
     */
    @Test
    public void traversalBanInStateInsideNoDropOffZoneIsDeadEnd() {
      // No-drop-off zone with traversalBanned=null (unspecified) so it doesn't override
      // the no-traversal zone's traversalBanned=true via per-field precedence.
      // No-drop-off zone: dropOffBanned=true, traversalBanned=null (unspecified).
      // Higher priority (lower value) so its dropOffBanned wins per-field precedence.
      var noDropOffOnly = TestGeofencingZoneBuilder.of(NETWORK_TIER, "no-dropoff-only")
        .withGeometry(Polygons.OSLO)
        .withDropOffBanned(true)
        .build();
      // No-traversal zone: traversalBanned=true, dropOffBanned=null (unspecified).
      var noTraversalOnly = TestGeofencingZoneBuilder.of(NETWORK_TIER, "no-traverse-only")
        .withGeometry(Polygons.OSLO)
        .withTraversalBanned(true)
        .withPriority(1)
        .build();

      var edge = streetEdge(V1, V2);
      // Rider has both zones in state: can't traverse (no-traversal) and can't drop (no-drop-off)
      var state = initialStateWithZones(
        V1,
        NETWORK_TIER,
        false,
        Set.of(noDropOffOnly, noTraversalOnly)
      );

      var results = edge.traverse(state);
      assertEquals(0, results.length);
    }

    /**
     * Same scenario but triggered by a business area exit boundary: rider is inside a
     * no-drop-off zone and hits a business area boundary.
     */
    @Test
    public void businessAreaExitInsideNoDropOffZoneIsDeadEnd() {
      // V1 inside business area, V2 outside → exiting at V1→V2
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(BUSINESS_AREA_ZONE, false));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(BUSINESS_AREA_ZONE, true));

      var edge = streetEdge(V1, V2);
      var state = initialStateWithZones(
        V1,
        NETWORK_TIER,
        false,
        Set.of(NO_DROP_OFF_ZONE_TIER, BUSINESS_AREA_ZONE)
      );

      var results = edge.traverse(state);
      assertEquals(0, results.length);
    }

    /**
     * Business area exit and no-drop-off zone boundary at the same edge: the rider
     * exits the business area AND enters a no-drop-off zone during the same traversal.
     * The business area forced-drop must check post-traversal zone state.
     */
    @Test
    public void businessAreaExitEnteringNoDropOffZoneIsDeadEnd() {
      // V1 inside business area, V2 outside → exiting
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(BUSINESS_AREA_ZONE, false));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(BUSINESS_AREA_ZONE, true));
      // V1→V2 also crosses a no-drop-off zone boundary (V1 outside, V2 inside)
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, true));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, false));

      var edge = streetEdge(V1, V2);
      // Rider has business area zone in state but NOT the no-drop-off zone
      var state = initialStateWithZones(V1, NETWORK_TIER, false, Set.of(BUSINESS_AREA_ZONE));

      var results = edge.traverse(state);
      assertEquals(0, results.length);
    }

    /**
     * When a renting state already has a no-traversal zone in {@code currentZones} (e.g., from
     * a pickup inside the zone or a prior boundary crossing), subsequent traversals are blocked
     * by {@code RestrictedZoneEnforcement.enforceInside}.
     */
    @Test
    public void forwardTraversalBanWhenZoneAlreadyInState() {
      // No boundaries on vertices — the zone is already in state
      var edge = streetEdge(V1, V2);
      var state = initialStateWithZones(V1, NETWORK_TIER, false, Set.of(NO_TRAVERSAL_ZONE));

      var results = edge.traverse(state);
      assertEquals(0, results.length);
    }

    /**
     * When a rider is already inside a no-drop-off zone and approaches a second one,
     * no fork is offered — the rider continues riding through.
     */
    @Test
    public void noForkWhenAlreadyInsideNoDropOffZone() {
      // V2→V3 boundary for a second no-drop-off zone
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER_2, true));
      V3.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER_2, false));

      var edge = streetEdge(V1, V2);
      // Rider already inside the first no-drop-off zone
      var state = initialStateWithZones(V1, NETWORK_TIER, false, Set.of(NO_DROP_OFF_ZONE_TIER));

      var results = edge.traverse(state);
      assertEquals(1, results.length);
      assertEquals(RENTING_FLOATING, results[0].getVehicleRentalState());
      assertEquals(SCOOTER, results[0].getBackMode());
    }

    /**
     * A committed RENTING_FLOATING state at a no-drop-off boundary takes the drop/ride
     * fork. Generic (null-network) states only exist in arrive-by searches.
     */
    @Test
    public void committedStateForkAtNoDropOffBoundary() {
      // Boundary on V2 (entering=true) / V3 (entering=false). Edge V1→V2, tov=V2.
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, true));
      V3.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, false));

      var edge = streetEdge(V1, V2);
      var state = initialState(V1, NETWORK_TIER, false);

      var results = edge.traverse(state);
      // Drop + ride branches from the no-drop-off fork
      assertEquals(2, results.length);
      var dropped = Arrays.stream(results)
        .filter(s -> s.getVehicleRentalState() == HAVE_RENTED)
        .findFirst();
      assertTrue(dropped.isPresent());
      var riding = Arrays.stream(results)
        .filter(s -> s.getVehicleRentalState() == RENTING_FLOATING)
        .findFirst();
      assertTrue(riding.isPresent());
    }
  }

  @Nested
  class Reverse {

    @Test
    public void pickupFloatingVehicleWhenLeavingNoTraversalZone() {
      // 3-vertex graph: V3→V1→V2. Boundary at V1/V2, V2 inside no-traversal zone.
      // Boundary fork fires on V1→V2 (walking only), deferred fork fires on V3→V1.
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, true));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, false));

      var boundaryEdge = streetEdge(V1, V2);
      var deferredEdge = streetEdge(V3, V1);
      var req = makeArriveByRequest(Set.of(NETWORK_TIER), Collections.emptySet());
      var haveRentedState = makeHaveRentedState(V2, req, Set.of(NO_TRAVERSAL_ZONE));

      assertTrue(haveRentedState.getCurrentGeofencingZones().contains(NO_TRAVERSAL_ZONE));

      // Step 1: boundary fork on V1→V2 — only produces walking branch
      var boundaryStates = boundaryEdge.traverse(haveRentedState);
      assertEquals(1, boundaryStates.length);
      var walkingAtV1 = boundaryStates[0];
      assertEquals(HAVE_RENTED, walkingAtV1.getVehicleRentalState());
      assertEquals(WALK, walkingAtV1.currentMode());

      // Step 2: deferred fork on V3→V1 — produces walking + RENTING_FLOATING
      var states = deferredEdge.traverse(walkingAtV1);
      assertTrue(states.length >= 2);

      // Walking branch
      var walkingState = Arrays.stream(states)
        .filter(s -> s.getVehicleRentalState() == HAVE_RENTED)
        .findFirst()
        .get();
      assertEquals(WALK, walkingState.currentMode());

      // Renting branch (speculative pickup)
      var rentalState = Arrays.stream(states)
        .filter(s -> s.getVehicleRentalState() == RENTING_FLOATING)
        .findFirst()
        .get();
      assertEquals(SCOOTER, rentalState.currentMode());
    }

    @Test
    public void pickupFloatingVehiclesWhenStartedInNoDropOffZone() {
      // 3-vertex graph: V3→V1→V2. Boundary at V1/V2, V2 inside both zones.
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, true));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, false));
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_BIRD, true));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_BIRD, false));

      var boundaryEdge = streetEdge(V1, V2);
      var deferredEdge = streetEdge(V3, V1);
      var req = makeArriveByRequest(Collections.emptySet(), Collections.emptySet());
      var haveRentedState = makeHaveRentedState(
        V2,
        req,
        Set.of(NO_DROP_OFF_ZONE_TIER, NO_DROP_OFF_ZONE_BIRD)
      );

      // Step 1: boundary fork — walking only
      var boundaryStates = boundaryEdge.traverse(haveRentedState);
      assertEquals(1, boundaryStates.length);
      var walkingAtV1 = boundaryStates[0];
      assertEquals(HAVE_RENTED, walkingAtV1.getVehicleRentalState());

      // Step 2: deferred fork — walking + per-network + generic
      var states = deferredEdge.traverse(walkingAtV1);

      // Should have: walking + per-network committed branches + generic
      assertTrue(states.length >= 3);

      // Walking branch
      var walkState = Arrays.stream(states)
        .filter(s -> s.getVehicleRentalState() == HAVE_RENTED)
        .findFirst()
        .get();
      assertEquals(WALK, walkState.currentMode());

      // Generic renting state (null network)
      var genericState = Arrays.stream(states)
        .filter(
          s -> s.getVehicleRentalState() == RENTING_FLOATING && s.getVehicleRentalNetwork() == null
        )
        .findFirst();
      assertTrue(genericState.isPresent());
    }

    @Test
    public void pickupFloatingVehiclesWhenAllNetworksBanned() {
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, true));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, false));
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_BIRD, true));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_BIRD, false));

      var edge = streetEdge(V1, V2);
      var req = makeArriveByRequest(Collections.emptySet(), Set.of(NETWORK_TIER, NETWORK_BIRD));
      var haveRentedState = makeHaveRentedState(
        V2,
        req,
        Set.of(NO_DROP_OFF_ZONE_TIER, NO_DROP_OFF_ZONE_BIRD)
      );

      var states = edge.traverse(haveRentedState);

      // All networks banned — only walking state
      assertEquals(1, states.length);
      assertEquals(HAVE_RENTED, states[0].getVehicleRentalState());
      assertEquals(WALK, states[0].currentMode());
    }

    @Test
    public void pickupFloatingVehiclesWhenSomeNetworksBanned() {
      // 3-vertex graph: V3→V1→V2. Boundary at V1/V2, V2 inside both zones.
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, true));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, false));
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_BIRD, true));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_BIRD, false));

      var boundaryEdge = streetEdge(V1, V2);
      var deferredEdge = streetEdge(V3, V1);
      // Bird is banned, tier is allowed
      var req = makeArriveByRequest(Collections.emptySet(), Set.of(NETWORK_BIRD));
      var haveRentedState = makeHaveRentedState(
        V2,
        req,
        Set.of(NO_DROP_OFF_ZONE_TIER, NO_DROP_OFF_ZONE_BIRD)
      );

      // Step 1: boundary fork — walking only
      var boundaryStates = boundaryEdge.traverse(haveRentedState);
      assertEquals(1, boundaryStates.length);

      // Step 2: deferred fork — walking + tier (bird banned)
      var states = deferredEdge.traverse(boundaryStates[0]);

      // Walking + tier committed + generic (bird is banned)
      assertTrue(states.length >= 2);

      var tierState = Arrays.stream(states)
        .filter(s -> NETWORK_TIER.equals(s.getVehicleRentalNetwork()))
        .findFirst();
      assertTrue(tierState.isPresent());

      var birdState = Arrays.stream(states)
        .filter(s -> NETWORK_BIRD.equals(s.getVehicleRentalNetwork()))
        .findFirst();
      assertFalse(birdState.isPresent());
    }

    @Test
    public void pickupFloatingVehiclesWithAllowedNetworkFilter() {
      // 3-vertex graph: V3→V1→V2. Boundary at V1/V2, V2 inside both zones.
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, true));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, false));
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_BIRD, true));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_BIRD, false));

      var boundaryEdge = streetEdge(V1, V2);
      var deferredEdge = streetEdge(V3, V1);
      // Only tier is allowed
      var req = makeArriveByRequest(Set.of(NETWORK_TIER), Collections.emptySet());
      var haveRentedState = makeHaveRentedState(
        V2,
        req,
        Set.of(NO_DROP_OFF_ZONE_TIER, NO_DROP_OFF_ZONE_BIRD)
      );

      // Step 1: boundary fork — walking only
      var boundaryStates = boundaryEdge.traverse(haveRentedState);
      assertEquals(1, boundaryStates.length);

      // Step 2: deferred fork — walking + tier (bird not in allowed list)
      var states = deferredEdge.traverse(boundaryStates[0]);

      var tierState = Arrays.stream(states)
        .filter(s -> NETWORK_TIER.equals(s.getVehicleRentalNetwork()))
        .findFirst();
      assertTrue(tierState.isPresent());

      var birdState = Arrays.stream(states)
        .filter(s -> NETWORK_BIRD.equals(s.getVehicleRentalNetwork()))
        .findFirst();
      assertFalse(birdState.isPresent());
    }

    /**
     * Create a HAVE_RENTED walker state with pre-populated currentGeofencingZones,
     * simulating an arriveBy initial state at a destination inside geofencing zones.
     */
    private State makeHaveRentedState(
      Vertex vertex,
      StreetSearchRequest req,
      Set<GeofencingZone> zones
    ) {
      var reqWithZones = StreetSearchRequest.copyOf(req)
        .withArriveByDestinationZones(zones)
        .build();
      return State.getInitialStates(Set.of(vertex), reqWithZones)
        .stream()
        .filter(s -> s.getVehicleRentalState() == HAVE_RENTED)
        .findAny()
        .get();
    }

    @Test
    public void arriveByRentingBlockedFromEnteringNoTraversalZone() {
      // V1 inside no-traversal zone (entering=false), V2 boundary outside (entering=true). Paired.
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, false));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, true));

      var edge = streetEdge(V1, V2);
      // RENTING_FLOATING rider at V2 (tov), arrive-by traversal goes from V2 to V1 (into zone)
      var rentingState = initialState(V2, NETWORK_TIER, true);

      var states = edge.traverse(rentingState);
      assertEquals(0, states.length);
    }

    @Test
    public void arriveByRentingBlockedOnInteriorEdgeAdjacentToBoundary() {
      // V1 is boundary vertex (entering=false, inside zone). V3 is interior (no boundary).
      // Edge V1→V3: fromv=V1 has boundary, tov=V3 has none (not paired).
      // Arrive-by traversal from V3 to V1 should still be blocked — the rider would
      // enter the zone even though V3 has no boundary extension.
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, false));

      var edge = streetEdge(V1, V3);
      var rentingState = initialState(V3, NETWORK_TIER, true);

      var states = edge.traverse(rentingState);
      assertEquals(0, states.length);
    }

    @Test
    public void arriveByRentingNotBlockedForDifferentNetwork() {
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, false));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, true));

      var edge = streetEdge(V1, V2);
      // Rider on BIRD network — zone is for TIER, should not be blocked
      var rentingState = initialState(V2, NETWORK_BIRD, true);

      var states = edge.traverse(rentingState);
      assertTrue(states.length > 0);
    }

    @Test
    public void arriveByGenericRentingPassesThroughNoTraversalZoneWithCommittedNetwork() {
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, false));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, true));

      var edge = streetEdge(V1, V2);
      // Generic state (null network) passes through but records the zone's network in
      // committedNetworks — a committed branch for that network could never use this path.
      var rentingState = initialState(V2, null, true);

      var states = edge.traverse(rentingState);
      assertEquals(1, states.length);
      assertTrue(states[0].getCommittedNetworks().contains(NETWORK_TIER));
    }

    /**
     * When a HAVE_RENTED walker has no zones in state, the arriveBy boundary fork trigger
     * returns false (early return). Normal walking traversal occurs.
     */
    @Test
    public void arriveByBoundaryForkNoOpWhenNoZonesInState() {
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, true));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, false));

      var edge = streetEdge(V1, V2);
      var req = makeArriveByRequest(Collections.emptySet(), Collections.emptySet());
      // HAVE_RENTED walker with EMPTY zones
      var haveRentedState = makeHaveRentedState(V2, req, Collections.emptySet());

      var states = edge.traverse(haveRentedState);
      assertEquals(1, states.length);
      assertEquals(HAVE_RENTED, states[0].getVehicleRentalState());
      assertEquals(WALK, states[0].currentMode());
    }

    /**
     * When a walker exits one zone but remains inside another (overlapping zones),
     * the deferred fork only creates branches for the exited zone's network.
     */
    @Test
    public void deferredForkOnlyForksExitedZoneNetworks() {
      // Boundary at V1/V2 for TIER zone only (not BIRD)
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, true));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, false));

      var boundaryEdge = streetEdge(V1, V2);
      var deferredEdge = streetEdge(V4, V1);
      var req = makeArriveByRequest(Collections.emptySet(), Collections.emptySet());
      // Walker inside both TIER and BIRD zones
      var haveRentedState = makeHaveRentedState(
        V2,
        req,
        Set.of(NO_DROP_OFF_ZONE_TIER, NO_DROP_OFF_ZONE_BIRD)
      );

      // Step 1: boundary fork — exits TIER zone, still inside BIRD
      var boundaryStates = boundaryEdge.traverse(haveRentedState);
      assertEquals(1, boundaryStates.length);
      var walkingAtV1 = boundaryStates[0];
      // TIER was exited, BIRD remains
      assertTrue(walkingAtV1.getCurrentGeofencingZones().contains(NO_DROP_OFF_ZONE_BIRD));
      assertFalse(walkingAtV1.getCurrentGeofencingZones().contains(NO_DROP_OFF_ZONE_TIER));

      // Step 2: deferred fork — should fork TIER only (not BIRD)
      var states = deferredEdge.traverse(walkingAtV1);
      assertTrue(states.length >= 2);

      // TIER committed branch exists
      var tierState = Arrays.stream(states)
        .filter(s -> NETWORK_TIER.equals(s.getVehicleRentalNetwork()))
        .findFirst();
      assertTrue(tierState.isPresent());

      // No BIRD committed branch (BIRD zone was not exited)
      var birdState = Arrays.stream(states)
        .filter(s -> NETWORK_BIRD.equals(s.getVehicleRentalNetwork()))
        .findFirst();
      assertFalse(birdState.isPresent());
    }

    /**
     * Adjacent no-drop-off zones (same network): walker exits zone A and enters zone B
     * at the same boundary. The deferred fork should NOT create renting branches because
     * the walker is still inside a no-drop-off zone (B) for the same network.
     *
     * This is the scenario that caused the Tøyen bug: the deferred fork's post-traversal
     * veto failed because the traversed edge exited zone B, removing it from the editor's
     * zone state. The pre-traversal veto on s0's zones catches this.
     */
    @Test
    public void deferredForkBlockedByAdjacentSameNetworkNoDropOffZone() {
      // Zone A boundary at V1/V2: V1 outside (entering=true), V2 inside (entering=false)
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, true));
      V2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER, false));
      // Zone B (same network, adjacent) boundary at V1/V3: V1 inside (entering=false),
      // V3 outside (entering=true). V1 is the shared vertex — inside B, outside A.
      // Both sides of the boundary edge V3→V1 need extensions for updateGeofencingZones.
      V3.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER_2, true));
      V1.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE_TIER_2, false));

      var boundaryEdge = streetEdge(V1, V2);
      // V3→V1 edge: the deferred fork will fire here. V3 is outside zone B.
      var deferredEdge = streetEdge(V3, V1);

      var req = makeArriveByRequest(Collections.emptySet(), Collections.emptySet());
      // Initialize with both zones: zone A (destination) and zone B (adjacent, entered
      // because destination vertex is near both zone boundaries in real geometry).
      var haveRentedInBothZones = makeHaveRentedState(
        V2,
        req,
        Set.of(NO_DROP_OFF_ZONE_TIER, NO_DROP_OFF_ZONE_TIER_2)
      );

      // Step 1: boundary fork on V1→V2 — exits zone A only (zone B has no pair on V2)
      var boundaryStates = boundaryEdge.traverse(haveRentedInBothZones);
      assertEquals(1, boundaryStates.length);
      var walkingAtV1 = boundaryStates[0];
      assertFalse(walkingAtV1.getCurrentGeofencingZones().contains(NO_DROP_OFF_ZONE_TIER));
      assertTrue(walkingAtV1.getCurrentGeofencingZones().contains(NO_DROP_OFF_ZONE_TIER_2));

      // Step 2: deferred fork on V3→V1 — should NOT create renting branches
      // because the walker is inside zone B (same network, no-drop-off).
      // Without the pre-traversal veto fix, this edge would exit zone B during
      // traversal, and the post-traversal veto would miss it.
      var states = deferredEdge.traverse(walkingAtV1);
      assertEquals(1, states.length);
      assertEquals(HAVE_RENTED, states[0].getVehicleRentalState());
    }

    private static StreetSearchRequest makeArriveByRequest(
      Set<String> allowedNetworks,
      Set<String> bannedNetworks
    ) {
      return StreetSearchRequest.of()
        .withScooter(b ->
          b.withRental(r ->
            r.withAllowedNetworks(allowedNetworks).withBannedNetworks(bannedNetworks)
          )
        )
        .withMode(StreetMode.SCOOTER_RENTAL)
        .withArriveBy(true)
        .build();
    }
  }

  private State[] traverseFromV1(StreetEdge edge) {
    var state = initialState(V1, NETWORK_TIER, false);
    return edge.traverse(state);
  }

  private State initialState(Vertex startVertex, String network, boolean arriveBy) {
    var req = StreetSearchRequest.of()
      .withMode(StreetMode.SCOOTER_RENTAL)
      .withArriveBy(arriveBy)
      .build();
    var editor = new StateEditor(startVertex, req);
    editor.beginFloatingVehicleRenting(
      RentalFormFactor.SCOOTER,
      PropulsionType.ELECTRIC,
      network,
      false
    );
    return editor.makeState();
  }

  private State initialStateWithZones(
    Vertex startVertex,
    String network,
    boolean arriveBy,
    Set<GeofencingZone> zones
  ) {
    var req = StreetSearchRequest.of()
      .withMode(StreetMode.SCOOTER_RENTAL)
      .withArriveBy(arriveBy)
      .build();
    var editor = new StateEditor(startVertex, req);
    editor.beginFloatingVehicleRenting(
      RentalFormFactor.SCOOTER,
      PropulsionType.ELECTRIC,
      network,
      false
    );
    editor.initializeGeofencingZones(zones);
    return editor.makeState();
  }

  private State initialStateWithCommittedNetworks(
    Vertex startVertex,
    Set<String> committedNetworks
  ) {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var editor = new StateEditor(startVertex, req);
    editor.beginFloatingVehicleRenting(
      RentalFormFactor.SCOOTER,
      PropulsionType.ELECTRIC,
      null,
      false
    );
    for (var network : committedNetworks) {
      editor.addCommittedNetwork(network);
    }
    return editor.makeState();
  }
}
