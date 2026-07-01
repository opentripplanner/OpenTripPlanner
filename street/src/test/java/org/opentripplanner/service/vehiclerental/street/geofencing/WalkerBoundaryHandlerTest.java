package org.opentripplanner.service.vehiclerental.street.geofencing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.VehicleRentalState;

class WalkerBoundaryHandlerTest {

  static final String NETWORK = "tier";

  static final GeofencingZone BUSINESS_AREA = TestGeofencingZoneBuilder.of(NETWORK, "ba")
    .withGeometry(Polygons.OSLO)
    .asBusinessArea()
    .build();

  static final GeofencingZone NO_TRAVERSAL_ZONE = TestGeofencingZoneBuilder.of(NETWORK, "nt")
    .withGeometry(Polygons.OSLO)
    .noTraversal()
    .build();

  static final GeofencingZone NO_DROP_OFF_ZONE = TestGeofencingZoneBuilder.of(NETWORK, "ndo")
    .withGeometry(Polygons.OSLO)
    .noDropOff()
    .build();

  StreetVertex v1, v2, v3;
  StreetEdge setupEdge, testEdge;
  EdgeTraversal edgeTraversal;

  @BeforeEach
  void setUp() {
    v1 = intersectionVertex(1, 1);
    v2 = intersectionVertex(2, 2);
    v3 = intersectionVertex(3, 3);
    setupEdge = streetEdge(v1, v2);
    testEdge = streetEdge(v2, v3);
    edgeTraversal = (s0, mode) -> s0.edit(testEdge);
  }

  /**
   * An arrive-by HAVE_RENTED walker — represents the walker walking back from the destination
   * to a pickup point. State is placed at v3 (the to-vertex of testEdge) so that editing
   * testEdge traverses backward to v2, matching the arrive-by search direction.
   */
  private State createArriveByHaveRentedWalker() {
    var req = StreetSearchRequest.of()
      .withMode(StreetMode.SCOOTER_RENTAL)
      .withArriveBy(true)
      .build();
    return State.getInitialStates(Set.of(v3), req)
      .stream()
      .filter(st -> st.getVehicleRentalState() == VehicleRentalState.HAVE_RENTED)
      .findFirst()
      .orElseThrow();
  }

  /**
   * An arrive-by walker that never rented — handler should ignore.
   */
  private State createArriveByPlainWalker() {
    var req = StreetSearchRequest.of().withMode(StreetMode.WALK).withArriveBy(true).build();
    return State.getInitialStates(Set.of(v3), req).iterator().next();
  }

  @Nested
  class BusinessArea {

    @Test
    void enforcesWhenWalkerExitsBA() {
      // Real time: edge v2→v3 exits BA (fromv inside, tov outside).
      //   fromv entering=false (inside, away exits)
      //   tov   entering=true  (outside, away enters)
      // In arrive-by search direction, the HAVE_RENTED walker is going BACK from destination
      // (real-time tov→fromv), so in real time the walker exits BA on this edge. Trigger.
      var fromBounds = List.of(new GeofencingBoundaryExtension(BUSINESS_AREA, false));
      var toBounds = List.of(new GeofencingBoundaryExtension(BUSINESS_AREA, true));

      var state = createArriveByHaveRentedWalker();
      var result = WalkerBoundaryHandler.apply(state, fromBounds, toBounds, edgeTraversal);

      assertNotNull(result, "walker exits BA → should trigger walking branch");
      assertEquals(1, result.length, "should produce walking-only branch");
      assertEquals(
        VehicleRentalState.HAVE_RENTED,
        result[0].getVehicleRentalState(),
        "walker remains HAVE_RENTED"
      );
    }

    @Test
    void passesWhenWalkerEntersBA() {
      // Real time: edge v2→v3 enters BA (fromv outside, tov inside).
      //   fromv entering=true  (outside, away enters)
      //   tov   entering=false (inside, away exits)
      // In real time the walker entered BA on this edge — that's not a rental drop point
      // (drop must be inside BA). No enforcement.
      var fromBounds = List.of(new GeofencingBoundaryExtension(BUSINESS_AREA, true));
      var toBounds = List.of(new GeofencingBoundaryExtension(BUSINESS_AREA, false));

      var state = createArriveByHaveRentedWalker();
      var result = WalkerBoundaryHandler.apply(state, fromBounds, toBounds, edgeTraversal);

      assertNull(result, "walker enters BA in real time → no enforcement");
    }
  }

