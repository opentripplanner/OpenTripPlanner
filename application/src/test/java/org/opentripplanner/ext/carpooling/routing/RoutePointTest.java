package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.*;
import static org.opentripplanner.ext.carpooling.TestFixtures.*;

import org.junit.jupiter.api.Test;

class RoutePointTest {

  @Test
  void constructor_validInputs_createsInstance() {
    var point = new RoutePoint(OSLO_CENTER, "Test Point");

    assertEquals(OSLO_CENTER, point.coordinate());
    assertEquals("Test Point", point.label());
  }

  @Test
  void constructor_nullCoordinate_throwsException() {
    assertThrows(IllegalArgumentException.class, () -> new RoutePoint(null, "Test"));
  }

  @Test
  void constructor_nullLabel_throwsException() {
    assertThrows(IllegalArgumentException.class, () -> new RoutePoint(OSLO_CENTER, null));
  }

  @Test
  void constructor_blankLabel_throwsException() {
    assertThrows(IllegalArgumentException.class, () -> new RoutePoint(OSLO_CENTER, "   "));
  }

  @Test
  void constructor_emptyLabel_throwsException() {
    assertThrows(IllegalArgumentException.class, () -> new RoutePoint(OSLO_CENTER, ""));
  }

  @Test
  void toString_includesLabelAndCoordinates() {
    var point = new RoutePoint(OSLO_CENTER, "Oslo");
    var str = point.toString();

    assertTrue(str.contains("Oslo"));
    assertTrue(str.contains("59.91")); // Partial coordinate
    assertTrue(str.contains("10.75"));
  }

  @Test
  void equals_sameValues_returnsTrue() {
    var point1 = new RoutePoint(OSLO_CENTER, "Test");
    var point2 = new RoutePoint(OSLO_CENTER, "Test");

    assertEquals(point1, point2);
  }

  @Test
  void equals_differentCoordinates_returnsFalse() {
    var point1 = new RoutePoint(OSLO_CENTER, "Test");
    var point2 = new RoutePoint(OSLO_NORTH, "Test");

    assertNotEquals(point1, point2);
  }

  @Test
  void equals_differentLabels_returnsFalse() {
    var point1 = new RoutePoint(OSLO_CENTER, "Test1");
    var point2 = new RoutePoint(OSLO_CENTER, "Test2");

    assertNotEquals(point1, point2);
  }

  @Test
  void hashCode_sameValues_returnsSameHash() {
    var point1 = new RoutePoint(OSLO_CENTER, "Test");
    var point2 = new RoutePoint(OSLO_CENTER, "Test");

    assertEquals(point1.hashCode(), point2.hashCode());
  }
}
