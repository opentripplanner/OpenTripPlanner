package org.opentripplanner.service.vehiclerental.street.geofencing;

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

class DeferredForkHandlerTest {

  static final String NETWORK = "tier";

  static final GeofencingZone NO_DROP_OFF_ZONE = TestGeofencingZoneBuilder.of(NETWORK, "zone-ndo")
    .withGeometry(Polygons.OSLO)
    .noDropOff()
    .build();

  static final GeofencingZone NO_TRAVERSAL_ZONE = TestGeofencingZoneBuilder.of(NETWORK, "zone-nt")
    .withGeometry(Polygons.OSLO)
    .noTraversal()
    .build();

  static final GeofencingZone BUSINESS_AREA = TestGeofencingZoneBuilder.of(NETWORK, "business")
    .withGeometry(Polygons.OSLO)
    .asBusinessArea()
    .build();

  StreetVertex v1, v2, v3, v4, v5;
  StreetEdge edge1, edge2, edge3, edge4;

  @BeforeEach
  void setUp() {
    v1 = intersectionVertex(1, 1);
    v2 = intersectionVertex(2, 2);
    v3 = intersectionVertex(3, 3);
    v4 = intersectionVertex(4, 4);
    v5 = intersectionVertex(5, 5);
    edge1 = streetEdge(v1, v2);
    edge2 = streetEdge(v2, v3);
    edge3 = streetEdge(v3, v4);
    edge4 = streetEdge(v4, v5);
  }

  /**
   * Build a HAVE_RENTED state where backState had the given zone but current state doesn't.
   * This simulates a walker that just exited a restricted zone.
   *
   * Chain: v1 →(rent)→ v2 →(drop + set zones)→ v3 →(traverse, zones cleared)→ v4
   */
  private State createHaveRentedWithZoneExit(GeofencingZone zone) {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();

    var s0 = new State(v1, req);
    var rentEditor = s0.edit(edge1);
    rentEditor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER_STANDING, null, NETWORK, false);
    var renting = rentEditor.makeState();

    var dropEditor = renting.edit(edge2);
    dropEditor.dropFloatingVehicle(RentalFormFactor.SCOOTER_STANDING, null, NETWORK, false);
    dropEditor.initializeGeofencingZones(Set.of(zone));
    var insideZone = dropEditor.makeState();

