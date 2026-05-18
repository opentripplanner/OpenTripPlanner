package org.opentripplanner.service.vehiclerental.street.geofencing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdFactory.id;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.TestGeofencingZoneBuilder;
import org.opentripplanner.street.geometry.Polygons;

class GeofencingZoneIndexTest {

  final GeofencingZone frognerPark = TestGeofencingZoneBuilder.of(id("frogner-park"))
    .withGeometry(Polygons.OSLO_FROGNER_PARK)
    .noDropOff()
    .build();

  final GeofencingZone oslo = TestGeofencingZoneBuilder.of(id("oslo"))
    .withGeometry(Polygons.OSLO)
    .asBusinessArea()
    .build();

  final GeofencingZoneIndex index = new GeofencingZoneIndex(List.of(frognerPark, oslo));

  @Test
  void pointInsideSingleZone() {
    // inside Frogner Park and inside Oslo
    var coord = new Coordinate(10.699322, 59.928667);
    var zones = index.getZonesContaining(coord);
    assertTrue(zones.contains(frognerPark));
    assertTrue(zones.contains(oslo));
  }

  @Test
  void pointInsideOnlyOuterZone() {
    // inside Oslo but outside Frogner Park
    var coord = new Coordinate(10.76411762080707, 59.95961972533365);
    var zones = index.getZonesContaining(coord);
    assertTrue(zones.contains(oslo));
    assertEquals(Set.of(oslo), zones);
  }

  @Test
  void pointOutsideAllZones() {
    // way outside Oslo
    var coord = new Coordinate(5.0, 60.5);
    var zones = index.getZonesContaining(coord);
    assertTrue(zones.isEmpty());
  }

  @Test
  void pointOnZoneBoundaryIsIncluded() {
    // A point exactly on the polygon boundary should be classified as inside,
    // matching GeofencingZoneApplier.isVertexInZone which uses covers().
    // Use the first vertex of the Oslo polygon (which is on its boundary).
    var boundaryCoord = new Coordinate(10.62535658370308, 59.961055202323195);
    var zones = index.getZonesContaining(boundaryCoord);
    assertTrue(
      zones.contains(oslo),
      "Point on zone boundary should be included (covers semantics)"
    );
  }

  @Test
  void emptyIndex() {
    var emptyIndex = new GeofencingZoneIndex(List.of());
    var zones = emptyIndex.getZonesContaining(new Coordinate(10.7, 59.9));
    assertTrue(zones.isEmpty());
  }
}
