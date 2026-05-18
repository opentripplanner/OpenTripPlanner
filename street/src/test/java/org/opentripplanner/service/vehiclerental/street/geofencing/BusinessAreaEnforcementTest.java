package org.opentripplanner.service.vehiclerental.street.geofencing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class BusinessAreaEnforcementTest {

  static final String NETWORK = "tier";

  static final GeofencingZone BUSINESS_AREA = TestGeofencingZoneBuilder.of(NETWORK, "business")
    .withGeometry(Polygons.OSLO)
    .asBusinessArea()
    .build();

  // No-drop-off zone with higher priority (lower number) so it wins over business area
  static final GeofencingZone NO_DROP_OFF_ZONE = TestGeofencingZoneBuilder.of(NETWORK, "zone-ndo")
    .withGeometry(Polygons.OSLO_FROGNER_PARK)
    .noDropOff()
    .build();

  static final BusinessAreaEnforcement ENFORCEMENT = BusinessAreaEnforcement.INSTANCE;

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
    edgeTraversal = (s0, mode) -> s0.edit(testEdge);
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
  class ForwardEntering {

    @Test
    void passesThrough() {
      var state = createRentingState(BUSINESS_AREA);
      var result = ENFORCEMENT.evaluate(BUSINESS_AREA, true, false, state, edgeTraversal);
      assertNull(result, "entering business area should pass through");
    }
  }

  @Nested
  class ForwardExiting {

    @Test
    void forceDropAtBoundary() {
      var state = createRentingState(BUSINESS_AREA);
      var result = ENFORCEMENT.evaluate(BUSINESS_AREA, false, false, state, edgeTraversal);

      assertNotNull(result);
      assertEquals(1, result.length, "should produce one drop branch");
      assertEquals(
        VehicleRentalState.HAVE_RENTED,
        result[0].getVehicleRentalState(),
        "should have dropped vehicle"
      );
    }

    @Test
    void blockedWhenDropOffBanned() {
      // Only NO_DROP_OFF_ZONE in state (no competing BUSINESS_AREA zone)
      var state = createRentingState(NO_DROP_OFF_ZONE);
      var result = ENFORCEMENT.evaluate(BUSINESS_AREA, false, false, state, edgeTraversal);

      assertNotNull(result);
      assertEquals(0, result.length, "should return empty — drop-off banned");
    }

    @Test
    void blockedByPostTraversalVeto() {
      // Set up boundary extensions so traversing v2→v3 enters a no-drop-off zone
      v2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, true));
      v3.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, false));

      // EdgeTraversal that updates zones during traversal
      EdgeTraversal zoneUpdatingTraversal = (s0, mode) -> {
        var editor = s0.edit(testEdge);
        editor.updateGeofencingZones(v2, v3, false);
        return editor;
      };

      // State starts without no-drop-off zone — it enters one during traversal
      var state = createRentingState();
      var result = ENFORCEMENT.evaluate(BUSINESS_AREA, false, false, state, zoneUpdatingTraversal);

      assertNotNull(result);
      assertEquals(0, result.length, "should block — post-traversal entered no-drop-off zone");
    }

    @Test
    void passesForNonRentingState() {
      var state = createHaveRentedState(BUSINESS_AREA);
      var result = ENFORCEMENT.evaluate(BUSINESS_AREA, false, false, state, edgeTraversal);
      assertNull(result, "HAVE_RENTED should pass — not renting");
    }
  }

  @Nested
  class StationRentalForward {

    @Test
    void blockedExitingBusinessArea() {
      var state = createStationRentingState(BUSINESS_AREA);
      var result = ENFORCEMENT.evaluate(BUSINESS_AREA, false, false, state, edgeTraversal);

      assertNotNull(result);
      assertEquals(
        0,
        result.length,
        "station rentals can't drop mid-street to exit BA — must block"
      );
    }

    @Test
    void blockedExitingAtBoundary() {
      var state = createStationRentingState(BUSINESS_AREA);
      var result = ENFORCEMENT.evaluateForwardExitingAtBoundary(state, edgeTraversal);

      assertNotNull(result);
      assertEquals(0, result.length, "station rentals can't drop at BA boundary — must block");
    }

    @Test
    void passesEnteringBusinessArea() {
      var state = createStationRentingState();
      var result = ENFORCEMENT.evaluate(BUSINESS_AREA, true, false, state, edgeTraversal);

      assertNull(result, "entering BA is fine for any rental type");
    }
  }

  @Nested
  class ArriveByEntering {

    @Test
    void passesThrough() {
      var state = createRentingState(BUSINESS_AREA);
      var result = ENFORCEMENT.evaluate(BUSINESS_AREA, true, true, state, edgeTraversal);
      assertNull(result, "entering business area in arriveBy should pass");
    }
  }

  @Nested
  class ArriveByExiting {

    @Test
    void walkingBranchForHaveRented() {
      // HAVE_RENTED state ends up at v3 (dropped on testEdge v2→v3),
      // so the edge traversal must use testEdge2 (v3→v4)
      EdgeTraversal arriveByTraversal = (s0, mode) -> s0.edit(testEdge2);
      var state = createHaveRentedState(BUSINESS_AREA);
      var result = ENFORCEMENT.evaluate(BUSINESS_AREA, false, true, state, arriveByTraversal);

      // HAVE_RENTED walker at business area boundary produces walking branch only.
      // Renting branches are deferred to the next edge by DeferredForkHandler.
      assertNotNull(result, "should produce walking branch for HAVE_RENTED");
      assertEquals(1, result.length, "should produce exactly one walking branch");
      assertEquals(
        VehicleRentalState.HAVE_RENTED,
        result[0].getVehicleRentalState(),
        "should remain HAVE_RENTED"
      );
    }

    @Test
    void passesForRentingState() {
      // Renting state reaching arriveBy exiting → evaluateArriveByExiting checks HAVE_RENTED
      var state = createRentingState(BUSINESS_AREA);
      var result = ENFORCEMENT.evaluate(BUSINESS_AREA, false, true, state, edgeTraversal);

      // Renting states are not HAVE_RENTED, so evaluateArriveByExiting returns null
      assertNull(result, "renting state arriveBy exiting should return null");
    }
  }
}
