package org.opentripplanner.ext.flex.flexpathcalculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelForTest.streetEdge;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

class StreetFlexPathCalculatorTest {

  private static final int IGNORED = -99;
  private static final int EPSILON = 10;
  private static final int EXPECTED_DURATION = 42_112;
  private static final int EXPECTED_DISTANCE = 471_653;

  private final IntersectionVertex v0 = intersectionVertex(0, 0);
  private final IntersectionVertex v1 = intersectionVertex(1, 1);
  private final IntersectionVertex v2 = intersectionVertex(2, 2);
  private final IntersectionVertex v3 = intersectionVertex(3, 3);

  {
    streetEdge(v0, v1);
    streetEdge(v1, v0);
    streetEdge(v1, v2);
    streetEdge(v2, v1);
    streetEdge(v2, v3);
    streetEdge(v3, v2);
  }

  @Test
  void forward() {
    var forwardCalculator = new StreetFlexPathCalculator(false, Duration.ofDays(1));
    var forwardPath = forwardCalculator.calculateFlexPath(v0, v3, IGNORED, IGNORED);
    assertEquals("LINESTRING (0 0, 1 1, 2 2, 3 3)", forwardPath.getGeometry().toString());

    assertEquals(EXPECTED_DURATION, forwardPath.durationSeconds, EPSILON);
    assertEquals(EXPECTED_DISTANCE, forwardPath.distanceMeters, EPSILON);
  }

  @Test
  void backward() {
    var reverseCalculator = new StreetFlexPathCalculator(true, Duration.ofDays(1));
    var reversePath = reverseCalculator.calculateFlexPath(v3, v0, IGNORED, IGNORED);
    assertEquals("LINESTRING (3 3, 2 2, 1 1, 0 0)", reversePath.getGeometry().toString());

    assertEquals(EXPECTED_DURATION, reversePath.durationSeconds, EPSILON);
    assertEquals(EXPECTED_DISTANCE, reversePath.distanceMeters, EPSILON);
  }
}