    // Traverse edge3 — zones cleared (exited zone)
    var exitEditor = insideZone.edit(edge3);
    exitEditor.initializeGeofencingZones(Set.of());
    return exitEditor.makeState();
  }

  /**
   * Build a HAVE_RENTED state where s0 gained a BA zone that backState didn't have.
   * This simulates a walker that just entered a BA in arrive-by (= exited in forward time).
   *
   * Chain: v1 →(rent)→ v2 →(drop, no zones)→ v3 →(traverse, BA zone gained)→ v4
   */
  private State createHaveRentedWithZoneGain(GeofencingZone zone) {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();

    var s0 = new State(v1, req);
    var rentEditor = s0.edit(edge1);
    rentEditor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER_STANDING, null, NETWORK, false);
    var renting = rentEditor.makeState();

    var dropEditor = renting.edit(edge2);
    dropEditor.dropFloatingVehicle(RentalFormFactor.SCOOTER_STANDING, null, NETWORK, false);
    // backState has no zones (outside BA)
    var outsideBA = dropEditor.makeState();

    // Traverse edge3 — zones gained (entered BA)
    var enterEditor = outsideBA.edit(edge3);
    enterEditor.initializeGeofencingZones(Set.of(zone));
    return enterEditor.makeState();
  }

  /**
   * Build a HAVE_RENTED state where back and current have the same zones (no change).
   */
  private State createHaveRentedNoZoneChange() {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var s0 = new State(v1, req);
    var rentEditor = s0.edit(edge1);
    rentEditor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER_STANDING, null, NETWORK, false);
    var renting = rentEditor.makeState();
    var dropEditor = renting.edit(edge2);
    dropEditor.dropFloatingVehicle(RentalFormFactor.SCOOTER_STANDING, null, NETWORK, false);
    dropEditor.initializeGeofencingZones(Set.of(NO_DROP_OFF_ZONE));
    var insideZone = dropEditor.makeState();

    // Traverse but keep the same zones
    var traverseEditor = insideZone.edit(edge3);
    traverseEditor.initializeGeofencingZones(Set.of(NO_DROP_OFF_ZONE));
    return traverseEditor.makeState();
  }

  private State createRentingState() {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var s0 = new State(v1, req);
    var editor = s0.edit(edge1);
    editor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER_STANDING, null, NETWORK, false);
    return editor.makeState();
  }

  private EdgeTraversal edgeTraversal() {
    return (s0, mode) -> s0.edit(edge4);
  }

  @Nested
  class TriggerDetection {

    @Test
    void noTriggerForRentingState() {
      var state = createRentingState();

      var result = DeferredForkHandler.applyDeferredFork(state, edgeTraversal());
      assertNull(result, "renting state should not trigger deferred fork");
    }

    @Test
    void noTriggerWhenNoBackState() {
      // Initial BEFORE_RENTING state has no backState and is not HAVE_RENTED
      var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
      var s0 = new State(v1, req);
      var result = DeferredForkHandler.applyDeferredFork(s0, edgeTraversal());
      assertNull(result, "state without HAVE_RENTED should not trigger");
    }

    @Test
    void noTriggerWhenZonesUnchanged() {
      var state = createHaveRentedNoZoneChange();

      var result = DeferredForkHandler.applyDeferredFork(state, edgeTraversal());
      assertNull(result, "same zones in back and current should not trigger");
    }

    @Test
    void triggersWhenRestrictedZoneExited() {
      var state = createHaveRentedWithZoneExit(NO_DROP_OFF_ZONE);

      var result = DeferredForkHandler.applyDeferredFork(state, edgeTraversal());
      assertNotNull(result, "should trigger fork when restricted zone exited");
      assertTrue(result.length >= 1, "should produce at least one state");
    }

    @Test
    void triggersWhenBusinessAreaExited() {
      var state = createHaveRentedWithZoneExit(BUSINESS_AREA);

      var result = DeferredForkHandler.applyDeferredFork(state, edgeTraversal());
      assertNotNull(result, "should trigger fork when business area exited");
      assertTrue(result.length >= 1, "should produce at least one state");
    }

    @Test
    void triggersWhenBusinessAreaGained() {
      // Walker entered BA in arrive-by: s0 has BA zone, backState doesn't.
      // This is the forward-time BA exit (drop-off point inside BA).
      var state = createHaveRentedWithZoneGain(BUSINESS_AREA);

      var result = DeferredForkHandler.applyDeferredFork(state, edgeTraversal());
      assertNotNull(result, "should trigger fork when BA zone gained (arrive-by entry)");
      assertTrue(result.length >= 1, "should produce at least one state");
    }

    @Test
    void doesNotTriggerWhenRestrictedZoneGained() {
      // Zone gain for non-BA zones should NOT trigger (only BA zones use gain direction)
      var state = createHaveRentedWithZoneGain(NO_DROP_OFF_ZONE);

      var result = DeferredForkHandler.applyDeferredFork(state, edgeTraversal());
      assertNull(result, "zone gain for restricted zone should not trigger deferred fork");
    }
  }

  @Nested
  class ForkBehavior {

    @Test
    void producesWalkingAndRentingBranches() {
      var state = createHaveRentedWithZoneExit(NO_DROP_OFF_ZONE);

      var result = DeferredForkHandler.applyDeferredFork(state, edgeTraversal());

      assertNotNull(result);
      assertTrue(result.length >= 2, "should have walking + renting branches");

      boolean hasWalking = false;
      boolean hasRenting = false;
      boolean hasGeneric = false;
      for (var s : result) {
        if (s.getVehicleRentalState() == VehicleRentalState.HAVE_RENTED) {
          hasWalking = true;
        }
        if (
          s.getVehicleRentalState() == VehicleRentalState.RENTING_FLOATING &&
          NETWORK.equals(s.getVehicleRentalNetwork())
        ) {
          hasRenting = true;
        }
        if (
          s.getVehicleRentalState() == VehicleRentalState.RENTING_FLOATING &&
          s.getVehicleRentalNetwork() == null
        ) {
          hasGeneric = true;
        }
      }
      assertTrue(hasWalking, "should have walking branch");
      assertTrue(hasRenting, "should have per-network renting branch");
      assertTrue(hasGeneric, "should have generic renting branch");
    }

    @Test
    void skipsNetworkBannedByRequest() {
      var req = StreetSearchRequest.of()
        .withMode(StreetMode.SCOOTER_RENTAL)
        .withScooter(s -> s.withRental(r -> r.withBannedNetworks(Set.of(NETWORK))))
        .build();

      var s0 = new State(v1, req);
      var rentEditor = s0.edit(edge1);
      rentEditor.beginFloatingVehicleRenting(
        RentalFormFactor.SCOOTER_STANDING,
        null,
        NETWORK,
        false
      );
      var renting = rentEditor.makeState();
      var dropEditor = renting.edit(edge2);
      dropEditor.dropFloatingVehicle(RentalFormFactor.SCOOTER_STANDING, null, NETWORK, false);
      dropEditor.initializeGeofencingZones(Set.of(NO_DROP_OFF_ZONE));
      var insideZone = dropEditor.makeState();
      var exitEditor = insideZone.edit(edge3);
      exitEditor.initializeGeofencingZones(Set.of());
      var state = exitEditor.makeState();

      var result = DeferredForkHandler.applyDeferredFork(state, edgeTraversal());

      assertNotNull(result);
      for (var s : result) {
        if (s.getVehicleRentalState() == VehicleRentalState.RENTING_FLOATING) {
          assertNull(
            s.getVehicleRentalNetwork(),
            "banned network should not have a committed renting branch"
          );
        }
      }
    }

    @Test
    void preTraversalVetoForAdjacentNoDropOff() {
      var adjacentNoDropOff = TestGeofencingZoneBuilder.of(NETWORK, "adjacent-ndo")
        .withGeometry(Polygons.OSLO_FROGNER_PARK)
        .noDropOff()
        .build();

      var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();

      var s0 = new State(v1, req);
      var rentEditor = s0.edit(edge1);
      rentEditor.beginFloatingVehicleRenting(
        RentalFormFactor.SCOOTER_STANDING,
        null,
        NETWORK,
        false
      );
      var renting = rentEditor.makeState();
      var dropEditor = renting.edit(edge2);
      dropEditor.dropFloatingVehicle(RentalFormFactor.SCOOTER_STANDING, null, NETWORK, false);
      dropEditor.initializeGeofencingZones(Set.of(NO_DROP_OFF_ZONE, adjacentNoDropOff));
      var insideZone = dropEditor.makeState();
      var exitEditor = insideZone.edit(edge3);
      // Exited NO_DROP_OFF_ZONE but still in adjacentNoDropOff
      exitEditor.initializeGeofencingZones(Set.of(adjacentNoDropOff));
      var state = exitEditor.makeState();

      var result = DeferredForkHandler.applyDeferredFork(state, edgeTraversal());

      assertNotNull(result);
      // Should only have walking branch — renting vetoed by pre-traversal check
      for (var s : result) {
        if (
          s.getVehicleRentalState() == VehicleRentalState.RENTING_FLOATING &&
          NETWORK.equals(s.getVehicleRentalNetwork())
        ) {
          fail("should not create renting branch for network inside adjacent no-drop-off zone");
        }
      }
    }
  }
}
