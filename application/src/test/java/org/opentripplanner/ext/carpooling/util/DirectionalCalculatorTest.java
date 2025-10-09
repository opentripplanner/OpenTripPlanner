package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.opentripplanner.ext.carpooling.TestFixtures.*;

import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.util.DirectionalCalculator.DirectionalAlignment;
import org.opentripplanner.framework.geometry.WgsCoordinate;

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

  @Test
  void isDirectionallyCompatible_sameDirection_returnsTrue() {
    // Both going north
    boolean compatible = DirectionalCalculator.isDirectionallyCompatible(
      OSLO_CENTER,
      OSLO_NORTH, // Trip: north
      OSLO_EAST,
      new WgsCoordinate(59.9549, 10.7922), // Passenger: also north
      60.0
    );
    assertTrue(compatible);
  }

  @Test
  void isDirectionallyCompatible_oppositeDirection_returnsFalse() {
    // Trip north, passenger south
    boolean compatible = DirectionalCalculator.isDirectionallyCompatible(
      OSLO_CENTER,
      OSLO_NORTH, // Trip: north
      OSLO_EAST,
      OSLO_CENTER, // Passenger: west/southwest
      60.0
    );
    assertFalse(compatible);
  }

  @Test
  void isDirectionallyCompatible_withinTolerance_returnsTrue() {
    // Trip going north, passenger going slightly northeast (within 60° tolerance)
    boolean compatible = DirectionalCalculator.isDirectionallyCompatible(
      OSLO_CENTER,
      OSLO_NORTH,
      OSLO_CENTER,
      OSLO_NORTHEAST, // Northeast, ~45° from north
      60.0
    );
    assertTrue(compatible);
  }

  @Test
  void isDirectionallyCompatible_exceedsTolerance_returnsFalse() {
    // Trip going north, passenger going east (90° difference)
    boolean compatible = DirectionalCalculator.isDirectionallyCompatible(
      OSLO_CENTER,
      OSLO_NORTH,
      OSLO_CENTER,
      OSLO_EAST, // East, 90° from north
      60.0 // Tolerance too small
    );
    assertFalse(compatible);
  }

  @Test
  void maintainsForwardProgress_straightLine_returnsTrue() {
    // Inserting point along straight line maintains progress
    boolean maintains = DirectionalCalculator.maintainsForwardProgress(
      OSLO_CENTER,
      OSLO_MIDPOINT_NORTH, // Midpoint north
      OSLO_NORTH,
      90.0
    );
    assertTrue(maintains);
  }

  @Test
  void maintainsForwardProgress_backtracking_returnsFalse() {
    // Inserting point behind causes backtracking
    boolean maintains = DirectionalCalculator.maintainsForwardProgress(
      OSLO_CENTER,
      OSLO_SOUTH, // Point south when going north
      OSLO_NORTH,
      90.0
    );
    assertFalse(maintains);
  }

  @Test
  void maintainsForwardProgress_moderateDetour_returnsTrue() {
    // Slight eastward detour should be allowed
    var slightlyEast = new WgsCoordinate(59.9289, 10.7622);
    boolean maintains = DirectionalCalculator.maintainsForwardProgress(
      OSLO_CENTER,
      slightlyEast,
      OSLO_NORTH,
      90.0
    );
    assertTrue(maintains);
  }

  @Test
  void maintainsForwardProgress_largeDetour_returnsFalse() {
    // Large detour exceeds tolerance
    boolean maintains = DirectionalCalculator.maintainsForwardProgress(
      OSLO_CENTER,
      OSLO_WEST, // Large westward detour when going north
      OSLO_NORTH,
      45.0 // Strict tolerance
    );
    assertFalse(maintains);
  }

  @Test
  void classify_highlyAligned_when10Degrees() {
    // Very close directions
    var alignment = DirectionalCalculator.classify(
      OSLO_CENTER,
      OSLO_NORTH,
      OSLO_EAST,
      new WgsCoordinate(59.9549, 10.7922) // Slightly north from east point
    );
    assertEquals(DirectionalAlignment.HIGHLY_ALIGNED, alignment);
  }

  @Test
  void classify_aligned_when45Degrees() {
    var alignment = DirectionalCalculator.classify(
      OSLO_CENTER,
      OSLO_NORTH, // North
      OSLO_CENTER,
      OSLO_NORTHEAST // Northeast (~45°)
    );
    assertEquals(DirectionalAlignment.ALIGNED, alignment);
  }

  @Test
  void classify_divergent_when90Degrees() {
    var alignment = DirectionalCalculator.classify(
      OSLO_CENTER,
      OSLO_NORTH, // North
      OSLO_CENTER,
      OSLO_EAST // East (90°)
    );
    assertEquals(DirectionalAlignment.DIVERGENT, alignment);
  }

  @Test
  void classify_opposite_when180Degrees() {
    var alignment = DirectionalCalculator.classify(
      OSLO_CENTER,
      OSLO_NORTH, // North
      OSLO_CENTER,
      OSLO_SOUTH // South (180°)
    );
    assertEquals(DirectionalAlignment.OPPOSITE, alignment);
  }

  @Test
  void classify_withCustomThresholds_usesProvidedValues() {
    var alignment = DirectionalCalculator.classify(
      45.0, // Bearing difference
      20.0, // Highly aligned threshold
      50.0, // Aligned threshold
      100.0 // Divergent threshold
    );
    assertEquals(DirectionalAlignment.ALIGNED, alignment); // 45° fits in 20-50 range
  }
}
