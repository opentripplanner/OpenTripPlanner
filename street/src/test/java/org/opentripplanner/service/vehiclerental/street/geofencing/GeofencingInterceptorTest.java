package org.opentripplanner.service.vehiclerental.street.geofencing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelFactory.streetEdge;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.TestGeofencingZoneBuilder;
import org.opentripplanner.street.geometry.Polygons;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.VehicleRentalState;

class GeofencingInterceptorTest {

  static final String NETWORK = "tier";

  static final GeofencingZone NO_DROP_OFF_ZONE = TestGeofencingZoneBuilder.of(NETWORK, "zone-ndo")
    .withGeometry(Polygons.OSLO_FROGNER_PARK)
    .noDropOff()
    .build();

  static final GeofencingZone NO_TRAVERSAL_ZONE = TestGeofencingZoneBuilder.of(NETWORK, "zone-nt")
    .withGeometry(Polygons.OSLO)
    .noTraversal()
    .build();

  StreetVertex v1, v2, v3, v4;
  StreetEdge setupEdge, testEdge, testEdge2;
  EdgeTraversal edgeTraversal;

  @BeforeEach
  void setUp() {
    v1 = intersectionVertex(1, 1);
    v2 = intersectionVertex(2, 2);
    v3 = intersectionVertex(3, 3);
    v4 = intersectionVertex(4, 4);
    setupEdge = streetEdge(v1, v2);
    testEdge = streetEdge(v2, v3);
    testEdge2 = streetEdge(v3, v4);
    edgeTraversal = (s0, mode) -> {
      if (s0.getVertex().equals(v2)) {
        return s0.edit(testEdge);
      }
      return s0.edit(testEdge2);
    };
  }

