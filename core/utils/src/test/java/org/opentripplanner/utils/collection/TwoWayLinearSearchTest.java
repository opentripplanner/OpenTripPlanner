package org.opentripplanner.utils.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TwoWayLinearSearchTest {

  private static final int[] O = {};
  private static final int[] A0 = { 0 };
  private static final int[] A1 = { 1 };
  private static final int[] A00 = { 0, 0 };
  private static final int[] A01 = { 0, 1 };
  private static final int[] A10 = { 1, 0 };
  private static final int[] A11 = { 1, 1 };
  private static final int[] A000 = { 0, 0, 0 };
  private static final int[] A100 = { 1, 0, 0 };
  private static final int[] A001 = { 0, 0, 1 };
  private static final int[] A1001 = { 1, 0, 0, 1 };

  static List<Arguments> findNearestTestCases() {
    return List.of(
      // find the one existing element
      Arguments.of(0, A1, 0, 0, 1),
      Arguments.of(0, A10, 0, 0, 2),
      Arguments.of(0, A10, 1, 0, 2),
      Arguments.of(1, A01, 0, 0, 2),
      Arguments.of(1, A01, 1, 0, 2),
      // find first match
      Arguments.of(0, A1001, 0, 0, 4),
      Arguments.of(0, A1001, 1, 0, 4),
      Arguments.of(3, A1001, 2, 0, 4),
      Arguments.of(3, A1001, 3, 0, 4),
      // Restrict range, exclude first element
      Arguments.of(3, A1001, 0, 1, 4),
      Arguments.of(3, A1001, 1, 1, 4),
      // Restrict range, exclude last element
      Arguments.of(0, A1001, 2, 0, 3),
      Arguments.of(0, A1001, 3, 0, 3)
    );
  }

  @ParameterizedTest
  @MethodSource("findNearestTestCases")
  void testFindNearest(int expected, int[] a, int start, int lowerBound, int upperBound) {
    assertEquals(
      OptionalInt.of(expected),
      TwoWayLinearSearch.findNearest(start, lowerBound, upperBound, i -> a[i] == 1)
    );
    assertEquals(
      OptionalInt.of(expected),
      TwoWayLinearSearch.findNearest(start, lowerBound, upperBound, i -> a[i] == 1)
    );
  }

  static List<Arguments> findNearestNotFoundTestCases() {
    return List.of(
      Arguments.of(O, 0, 0, 0, "No match in array"),
      Arguments.of(A0, 0, 0, 1, "No match in array"),
      Arguments.of(A00, 0, 0, 2, "No match in array"),
      Arguments.of(A000, 0, 0, 3, "No match in array"),
      // start index is outside range above
      Arguments.of(O, 1, 0, 0, "Empty array, startIndex is above"),
      // start index is outside range below
      Arguments.of(O, -1, 0, 0, "Empty array, startIndex is below"),
      // start index not found in range
      Arguments.of(A1, 0, 0, 0, "One element, upper bound == lower bound"),
      Arguments.of(A11, 0, 0, 0, "Two elements, upper bound == lower bound"),
      Arguments.of(A11, 1, 1, 1, "Two elements, upper bound == lower bound"),
      Arguments.of(A000, 0, 0, 3, "3 elements, element do not exist, index=0"),
      Arguments.of(A000, 1, 0, 3, "3 elements, element do not exist, index=1"),
      Arguments.of(A000, 2, 0, 3, "3 elements, element do not exist, index=2"),
      Arguments.of(A100, 1, 1, 3, "3 elements, element exists(#0) outside range[1..2]"),
      Arguments.of(A001, 1, 0, 2, "3 elements, element exists(#2) outside range[0..1]"),
      Arguments.of(A1001, 1, 1, 3, "4 elements, element exists(#0 & #3) outside range[1..2]")
    );
  }

  @ParameterizedTest
  @MethodSource("findNearestNotFoundTestCases")
  void testFindNearestNotFound(
    int[] a,
    int start,
    int lowerBound,
    int upperBound,
    String description
  ) {
    assertEquals(
      OptionalInt.empty(),
      TwoWayLinearSearch.findNearest(start, lowerBound, upperBound, i -> a[i] == 1),
      description
    );
  }

  @Test
  void testOutOfBounds() {
    var ex = assertThrows(ArrayIndexOutOfBoundsException.class, () ->
      TwoWayLinearSearch.findNearest(0, -1, 3, i -> A001[i] == 1)
    );
    assertEquals("Index -1 out of bounds for length 3", ex.getMessage());

    ex = assertThrows(ArrayIndexOutOfBoundsException.class, () ->
      TwoWayLinearSearch.findNearest(2, 0, A100.length + 1, i -> A100[i] == 1)
    );
    assertEquals("Index 3 out of bounds for length 3", ex.getMessage());
  }
}
