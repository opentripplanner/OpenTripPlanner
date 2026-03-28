package org.opentripplanner.ext.flex.flexpathcalculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelForTest.streetEdge;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class StreetFlexPathCalculatorTest {

  public static final int IGNORED = -99;

  @Test
  void test() {
    var v0 = intersectionVertex(0, 0);
    var v1 = intersectionVertex(1, 1);
    var v2 = intersectionVertex(2, 2);
    var v3 = intersectionVertex(3, 3);

    streetEdge(v0, v1);
    streetEdge(v1, v2);
    streetEdge(v2, v3);

    var forwardCalculator = new StreetFlexPathCalculator(false, Duration.ofDays(1));
    var forwardPath = forwardCalculator.calculateFlexPath(v0, v3, IGNORED, IGNORED);
    assertEquals("LINESTRING (1 1, 0 0, 2 2, 1 1, 3 3, 2 2)", forwardPath.getGeometry().toString());

    var reverseCalculator = new StreetFlexPathCalculator(true, Duration.ofDays(1));
    var reversePath = reverseCalculator.calculateFlexPath(v0, v3, IGNORED, IGNORED);
    assertEquals("LINESTRING (0 0, 1 1, 2 2, 3 3)", reversePath.getGeometry().toString());

    assertEquals(forwardPath.distanceMeters, reversePath.distanceMeters);
  }
}