  private State createRentingState(GeofencingZone... zones) {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var s0 = new State(v1, req);
    var editor = s0.edit(setupEdge);
    editor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER_STANDING, null, NETWORK, false);
    if (zones.length > 0) {
      editor.initializeGeofencingZones(Set.of(zones));
    }
    return editor.makeState();
  }

  private State createStationRentingState(GeofencingZone... zones) {
    var req = StreetSearchRequest.of().withMode(StreetMode.BIKE_RENTAL).build();
    var s0 = new State(v1, req);
    var editor = s0.edit(setupEdge);
    editor.beginVehicleRentingAtStation(RentalFormFactor.BICYCLE, null, NETWORK, false, false);
    if (zones.length > 0) {
      editor.initializeGeofencingZones(Set.of(zones));
    }
    return editor.makeState();
  }

  @Nested
  class ForwardEnteringWithBoundaries {

    @Test
    void forksWhenEnteringNoDropOffZone() {
      // Forward: tov has entering=true (approach boundary), fromv has nothing
      var fromBounds = List.<GeofencingBoundaryExtension>of();
      var toBounds = List.of(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, true));

      var state = createRentingState();
      var result = GeofencingInterceptor.apply(state, fromBounds, toBounds, edgeTraversal);

      assertNotNull(result, "should trigger enforcement");
      assertTrue(result.length >= 1, "should produce at least one branch");

      boolean hasDropped = false;
      boolean hasRiding = false;
      for (var s : result) {
        if (s.getVehicleRentalState() == VehicleRentalState.HAVE_RENTED) {
          hasDropped = true;
        }
        if (s.getVehicleRentalState() == VehicleRentalState.RENTING_FLOATING) {
          hasRiding = true;
        }
      }
      assertTrue(hasDropped, "should have a drop branch");
      assertTrue(hasRiding, "should have a ride-through branch");
    }

    @Test
    void forksWhenEnteringNoTraversalZone() {
      // Forward: tov has entering=true (approach boundary)
      var fromBounds = List.<GeofencingBoundaryExtension>of();
      var toBounds = List.of(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, true));

      var state = createRentingState();
      var result = GeofencingInterceptor.apply(state, fromBounds, toBounds, edgeTraversal);

      assertNotNull(result, "should trigger enforcement");
      assertEquals(2, result.length, "should fork into drop + ride");
    }
  }

  @Nested
  class NoEnforcementNeeded {

    @Test
    void passesWhenNoBoundaries() {
      var state = createRentingState();
      var result = GeofencingInterceptor.apply(state, List.of(), List.of(), edgeTraversal);

      assertNull(result, "no boundaries → no enforcement");
    }

    @Test
    void passesWhenBoundaryNotPaired() {
      // fromv has boundary but tov doesn't have the pair
      var fromBounds = List.of(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, true));
      var toBounds = List.<GeofencingBoundaryExtension>of();

      var state = createRentingState();
      var result = GeofencingInterceptor.apply(state, fromBounds, toBounds, edgeTraversal);

      assertNull(result, "unpaired boundary → no enforcement");
    }

    @Test
    void passesWhenZoneNetworkDoesNotMatchState() {
      var otherNetworkZone = TestGeofencingZoneBuilder.of("bird", "zone")
        .withGeometry(Polygons.OSLO)
        .noDropOff()
        .build();
      var fromBounds = List.of(new GeofencingBoundaryExtension(otherNetworkZone, true));
      var toBounds = List.of(new GeofencingBoundaryExtension(otherNetworkZone, false));

      // state has network "tier", zone has network "bird"
      var state = createRentingState();
      var result = GeofencingInterceptor.apply(state, fromBounds, toBounds, edgeTraversal);

      assertNull(result, "zone network doesn't match state → no enforcement");
    }
  }

  @Nested
  class ForwardBusinessAreaExit {

    static final GeofencingZone BUSINESS_AREA = TestGeofencingZoneBuilder.of(NETWORK, "business")
      .withGeometry(Polygons.OSLO)
      .asBusinessArea()
      .build();

    @Test
    void dropsAtBoundaryVertexInsideBA() {
      // tov has entering=false (exit boundary, inside BA) → enforcement triggers here
      var fromBounds = List.<GeofencingBoundaryExtension>of();
      var toBounds = List.of(new GeofencingBoundaryExtension(BUSINESS_AREA, false));

      var state = createRentingState(BUSINESS_AREA);
      var result = GeofencingInterceptor.apply(state, fromBounds, toBounds, edgeTraversal);

      assertNotNull(result, "should trigger BA exit enforcement");
      assertEquals(1, result.length, "should produce one drop branch");
      assertEquals(
        VehicleRentalState.HAVE_RENTED,
        result[0].getVehicleRentalState(),
        "should have dropped vehicle"
      );
    }
  }

  @Nested
  class ArriveByBusinessAreaBoundary {

    static final GeofencingZone BUSINESS_AREA = TestGeofencingZoneBuilder.of(NETWORK, "business")
      .withGeometry(Polygons.OSLO)
      .asBusinessArea()
      .build();

    private State createArriveByHaveRentedState(GeofencingZone... zones) {
      // Build a HAVE_RENTED state compatible with arrive-by. Use forward rental
      // transitions to create the state (arriveBy on the request only matters for
      // the interceptor's dispatch, not for state construction in tests).
      // The trick: use a non-arrive-by request for construction, then create a
      // fresh State at the target vertex with the arrive-by request and copy
      // the rental state by traversing an edge.
      var forwardReq = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
      var s0 = new State(v1, forwardReq);
      var rentEditor = s0.edit(setupEdge);
      rentEditor.beginFloatingVehicleRenting(
        RentalFormFactor.SCOOTER_STANDING,
        null,
        NETWORK,
        false
      );
      var renting = rentEditor.makeState();
      var dropEditor = renting.edit(testEdge);
      dropEditor.dropFloatingVehicle(RentalFormFactor.SCOOTER_STANDING, null, NETWORK, false);
      if (zones.length > 0) {
        dropEditor.initializeGeofencingZones(Set.of(zones));
      }
      // Ensure rental chain is valid (not used directly)
      dropEditor.makeState();

      // Create an arrive-by HAVE_RENTED state via State.getInitialStates
      var arriveByReq = StreetSearchRequest.of()
        .withMode(StreetMode.SCOOTER_RENTAL)
        .withArriveBy(true)
        .build();
      return State.getInitialStates(Set.of(v3), arriveByReq)
        .stream()
        .filter(st -> st.getVehicleRentalState() == VehicleRentalState.HAVE_RENTED)
        .findFirst()
        .orElseThrow();
    }

    @Test
    void enforcesWhenWalkerExitsBA() {
      // Real time: edge v2→v3 exits BA (fromv inside, tov outside). In arrive-by, the walker
      // is going backward, so the walker exited BA at this edge in real time — should trigger
      // walking branch via WalkerBoundaryHandler → arriveByAtBoundary.
      var fromBounds = List.of(new GeofencingBoundaryExtension(BUSINESS_AREA, false));
      var toBounds = List.of(new GeofencingBoundaryExtension(BUSINESS_AREA, true));

      // State at v3 traverses testEdge (v2→v3) backward — valid for arrive-by
      EdgeTraversal arriveByTraversal = (s0, mode) -> s0.edit(testEdge);
      var state = createArriveByHaveRentedState();
      var result = GeofencingInterceptor.apply(state, fromBounds, toBounds, arriveByTraversal);

      assertNotNull(result, "walker exits BA → should trigger walking branch");
      assertEquals(1, result.length, "should produce walking-only branch");
      assertEquals(
        VehicleRentalState.HAVE_RENTED,
        result[0].getVehicleRentalState(),
        "should remain HAVE_RENTED walker"
      );
    }

    @Test
    void passesWhenWalkerEntersBA() {
      // Real time: edge v2→v3 enters BA (fromv outside, tov inside). Walker entered BA in
      // real time — not a rental drop point (drop must be inside BA). No enforcement.
      var fromBounds = List.of(new GeofencingBoundaryExtension(BUSINESS_AREA, true));
      var toBounds = List.of(new GeofencingBoundaryExtension(BUSINESS_AREA, false));

      EdgeTraversal arriveByTraversal = (s0, mode) -> s0.edit(testEdge);
      var state = createArriveByHaveRentedState(BUSINESS_AREA);
      var result = GeofencingInterceptor.apply(state, fromBounds, toBounds, arriveByTraversal);

      assertNull(result, "walker enters BA in real time → no enforcement");
    }
  }

  @Nested
  class EnforceInside {

    @Test
    void blocksRentingStateInsideNoTraversalZone() {
      var state = createRentingState(NO_TRAVERSAL_ZONE);
      var result = GeofencingInterceptor.apply(state, List.of(), List.of(), edgeTraversal);

      assertNotNull(result);
      assertEquals(0, result.length, "should block — state is inside no-traversal zone");
    }

    @Test
    void blocksStationRentalInsideNoTraversalZone() {
      var state = createStationRentingState(NO_TRAVERSAL_ZONE);
      var result = GeofencingInterceptor.apply(state, List.of(), List.of(), edgeTraversal);

      assertNotNull(result);
      assertEquals(0, result.length, "station rental inside no-traversal zone must be blocked");
    }

    @Test
    void blocksWhenTraversalAndDropOffBothBanned() {
      // Use zones that each specify only one field (null for the other) so per-field
      // precedence resolves each field independently.
      var noTraversalOnly = TestGeofencingZoneBuilder.of(NETWORK, "nt-only")
        .withGeometry(Polygons.OSLO)
        .withTraversalBanned(true)
        .build();
      var noDropOffOnly = TestGeofencingZoneBuilder.of(NETWORK, "ndo-only")
        .withGeometry(Polygons.OSLO_FROGNER_PARK)
        .withDropOffBanned(true)
        .withPriority(1)
        .build();
      var state = createRentingState(noTraversalOnly, noDropOffOnly);
      var result = GeofencingInterceptor.apply(state, List.of(), List.of(), edgeTraversal);

      assertNotNull(result);
      assertEquals(0, result.length, "should block — traversal-banned zone in current set");
    }
  }
}