  @Nested
  class RestrictedZone {

    @Test
    void enforcesWhenWalkerExitsRestrictedZone() {
      // Real time: edge v2→v3 enters the no-drop-off zone (fromv outside, tov inside).
      //   fromv entering=true  (outside, away enters)
      // In arrive-by, walker goes real-time-backward (tov→fromv), so the walker EXITED the
      // zone at this edge in real time. Restricted-zone direction: trigger on fromv
      // entering=true (opposite of BA).
      var fromBounds = List.of(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, true));
      var toBounds = List.of(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, false));

      var state = createArriveByHaveRentedWalker();
      var result = WalkerBoundaryHandler.apply(state, fromBounds, toBounds, edgeTraversal);

      assertNotNull(result, "walker exits restricted zone → should trigger walking branch");
      assertEquals(1, result.length, "should produce walking-only branch");
      assertEquals(
        VehicleRentalState.HAVE_RENTED,
        result[0].getVehicleRentalState(),
        "walker remains HAVE_RENTED"
      );
    }

    @Test
    void passesWhenWalkerEntersRestrictedZone() {
      // Real time: edge v2→v3 exits the zone (fromv inside, tov outside).
      //   fromv entering=false
      // In real time the walker entered the zone here — not a rental drop point (drop must
      // be outside restricted zone). No enforcement.
      var fromBounds = List.of(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, false));
      var toBounds = List.of(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, true));

      var state = createArriveByHaveRentedWalker();
      var result = WalkerBoundaryHandler.apply(state, fromBounds, toBounds, edgeTraversal);

      assertNull(result, "walker enters restricted zone in real time → no enforcement");
    }

    @Test
    void triggersForNoTraversalZoneToo() {
      // Same direction as the no-drop-off case; verify it works for any restriction type.
      var fromBounds = List.of(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, true));
      var toBounds = List.of(new GeofencingBoundaryExtension(NO_TRAVERSAL_ZONE, false));

      var state = createArriveByHaveRentedWalker();
      var result = WalkerBoundaryHandler.apply(state, fromBounds, toBounds, edgeTraversal);

      assertNotNull(result, "no-traversal restricted zone should also trigger");
      assertEquals(1, result.length);
    }
  }

  @Nested
  class Filters {

    @Test
    void passesForNonHaveRentedState() {
      var fromBounds = List.of(new GeofencingBoundaryExtension(BUSINESS_AREA, false));
      var toBounds = List.of(new GeofencingBoundaryExtension(BUSINESS_AREA, true));

      var state = createArriveByPlainWalker();
      var result = WalkerBoundaryHandler.apply(state, fromBounds, toBounds, edgeTraversal);

      assertNull(result, "non-HAVE_RENTED state should be ignored");
    }

    @Test
    void passesForUnpairedBoundary() {
      // fromv has BA boundary, but tov has no matching paired boundary
      var fromBounds = List.of(new GeofencingBoundaryExtension(BUSINESS_AREA, false));
      var toBounds = List.<GeofencingBoundaryExtension>of();

      var state = createArriveByHaveRentedWalker();
      var result = WalkerBoundaryHandler.apply(state, fromBounds, toBounds, edgeTraversal);

      assertNull(result, "unpaired boundary should be skipped");
    }

    @Test
    void passesForBoundaryWithoutRestrictionOrBA() {
      // A zone with neither restriction nor business-area type — handler should skip.
      var plainZone = TestGeofencingZoneBuilder.of(NETWORK, "plain")
        .withGeometry(Polygons.OSLO)
        .build();
      var fromBounds = List.of(new GeofencingBoundaryExtension(plainZone, false));
      var toBounds = List.of(new GeofencingBoundaryExtension(plainZone, true));

      var state = createArriveByHaveRentedWalker();
      var result = WalkerBoundaryHandler.apply(state, fromBounds, toBounds, edgeTraversal);

      assertNull(result, "zone with no restriction and not BA should be skipped");
    }
  }
}
