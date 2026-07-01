package org.opentripplanner.street.search.state;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelFactory.streetEdge;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.TestGeofencingZoneBuilder;
import org.opentripplanner.street.geometry.Polygons;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;

class StateGeofencingEnforcementTest {

  static final GeofencingZone NO_DROP_OFF_ZONE = TestGeofencingZoneBuilder.of("tier", "zone1")
    .withGeometry(Polygons.OSLO_FROGNER_PARK)
    .noDropOff()
    .build();

  static final GeofencingZone NO_TRAVERSAL_ZONE = TestGeofencingZoneBuilder.of("tier", "zone2")
    .withGeometry(Polygons.OSLO)
    .noTraversal()
    .build();

  static final GeofencingZone HIGH_PRIORITY_ZONE = TestGeofencingZoneBuilder.of("tier", "zone-hp")
    .withGeometry(Polygons.OSLO_FROGNER_PARK)
    .withDropOffBanned(true)
    .withTraversalBanned(true)
    .build();

  static final GeofencingZone LOW_PRIORITY_ZONE = TestGeofencingZoneBuilder.of("tier", "zone-lp")
    .withGeometry(Polygons.OSLO)
    .asBusinessArea()
    .withPriority(10)
    .build();

  private State createRentingState(String network, GeofencingZone... zones) {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var v = intersectionVertex(1, 1);
    var v2 = intersectionVertex(2, 2);
    var edge = streetEdge(v, v2);
    var s0 = new State(v, req);
    var editor = s0.edit(edge);

    editor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER_STANDING, null, network, false);

    if (zones.length > 0) {
      editor.initializeGeofencingZones(Set.of(zones));
    }
    return editor.makeState();
  }

  @Test
  void isDropOffBannedWhenNetworkMatches() {
    var state = createRentingState("tier", NO_DROP_OFF_ZONE);
    assertTrue(state.isDropOffBannedByCurrentZones());
  }

  @Test
  void isDropOffBannedFalseWhenNetworkDoesNotMatch() {
    var state = createRentingState("bird", NO_DROP_OFF_ZONE);
    assertFalse(state.isDropOffBannedByCurrentZones());
  }

  @Test
  void isDropOffBannedFalseWhenNoNetwork() {
    var state = createRentingState(null, NO_DROP_OFF_ZONE);
    assertFalse(state.isDropOffBannedByCurrentZones());
  }

  @Test
  void isDropOffBannedFalseWhenNoZones() {
    var state = createRentingState("tier");
    assertFalse(state.isDropOffBannedByCurrentZones());
  }

  @Test
  void isTraversalBannedWhenNetworkMatches() {
    var state = createRentingState("tier", NO_TRAVERSAL_ZONE);
    assertTrue(state.isTraversalBannedByCurrentZones());
  }

  @Test
  void isTraversalBannedFalseWhenNoRestriction() {
    var state = createRentingState("tier", NO_DROP_OFF_ZONE);
    assertFalse(state.isTraversalBannedByCurrentZones());
  }

  @Test
  void governingZoneRespectsPriority() {
    var state = createRentingState("tier", HIGH_PRIORITY_ZONE);
    assertTrue(state.isDropOffBannedByCurrentZones());
    assertTrue(state.isTraversalBannedByCurrentZones());
  }

  @Test
  void perFieldPrecedenceUnspecifiedFieldFallsThrough() {
    var highPriority = TestGeofencingZoneBuilder.of("tier", "zone-hp")
      .withGeometry(Polygons.OSLO_FROGNER_PARK)
      .withTraversalBanned(true)
      .build();
    var lowPriority = TestGeofencingZoneBuilder.of("tier", "zone-lp")
      .withGeometry(Polygons.OSLO)
      .withDropOffBanned(true)
      .withPriority(10)
      .build();
    var state = createRentingState("tier", highPriority, lowPriority);
    assertTrue(state.isTraversalBannedByCurrentZones());
    assertTrue(state.isDropOffBannedByCurrentZones());
  }

  @Test
  void perFieldPrecedenceHighPriorityExplicitlyAllowsOverridesLowPriority() {
    var highPriority = TestGeofencingZoneBuilder.of("tier", "zone-hp")
      .withGeometry(Polygons.OSLO_FROGNER_PARK)
      .withDropOffBanned(false)
      .build();
    var lowPriority = TestGeofencingZoneBuilder.of("tier", "zone-lp")
      .withGeometry(Polygons.OSLO)
      .withDropOffBanned(true)
      .withPriority(10)
      .build();
    var state = createRentingState("tier", highPriority, lowPriority);
    assertFalse(state.isDropOffBannedByCurrentZones());
  }

  @Test
  void nullFieldMeansNotSpecifiedDefaultsToPermissive() {
    var zone = TestGeofencingZoneBuilder.of("tier", "zone1")
      .withGeometry(Polygons.OSLO)
      .withTraversalBanned(true)
      .build();
    var state = createRentingState("tier", zone);
    assertTrue(state.isTraversalBannedByCurrentZones());
    assertFalse(state.isDropOffBannedByCurrentZones());
  }
}
