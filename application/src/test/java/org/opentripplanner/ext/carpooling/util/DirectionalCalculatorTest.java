package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.opentripplanner.ext.carpooling.TestFixtures.*;

import org.junit.jupiter.api.Test;

class DirectionalCalculatorTest {

  private static final double TOLERANCE = 5.0; // 5 degree tolerance for cardinal directions

  @Test
  void calculateBearing_northward_returns0Degrees() {
    // Oslo center to Oslo north should be ~0° (north)
    double bearing = DirectionalCalculator.calculateBearing(OSLO_CENTER, OSLO_NORTH);
    assertEquals(0.0, bearing, TOLERANCE);
  }

  @Test
  void calculateBearing_eastward_returns90Degrees() {
    // Oslo center to Oslo east should be ~90° (east)
    double bearing = DirectionalCalculator.calculateBearing(OSLO_CENTER, OSLO_EAST);
    assertEquals(90.0, bearing, TOLERANCE);
  }

  @Test
  void calculateBearing_southward_returns180Degrees() {
    double bearing = DirectionalCalculator.calculateBearing(OSLO_CENTER, OSLO_SOUTH);
    assertEquals(180.0, bearing, TOLERANCE);
  }

  @Test
  void calculateBearing_westward_returns270Degrees() {
    double bearing = DirectionalCalculator.calculateBearing(OSLO_CENTER, OSLO_WEST);
    assertEquals(270.0, bearing, TOLERANCE);
  }

  @Test
  void bearingDifference_similarDirections_returnsSmallValue() {
    // 10° and 20° should be 10° apart
    double diff = DirectionalCalculator.bearingDifference(10.0, 20.0);
    assertEquals(10.0, diff, 0.01);
  }

  @Test
  void bearingDifference_oppositeDirections_returns180() {
    // North (0°) and South (180°) are 180° apart
    double diff = DirectionalCalculator.bearingDifference(0.0, 180.0);
    assertEquals(180.0, diff, 0.01);
  }

  @Test
  void bearingDifference_wrapAround_returnsShortestAngle() {
    // 10° and 350° are only 20° apart (not 340°)
    double diff = DirectionalCalculator.bearingDifference(10.0, 350.0);
    assertEquals(20.0, diff, 0.01);
  }

  @Test
  void bearingDifference_reverse_returnsShortestAngle() {
    // Should be symmetric
    double diff1 = DirectionalCalculator.bearingDifference(10.0, 350.0);
    double diff2 = DirectionalCalculator.bearingDifference(350.0, 10.0);
    assertEquals(diff1, diff2, 0.01);
  }
}
