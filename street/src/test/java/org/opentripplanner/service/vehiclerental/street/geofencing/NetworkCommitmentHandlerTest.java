package org.opentripplanner.service.vehiclerental.street.geofencing;

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

class NetworkCommitmentHandlerTest {

  static final String NETWORK_A = "tier";
  static final String NETWORK_B = "voi";

  static final GeofencingZone NO_DROP_OFF_A = TestGeofencingZoneBuilder.of(NETWORK_A, "ndo-a")
    .withGeometry(Polygons.OSLO)
    .noDropOff()
    .build();

  static final GeofencingZone NO_DROP_OFF_B = TestGeofencingZoneBuilder.of(NETWORK_B, "ndo-b")
    .withGeometry(Polygons.OSLO)
    .noDropOff()
    .build();

  static final GeofencingZone NO_TRAVERSAL_A = TestGeofencingZoneBuilder.of(NETWORK_A, "nt-a")
    .withGeometry(Polygons.OSLO)
    .noTraversal()
    .build();

  static final GeofencingZone BUSINESS_AREA_A = TestGeofencingZoneBuilder.of(NETWORK_A, "ba-a")
    .withGeometry(Polygons.OSLO)
    .asBusinessArea()
    .build();

  StreetVertex v1, v2, v3;
  StreetEdge setupEdge, testEdge;

  @BeforeEach
  void setUp() {
    v1 = intersectionVertex(1, 1);
    v2 = intersectionVertex(2, 2);
    v3 = intersectionVertex(3, 3);
    setupEdge = streetEdge(v1, v2);
    testEdge = streetEdge(v2, v3);
  }

