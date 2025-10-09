package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.opentripplanner.ext.carpooling.TestFixtures.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;

class RouteGeometryTest {

  @Test
  void calculateBoundingBox_singlePoint_returnsPointBox() {
    List<WgsCoordinate> route = List.of(OSLO_CENTER);
    var bbox = RouteGeometry.calculateBoundingBox(route);

    assertEquals(OSLO_CENTER.latitude(), bbox.minLat());
    assertEquals(OSLO_CENTER.latitude(), bbox.maxLat());
    assertEquals(OSLO_CENTER.longitude(), bbox.minLon());
    assertEquals(OSLO_CENTER.longitude(), bbox.maxLon());
  }

  @Test
  void calculateBoundingBox_twoPoints_returnsEnclosingBox() {
    List<WgsCoordinate> route = List.of(OSLO_CENTER, OSLO_NORTH);
    var bbox = RouteGeometry.calculateBoundingBox(route);

    assertEquals(OSLO_CENTER.latitude(), bbox.minLat());
    assertEquals(OSLO_NORTH.latitude(), bbox.maxLat());
    assertEquals(OSLO_CENTER.longitude(), bbox.minLon());
    assertEquals(OSLO_CENTER.longitude(), bbox.maxLon());
  }

  @Test
  void calculateBoundingBox_multiplePoints_findsMinMax() {
    List<WgsCoordinate> route = List.of(OSLO_CENTER, OSLO_NORTH, OSLO_EAST, OSLO_SOUTH, OSLO_WEST);
    var bbox = RouteGeometry.calculateBoundingBox(route);

    assertEquals(OSLO_SOUTH.latitude(), bbox.minLat());
    assertEquals(OSLO_NORTH.latitude(), bbox.maxLat());
    assertEquals(OSLO_WEST.longitude(), bbox.minLon());
    assertEquals(OSLO_EAST.longitude(), bbox.maxLon());
  }

  @Test
  void calculateBoundingBox_emptyList_throwsException() {
    assertThrows(IllegalArgumentException.class, () ->
      RouteGeometry.calculateBoundingBox(List.of())
    );
  }

  @Test
  void areBothWithinCorridor_straightRoute_bothClose_returnsTrue() {
    List<WgsCoordinate> route = List.of(OSLO_CENTER, OSLO_NORTH);
    var pickup = new WgsCoordinate(59.9189, 10.7522); // Slightly north of center
    var dropoff = new WgsCoordinate(59.9389, 10.7522); // Slightly south of north

    assertTrue(RouteGeometry.areBothWithinCorridor(route, pickup, dropoff));
  }

  @Test
  void areBothWithinCorridor_straightRoute_oneFar_returnsFalse() {
    List<WgsCoordinate> route = List.of(OSLO_CENTER, OSLO_NORTH);
    var pickup = new WgsCoordinate(59.9189, 10.7522); // Close
    var dropoff = new WgsCoordinate(59.9189, 11.0000); // Far east

    assertFalse(RouteGeometry.areBothWithinCorridor(route, pickup, dropoff));
  }

  @Test
  void areBothWithinCorridor_bothOutside_returnsFalse() {
    List<WgsCoordinate> route = List.of(OSLO_CENTER, OSLO_NORTH);
    var pickup = new WgsCoordinate(59.9139, 11.0000); // Far east
    var dropoff = new WgsCoordinate(59.9439, 11.0000); // Far east

    assertFalse(RouteGeometry.areBothWithinCorridor(route, pickup, dropoff));
  }

  @Test
  void areBothWithinCorridor_emptyRoute_returnsFalse() {
    // Empty route should return false (or throw exception, both are acceptable)
    try {
      assertFalse(RouteGeometry.areBothWithinCorridor(List.of(), OSLO_CENTER, OSLO_NORTH));
    } catch (IllegalArgumentException e) {
      // Also acceptable to throw exception for empty route
      assertTrue(e.getMessage().contains("empty"));
    }
  }

  @Test
  void areBothWithinCorridor_singlePointRoute_usesExpansion() {
    List<WgsCoordinate> route = List.of(OSLO_CENTER);
    // Points within expanded bounding box should return true
    var pickup = new WgsCoordinate(59.9140, 10.7523); // Very close
    var dropoff = new WgsCoordinate(59.9141, 10.7524);

    assertTrue(RouteGeometry.areBothWithinCorridor(route, pickup, dropoff));
  }
}
