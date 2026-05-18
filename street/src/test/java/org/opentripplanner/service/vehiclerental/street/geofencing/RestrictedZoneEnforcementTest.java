package org.opentripplanner.service.vehiclerental.street.geofencing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelFactory.streetEdge;

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

class RestrictedZoneEnforcementTest {

  static final String NETWORK = "tier";

  static final GeofencingZone NO_TRAVERSAL_ZONE = TestGeofencingZoneBuilder.of(NETWORK, "zone-nt")
    .withGeometry(Polygons.OSLO)
    .noTraversal()
    .build();

  static final GeofencingZone NO_DROP_OFF_ZONE = TestGeofencingZoneBuilder.of(NETWORK, "zone-ndo")
    .withGeometry(Polygons.OSLO_FROGNER_PARK)
    .noDropOff()
    .build();

  static final RestrictedZoneEnforcement ENFORCEMENT = RestrictedZoneEnforcement.INSTANCE;

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
    // The enforcement traverses from the state's current vertex
    edgeTraversal = (s0, mode) -> {
      // Pick the edge that starts at the state's vertex
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

  private State createHaveRentedState(GeofencingZone... zones) {
    // Build a HAVE_RENTED state: rent on setupEdge, drop off on testEdge
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var s0 = new State(v1, req);
    var rentEditor = s0.edit(setupEdge);
    rentEditor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER_STANDING, null, NETWORK, false);
    var renting = rentEditor.makeState();
    var dropEditor = renting.edit(testEdge);
    dropEditor.dropFloatingVehicle(RentalFormFactor.SCOOTER_STANDING, null, NETWORK, false);
    if (zones.length > 0) {
      dropEditor.initializeGeofencingZones(Set.of(zones));
    }
    return dropEditor.makeState();
  }

  @Nested
  class ForwardEnteringNoTraversal {

    @Test
    void forkDropAndRide() {
      var state = createRentingState();
      var result = ENFORCEMENT.evaluate(NO_TRAVERSAL_ZONE, true, false, state, edgeTraversal);

      assertNotNull(result, "should produce a result");
      assertEquals(2, result.length, "should fork into drop + ride branches");

      var hasDropped = false;
      var hasRiding = false;
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
    void blockedWhenDropOffAlsoBanned() {
      var state = createRentingState(NO_DROP_OFF_ZONE);
      var result = ENFORCEMENT.evaluate(NO_TRAVERSAL_ZONE, true, false, state, edgeTraversal);

      assertNotNull(result);
      assertEquals(0, result.length, "should return empty — dead end");
    }
  }

  @Nested
  class ForwardEnteringNoDropOff {

    @Test
    void forkDropAndRide() {
      var state = createRentingState();
      var result = ENFORCEMENT.evaluate(NO_DROP_OFF_ZONE, true, false, state, edgeTraversal);

      assertNotNull(result, "should produce a result");
      assertTrue(result.length >= 1, "should have at least one branch");

      var hasDropped = false;
      var hasRiding = false;
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
    void passWhenAlreadyInsideNoDropOffZone() {
      var state = createRentingState(NO_DROP_OFF_ZONE);
      var result = ENFORCEMENT.evaluate(NO_DROP_OFF_ZONE, true, false, state, edgeTraversal);

      assertNull(result, "should pass through — already inside restricted zone");
    }

    @Test
    void postTraversalVetoDiscardsDropBranch() {
      // Edge traversal enters an adjacent no-drop-off zone during the edge
      v2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, true));
      v3.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, false));

      EdgeTraversal zoneUpdatingTraversal = (s0, mode) -> {
        var editor = s0.edit(testEdge);
        editor.updateGeofencingZones(v2, v3, false);
        return editor;
      };

      var state = createRentingState();
      var result = ENFORCEMENT.evaluate(
        NO_DROP_OFF_ZONE,
        true,
        false,
        state,
        zoneUpdatingTraversal
      );

      assertNotNull(result);
      // Drop branch should be vetoed, only ride-through should remain
      for (var s : result) {
        if (s.getVehicleRentalState() == VehicleRentalState.HAVE_RENTED) {
          fail("drop branch should be vetoed by post-traversal check");
        }
      }
    }
  }

  @Nested
  class ForwardExiting {

    @Test
    void passesThrough() {
      var state = createRentingState(NO_DROP_OFF_ZONE);
      var result = ENFORCEMENT.evaluate(NO_DROP_OFF_ZONE, false, false, state, edgeTraversal);

      assertNull(result, "forward exiting restricted zone should pass through");
    }
  }

  @Nested
  class StationRentalForwardEntering {

    @Test
    void blockedAtNoTraversalZone() {
      var state = createStationRentingState();
      var result = ENFORCEMENT.evaluate(NO_TRAVERSAL_ZONE, true, false, state, edgeTraversal);

      assertNotNull(result);
      assertEquals(0, result.length, "station rentals can't drop mid-street — must block");
    }

    @Test
    void passesAtNoDropOffZone() {
      var state = createStationRentingState();
      var result = ENFORCEMENT.evaluate(NO_DROP_OFF_ZONE, true, false, state, edgeTraversal);

      assertNull(result, "no-drop-off is irrelevant for station rentals — pass through");
    }
  }

  @Nested
  class ArriveByEnteringNoTraversal {

    @Test
    void blocksCommittedRentingState() {
      var state = createRentingState();
      var result = ENFORCEMENT.evaluate(NO_TRAVERSAL_ZONE, true, true, state, edgeTraversal);

      assertNotNull(result);
      assertEquals(0, result.length, "should block committed state entering no-traversal zone");
    }

    @Test
    void passesForNonRentingState() {
      var state = createHaveRentedState();
      var result = ENFORCEMENT.evaluate(NO_TRAVERSAL_ZONE, true, true, state, edgeTraversal);

      assertNull(result, "should pass — HAVE_RENTED is not a renting state");
    }

    @Test
    void passesForNullNetworkRentingState() {
      // Generic (null-network) renting state — not a committed state
      var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
      var s0 = new State(v1, req);
      var editor = s0.edit(setupEdge);
      editor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER_STANDING, null, null, false);
      var state = editor.makeState();

      var result = ENFORCEMENT.evaluate(NO_TRAVERSAL_ZONE, true, true, state, edgeTraversal);
      assertNull(result, "null-network renting state should pass — not committed");
    }
  }

  @Nested
  class ArriveByEnteringNoDropOff {

    @Test
    void passesThrough() {
      var state = createRentingState();
      var result = ENFORCEMENT.evaluate(NO_DROP_OFF_ZONE, true, true, state, edgeTraversal);
      assertNull(result, "no-drop-off zone should not block entering in arriveBy");
    }
  }

  @Nested
  class ArriveByExiting {

    @Test
    void producesWalkingBranchForHaveRented() {
      var state = createHaveRentedState(NO_DROP_OFF_ZONE);
      var result = ENFORCEMENT.evaluate(NO_DROP_OFF_ZONE, false, true, state, edgeTraversal);

      assertNotNull(result, "should produce walking branch");
      assertEquals(1, result.length);
      assertEquals(VehicleRentalState.HAVE_RENTED, result[0].getVehicleRentalState());
    }

    @Test
    void passesForRentingState() {
      var state = createRentingState(NO_DROP_OFF_ZONE);
      var result = ENFORCEMENT.evaluate(NO_DROP_OFF_ZONE, false, true, state, edgeTraversal);

      assertNull(result, "should pass — renting states don't trigger arriveBy exit fork");
    }
  }
}