  /**
   * Creates a generic (null-network) RENTING_FLOATING state.
   */
  private State createGenericRentingState(GeofencingZone... zones) {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var s0 = new State(v1, req);
    var editor = s0.edit(setupEdge);
    editor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER_STANDING, null, null, false);
    if (zones.length > 0) {
      editor.initializeGeofencingZones(Set.of(zones));
    }
    return editor.makeState();
  }

  private State createGenericRentingStateWithCommittedNetworks(
    Set<String> committed,
    GeofencingZone... zones
  ) {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var s0 = new State(v1, req);
    var editor = s0.edit(setupEdge);
    editor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER_STANDING, null, null, false);
    for (String n : committed) {
      editor.addCommittedNetwork(n);
    }
    if (zones.length > 0) {
      editor.initializeGeofencingZones(Set.of(zones));
    }
    return editor.makeState();
  }

  private State createGenericRentingStateWithBannedNetworks(Set<String> banned) {
    var req = StreetSearchRequest.of()
      .withMode(StreetMode.SCOOTER_RENTAL)
      .withScooter(s -> s.withRental(r -> r.withBannedNetworks(banned)))
      .build();
    var s0 = new State(v1, req);
    var editor = s0.edit(setupEdge);
    editor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER_STANDING, null, null, false);
    return editor.makeState();
  }

  /**
   * EdgeTraversal that also updates geofencing zones from boundary extensions on vertices.
   */
  private EdgeTraversal zoneUpdatingTraversal() {
    return (s0, mode) -> {
      var editor = s0.edit(testEdge);
      editor.updateGeofencingZones(v2, v3, false);
      return editor;
    };
  }

  private EdgeTraversal simpleTraversal() {
    return (s0, mode) -> s0.edit(testEdge);
  }

  @Nested
  class NoTraversalRecording {

    @Test
    void recordsNoTraversalNetwork() {
      // entering=false on fromv → effectiveEntering=true in arriveBy (entering zone)
      var boundaries = List.of(new GeofencingBoundaryExtension(NO_TRAVERSAL_A, false));
      var state = createGenericRentingState();

      var result = NetworkCommitmentHandler.applyNetworkCommitment(
        state,
        boundaries,
        simpleTraversal()
      );

      assertNotNull(result);
      assertTrue(result.length >= 1, "should produce at least one state");
      assertTrue(
        result[0].getCommittedNetworks().contains(NETWORK_A),
        "should record no-traversal network in committedNetworks"
      );
    }

    @Test
    void skipsAlreadyCommittedNetwork() {
      var boundaries = List.of(new GeofencingBoundaryExtension(NO_TRAVERSAL_A, false));
      var state = createGenericRentingStateWithCommittedNetworks(Set.of(NETWORK_A));

      var result = NetworkCommitmentHandler.applyNetworkCommitment(
        state,
        boundaries,
        simpleTraversal()
      );

      // Already committed → falls through to commitNetworks path
      // No new zones (simpleTraversal doesn't update zones) → returns null
      assertNull(result, "should not re-record already committed network");
    }

    @Test
    void skipsEnteringBoundary() {
      // entering=true on fromv → not a no-traversal entry direction
      var boundaries = List.of(new GeofencingBoundaryExtension(NO_TRAVERSAL_A, true));
      var state = createGenericRentingState();

      var result = NetworkCommitmentHandler.applyNetworkCommitment(
        state,
        boundaries,
        simpleTraversal()
      );

      // No no-traversal recording, no zone change → null
      assertNull(result, "entering=true should not trigger no-traversal recording");
    }
  }

  @Nested
  class ZoneCrossingFork {

    @Test
    void forksPerNetworkBranches() {
      // Set up boundary so traversing v2→v3 enters a no-drop-off zone for NETWORK_A
      v2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_A, true));
      v3.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_A, false));

      var boundaries = List.of(new GeofencingBoundaryExtension(NO_DROP_OFF_A, true));
      var state = createGenericRentingState();

      var result = NetworkCommitmentHandler.applyNetworkCommitment(
        state,
        boundaries,
        zoneUpdatingTraversal()
      );

      assertNotNull(result);
      assertTrue(result.length >= 2, "should have committed + generic branches");

      boolean hasCommitted = false;
      boolean hasGeneric = false;
      for (var s : result) {
        if (NETWORK_A.equals(s.getVehicleRentalNetwork())) {
          hasCommitted = true;
        }
        if (s.getVehicleRentalNetwork() == null) {
          hasGeneric = true;
        }
      }
      assertTrue(hasCommitted, "should have a committed branch for " + NETWORK_A);
      assertTrue(hasGeneric, "should have a generic continuation");
    }

    @Test
    void genericStateDoesNotGetCommittedAfterBoundaryFork() {
      v2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_A, true));
      v3.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_A, false));

      var boundaries = List.of(new GeofencingBoundaryExtension(NO_DROP_OFF_A, true));
      var state = createGenericRentingState();

      var result = NetworkCommitmentHandler.applyNetworkCommitment(
        state,
        boundaries,
        zoneUpdatingTraversal()
      );

      assertNotNull(result);
      for (var s : result) {
        if (s.getVehicleRentalNetwork() == null) {
          assertTrue(
            s.getCommittedNetworks().isEmpty(),
            "generic branch should not accumulate boundary-fork networks"
          );
        }
      }
    }

    @Test
    void noForkWhenNoNewZones() {
      // No boundary extensions → no zone change after traversal
      var boundaries = List.of(new GeofencingBoundaryExtension(NO_DROP_OFF_A, true));
      var state = createGenericRentingState();

      var result = NetworkCommitmentHandler.applyNetworkCommitment(
        state,
        boundaries,
        simpleTraversal()
      );

      assertNull(result, "no zone change → no fork needed");
    }

    @Test
    void skipsTraversalBannedZones() {
      v2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_A, true));
      v3.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_TRAVERSAL_A, false));

      var boundaries = List.of(new GeofencingBoundaryExtension(NO_TRAVERSAL_A, true));
      var state = createGenericRentingState();

      var result = NetworkCommitmentHandler.applyNetworkCommitment(
        state,
        boundaries,
        zoneUpdatingTraversal()
      );

      // traversalBanned zones are filtered out by findNewZoneNetworks
      assertNull(result, "no-traversal zones should not trigger fork");
    }

    @Test
    void businessAreaIsCommitOnlyNoFork() {
      // BA zones should add network to committedNetworks without forking a committed branch.
      // The committed rider for BA networks comes from the HAVE_RENTED walker path.
      v2.addGeofencingBoundary(new GeofencingBoundaryExtension(BUSINESS_AREA_A, true));
      v3.addGeofencingBoundary(new GeofencingBoundaryExtension(BUSINESS_AREA_A, false));

      var boundaries = List.of(new GeofencingBoundaryExtension(BUSINESS_AREA_A, true));
      var state = createGenericRentingState();

      var result = NetworkCommitmentHandler.applyNetworkCommitment(
        state,
        boundaries,
        zoneUpdatingTraversal()
      );

      assertNotNull(result, "should produce at least one state");

      // Should have only generic continuation, no committed branch
      for (var s : result) {
        assertNull(
          s.getVehicleRentalNetwork(),
          "BA zone should NOT produce committed branch — only commit-only"
        );
      }
      // Generic continuation should have BA network in committedNetworks
      boolean hasCommitOnly = false;
      for (var s : result) {
        if (s.getVehicleRentalNetwork() == null && s.getCommittedNetworks().contains(NETWORK_A)) {
          hasCommitOnly = true;
        }
      }
      assertTrue(hasCommitOnly, "generic branch should have BA network in committedNetworks");
    }

    @Test
    void skipsBannedNetwork() {
      v2.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_A, true));
      v3.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_A, false));

      var boundaries = List.of(new GeofencingBoundaryExtension(NO_DROP_OFF_A, true));
      var state = createGenericRentingStateWithBannedNetworks(Set.of(NETWORK_A));

      var result = NetworkCommitmentHandler.applyNetworkCommitment(
        state,
        boundaries,
        zoneUpdatingTraversal()
      );

      assertNotNull(result);
      // Should only have generic branch (banned network filtered from committed branches)
      for (var s : result) {
        assertNull(
          s.getVehicleRentalNetwork(),
          "banned network should not have a committed branch"
        );
      }
    }
  }
}
