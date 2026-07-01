package org.opentripplanner.street.search.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdFactory.id;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelFactory.streetEdge;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.TestGeofencingZoneBuilder;
import org.opentripplanner.service.vehiclerental.street.geofencing.GeofencingBoundaryExtension;
import org.opentripplanner.street.geometry.Polygons;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;

class StateEditorGeofencingZoneTest {

  static final GeofencingZone ZONE = TestGeofencingZoneBuilder.of(id("frogner-park"))
    .withGeometry(Polygons.OSLO_FROGNER_PARK)
    .noDropOff()
    .build();

  static final GeofencingZone ZONE_B = TestGeofencingZoneBuilder.of(id("oslo"))
    .withGeometry(Polygons.OSLO)
    .noTraversal()
    .build();

  @Test
  void initializeGeofencingZones() {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var v = intersectionVertex(1, 1);
    var v2 = intersectionVertex(2, 2);
    var edge = streetEdge(v, v2);
    var s0 = new State(v, req);
    var editor = s0.edit(edge);

    editor.initializeGeofencingZones(Set.of(ZONE));

    var s1 = editor.makeState();
    assertEquals(Set.of(ZONE), s1.getCurrentGeofencingZones());
  }

  @Test
  void initializeGeofencingZonesWithEmptySet() {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var v = intersectionVertex(1, 1);
    var v2 = intersectionVertex(2, 2);
    var edge = streetEdge(v, v2);
    var s0 = new State(v, req);
    var editor = s0.edit(edge);

    editor.initializeGeofencingZones(Set.of());

    var s1 = editor.makeState();
    assertTrue(s1.getCurrentGeofencingZones().isEmpty());
  }

  @Test
  void updateGeofencingZonesAddsOnPairedEntry() {
    var fromv = intersectionVertex(1, 1);
    var tov = intersectionVertex(2, 2);
    var edge = streetEdge(fromv, tov);

    // fromv has entering=true, tov has entering=false -> paired boundary
    fromv.addGeofencingBoundary(new GeofencingBoundaryExtension(ZONE, true));
    tov.addGeofencingBoundary(new GeofencingBoundaryExtension(ZONE, false));

    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var s0 = new State(fromv, req);
    var editor = s0.edit(edge);

    editor.updateGeofencingZones(fromv, tov, false);

    var s1 = editor.makeState();
    assertTrue(s1.getCurrentGeofencingZones().contains(ZONE));
  }

  @Test
  void updateGeofencingZonesRemovesOnPairedExit() {
    var fromv = intersectionVertex(1, 1);
    var tov = intersectionVertex(2, 2);
    var edge = streetEdge(fromv, tov);
    var returnEdge = streetEdge(tov, fromv);

    // fromv has entering=false, tov has entering=true -> paired exit
    fromv.addGeofencingBoundary(new GeofencingBoundaryExtension(ZONE, false));
    tov.addGeofencingBoundary(new GeofencingBoundaryExtension(ZONE, true));

    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var s0 = new State(fromv, req);
    // Pre-populate the zone
    var initEditor = s0.edit(edge);
    initEditor.initializeGeofencingZones(Set.of(ZONE));
    var s1 = initEditor.makeState();

    var editor = s1.edit(returnEdge);
    editor.updateGeofencingZones(fromv, tov, false);

    var s2 = editor.makeState();
    assertFalse(s2.getCurrentGeofencingZones().contains(ZONE));
  }

  @Test
  void updateGeofencingZonesNoOpWithoutPair() {
    var fromv = intersectionVertex(1, 1);
    var tov = intersectionVertex(2, 2);
    var edge = streetEdge(fromv, tov);

    // fromv has boundary extension, tov has nothing -> not paired
    fromv.addGeofencingBoundary(new GeofencingBoundaryExtension(ZONE, true));

    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var s0 = new State(fromv, req);
    var editor = s0.edit(edge);

    editor.updateGeofencingZones(fromv, tov, false);

    var s1 = editor.makeState();
    assertTrue(s1.getCurrentGeofencingZones().isEmpty());
  }

  @Test
  void updateGeofencingZonesNoOpWhenSameDirection() {
    var fromv = intersectionVertex(1, 1);
    var tov = intersectionVertex(2, 2);
    var edge = streetEdge(fromv, tov);

    // Both vertices have entering=true for same zone -> not paired
    fromv.addGeofencingBoundary(new GeofencingBoundaryExtension(ZONE, true));
    tov.addGeofencingBoundary(new GeofencingBoundaryExtension(ZONE, true));

    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var s0 = new State(fromv, req);
    var editor = s0.edit(edge);

    editor.updateGeofencingZones(fromv, tov, false);

    var s1 = editor.makeState();
    assertTrue(s1.getCurrentGeofencingZones().isEmpty());
  }

  @Test
  void updateGeofencingZonesArriveByFlipsDirection() {
    var fromv = intersectionVertex(1, 1);
    var tov = intersectionVertex(2, 2);
    var edge = streetEdge(fromv, tov);
    var returnEdge = streetEdge(tov, fromv);

    // fromv entering=true, tov entering=false -> paired
    // In forward: effectiveEntering = true (add zone)
    // In arriveBy: effectiveEntering = false (remove zone)
    fromv.addGeofencingBoundary(new GeofencingBoundaryExtension(ZONE, true));
    tov.addGeofencingBoundary(new GeofencingBoundaryExtension(ZONE, false));

    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var s0 = new State(fromv, req);

    // With arriveBy=true, entering=true gets flipped to effectiveEntering=false -> remove
    var editor = s0.edit(edge);
    editor.initializeGeofencingZones(Set.of(ZONE));
    var s1 = editor.makeState();

    var editor2 = s1.edit(returnEdge);
    editor2.updateGeofencingZones(fromv, tov, true);
    var s2 = editor2.makeState();

    assertFalse(s2.getCurrentGeofencingZones().contains(ZONE));
  }

  @Test
  void addCommittedNetwork() {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var v = intersectionVertex(1, 1);
    var v2 = intersectionVertex(2, 2);
    var v3 = intersectionVertex(3, 3);
    var edge = streetEdge(v, v2);
    var edge2 = streetEdge(v2, v3);
    var s0 = new State(v, req);
    var editor = s0.edit(edge);
    editor.addCommittedNetwork("tier");
    var s1 = editor.makeState();

    var editor2 = s1.edit(edge2);
    editor2.addCommittedNetwork("bird");
    var s2 = editor2.makeState();

    assertEquals(Set.of("tier", "bird"), s2.getCommittedNetworks());
  }

  @Test
  void addCommittedNetworkNoOpIfAlreadyPresent() {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var v = intersectionVertex(1, 1);
    var v2 = intersectionVertex(2, 2);
    var v3 = intersectionVertex(3, 3);
    var edge = streetEdge(v, v2);
    var edge2 = streetEdge(v2, v3);
    var s0 = new State(v, req);
    var editor = s0.edit(edge);
    editor.addCommittedNetwork("tier");
    var s1 = editor.makeState();

    var editor2 = s1.edit(edge2);
    editor2.addCommittedNetwork("tier");
    var s2 = editor2.makeState();

    assertEquals(Set.of("tier"), s2.getCommittedNetworks());
  }
}
