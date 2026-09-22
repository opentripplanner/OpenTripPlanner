package org.opentripplanner.utils.collection;

import java.util.OptionalInt;
import java.util.function.IntFunction;

/**
 * Utility class for performing two-way linear searches in index-based collections.
 * <p>
 * This class provides methods to search for elements by expanding outward from a starting
 * index in both forward and backward directions simultaneously, finding the closest match
 * to the starting point.
 */
public class TwoWayLinearSearch {

  /**
   * Finds the nearest index that satisfies the given test condition by searching
   * bidirectionally from a starting index.
   * <p>
   * The search expands outward from {@code startIndex} in both directions (forward and
   * backward) simultaneously, testing indices at increasing offsets until a match is found
   * or the bounds are exceeded. This ensures the closest matching index to the starting
   * point is found.
   * <p>
   * The search pattern is: startIndex, startIndex+1, startIndex-1, startIndex+2,
   * startIndex-2, ...
   *
   * @param startIndex the index to start searching from; if not within bounds, the method
   *                   returns empty. If the {@code startIndex} is out of bound, the start
   *                   position is adjusted to the closest position inside the search
   *                   range.
   * @param lowerBound the lower bound of the search range (inclusive)
   * @param upperBound the upper bound of the search range (exclusive)
   * @param test a function that tests if an index satisfies the search condition
   * @return an OptionalInt containing the nearest matching index, or empty if no match is
   *         found.
   */
  public static OptionalInt findNearest(
    int startIndex,
    int lowerBound,
    int upperBound,
    IntFunction<Boolean> test
  ) {
    if (lowerBound >= upperBound) {
      return OptionalInt.empty();
    }
    if (startIndex < lowerBound) {
      startIndex = lowerBound;
    }
    if (startIndex >= upperBound) {
      startIndex = upperBound - 1;
    }
    if (test.apply(startIndex)) {
      return OptionalInt.of(startIndex);
    }

    int size = Math.min(startIndex - lowerBound, upperBound - startIndex - 1);

    // Search in both directions until the first limit is reached, either the upper or lower
    // bound.
    for (int offset = 1; offset <= size; ++offset) {
      // Forward search
      int i = startIndex + offset;
      if (test.apply(i)) {
        return OptionalInt.of(i);
      }

      // Backward search
      i = startIndex - offset;
      if (test.apply(i)) {
        return OptionalInt.of(i);
      }
    }

    // Continue to search forward - in case there are more elements left
    for (int i = startIndex + size; i < upperBound; ++i) {
      if (test.apply(i)) {
        return OptionalInt.of(i);
      }
    }

    // Continue to search backwards - in case there are more elements left
    for (int i = startIndex - size; i >= lowerBound; --i) {
      if (test.apply(i)) {
        return OptionalInt.of(i);
      }
    }

    return OptionalInt.empty();
  }
}
