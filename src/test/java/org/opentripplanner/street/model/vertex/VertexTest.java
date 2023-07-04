package org.opentripplanner.street.model.vertex;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VertexTest {

  private static final double LAT = 1.0;
  private static final double LON = 2.0;
  private static final double EPSILON_10_E_MINUS_7 = 1e-7;
  private static final double EPSILON_10_E_MINUS_8 = 1e-8;

  @Test
  void testSameLocation() {
    Vertex v1 = new SimpleVertex("", LAT, LON);
    Vertex v2 = new SimpleVertex("", LAT + EPSILON_10_E_MINUS_8, LON);
    assertTrue(v1.sameLocation(v2));
  }

  @Test
  void testDifferentLocation() {
    Vertex v1 = new SimpleVertex("", LAT, LON);
    Vertex v2 = new SimpleVertex("", LAT + EPSILON_10_E_MINUS_7, LON);
    assertFalse(v1.sameLocation(v2));
  }
}
