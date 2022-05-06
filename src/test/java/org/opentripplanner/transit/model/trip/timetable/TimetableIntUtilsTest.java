package org.opentripplanner.transit.model.trip.timetable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model.trip.timetable.TimetableIntUtils.collapseAndFlipMatrix;
import static org.opentripplanner.transit.model.trip.timetable.TimetableIntUtils.matrixHashCode;

import org.junit.jupiter.api.Test;

class TimetableIntUtilsTest {

  private final int[][] A_2x3 = { { 1, 2, 3 }, { 4, 5, 6 } };
  private final int[][] E_1x1 = { { 3 } };

  private final int[] A = { 1, 4, 2, 5, 3, 6 };
  private final int[] A_EQ = { 1, 4, 2, 5, 3, 6 };
  private final int[] E = { 3 };
  private final int[] F = { 2 };

  @Test
  void testMatrixHashCode() {
    // Same size, one value differ:
    assertEquals(66591, matrixHashCode(1, 1, E));
    assertEquals(66591, matrixHashCode(1, 1, E, E));
    assertEquals(66560, matrixHashCode(1, 1, F));
    assertEquals(66560, matrixHashCode(1, 1, F, F));
    // Hash for the combination of E and F should differ from hashCode for E and hashCode for F
    assertEquals(2064354, matrixHashCode(1, 1, E, F));

    // Two A´s is not the same as one
    assertEquals(-1548072071, matrixHashCode(1, 3, A, A));
    assertEquals(174297457, matrixHashCode(1, 3, A, A_EQ));

    // Same matrix, but dimension differ
    assertEquals(-1547941001, matrixHashCode(3, 1, A, A));
    assertEquals(178360627, matrixHashCode(3, 1, A, A_EQ));
  }

  @Test
  void testCollapseAndFlipMatrix() {
    assertArrayEquals(E, collapseAndFlipMatrix(E_1x1));
    assertArrayEquals(A, collapseAndFlipMatrix(A_2x3));
  }

  @Test
  void testInterpolateFloor() {
    // Simple case increasing 1:1, f(0)=1
    assertEquals(2, TimetableIntUtils.interpolateFloor(1, 2, 3, 4, 1));
    assertEquals(3, TimetableIntUtils.interpolateFloor(1, 2, 3, 4, 2));
    assertEquals(4, TimetableIntUtils.interpolateFloor(1, 2, 3, 4, 3));

    // Increasing 5:1, f(0)=0
    assertEquals(0, TimetableIntUtils.interpolateFloor(0, 0, 10, 2, 0));
    assertEquals(0, TimetableIntUtils.interpolateFloor(0, 0, 10, 2, 1));
    assertEquals(0, TimetableIntUtils.interpolateFloor(0, 0, 10, 2, 4));
    assertEquals(1, TimetableIntUtils.interpolateFloor(0, 0, 10, 2, 5));
    assertEquals(1, TimetableIntUtils.interpolateFloor(0, 0, 10, 2, 9));
    assertEquals(2, TimetableIntUtils.interpolateFloor(0, 0, 10, 2, 10));

    // Increasing 1:3, f(0)=0
    assertEquals(0, TimetableIntUtils.interpolateFloor(0, 0, 2, 6, 0));
    assertEquals(3, TimetableIntUtils.interpolateFloor(0, 0, 2, 6, 1));
    assertEquals(6, TimetableIntUtils.interpolateFloor(0, 0, 2, 6, 2));
  }

  @Test
  void testInterpolateCeiling() {
    // Simple case increasing 1:1, f(0)=1
    assertEquals(2, TimetableIntUtils.interpolateCeiling(1, 2, 3, 4, 1));
    assertEquals(3, TimetableIntUtils.interpolateCeiling(1, 2, 3, 4, 2));
    assertEquals(4, TimetableIntUtils.interpolateCeiling(1, 2, 3, 4, 3));

    // Increasing 5:1, f(0)=0
    assertEquals(0, TimetableIntUtils.interpolateCeiling(0, 0, 10, 2, 0));
    assertEquals(1, TimetableIntUtils.interpolateCeiling(0, 0, 10, 2, 1));
    assertEquals(1, TimetableIntUtils.interpolateCeiling(0, 0, 10, 2, 4));
    assertEquals(1, TimetableIntUtils.interpolateCeiling(0, 0, 10, 2, 5));
    assertEquals(2, TimetableIntUtils.interpolateCeiling(0, 0, 10, 2, 6));
    assertEquals(2, TimetableIntUtils.interpolateCeiling(0, 0, 10, 2, 10));

    // Increasing 1:3, f(0)=0
    assertEquals(0, TimetableIntUtils.interpolateCeiling(0, 0, 2, 6, 0));
    assertEquals(3, TimetableIntUtils.interpolateCeiling(0, 0, 2, 6, 1));
    assertEquals(6, TimetableIntUtils.interpolateCeiling(0, 0, 2, 6, 2));
  }
}
