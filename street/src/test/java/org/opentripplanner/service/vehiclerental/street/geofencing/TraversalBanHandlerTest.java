package org.opentripplanner.service.vehiclerental.street.geofencing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelFactory.streetEdge;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
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

class TraversalBanHandlerTest {

  static final String NETWORK = "tier";

  static final GeofencingZone NO_TRAVERSAL_ZONE = TestGeofencingZoneBuilder.of(NETWORK, "zone-nt")
    .withGeometry(Polygons.OSLO)
    .noTraversal()
    .build();

  static final GeofencingZone NO_DROP_OFF_ZONE = TestGeofencingZoneBuilder.of(NETWORK, "zone-ndo")
    .withGeometry(Polygons.OSLO_FROGNER_PARK)
    .noDropOff()
    .build();

  StreetVertex v1, v2;
  StreetEdge setupEdge, testEdge;

  @BeforeEach
  void setUp() {
    v1 = intersectionVertex(1, 1);
    v2 = intersectionVertex(2, 2);
    setupEdge = streetEdge(v1, v2);
    testEdge = streetEdge(v2, intersectionVertex(3, 3));
  }

  @Test
  void blocksRentingStateWhenTraversalBanned() {
    var state = createRentingState(NO_TRAVERSAL_ZONE);
    var result = TraversalBanHandler.apply(state);

    assertNotNull(result);
    assertEquals(0, result.length, "should block — renting state inside no-traversal zone");
  }

  @Test
  void passesWhenNotRenting() {
    var state = createHaveRentedState(NO_TRAVERSAL_ZONE);
    var result = TraversalBanHandler.apply(state);

    assertNull(result, "HAVE_RENTED walker is not renting; pass through");
  }

  @Test
  void passesWhenNoTraversalBannedZone() {
    var state = createRentingState(NO_DROP_OFF_ZONE);
    var result = TraversalBanHandler.apply(state);

    assertNull(result, "no traversal-banned zone in set; pass through");
  }

  @Test
  void passesWhenHigherPriorityZoneOverridesTraversalBan() {
    // Low-priority zone says traversal banned; higher-priority overlapping zone (lower
    // priority number) says explicitly not banned. Per-network priority resolution should
    // pick the override → state is not effectively traversal-banned.
    var lowPriorityBanned = TestGeofencingZoneBuilder.of(NETWORK, "low-banned")
      .withGeometry(Polygons.OSLO)
      .withTraversalBanned(true)
      .withPriority(10)
      .build();
    var highPriorityAllowed = TestGeofencingZoneBuilder.of(NETWORK, "high-allowed")
      .withGeometry(Polygons.OSLO)
      .withTraversalBanned(false)
      .withPriority(1)
      .build();
    var state = createRentingState(lowPriorityBanned, highPriorityAllowed);
    var result = TraversalBanHandler.apply(state);

    assertNull(result, "higher-priority traversalBanned=false should override lower-priority true");
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
}
