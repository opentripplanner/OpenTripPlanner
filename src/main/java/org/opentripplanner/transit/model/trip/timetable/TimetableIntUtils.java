package org.opentripplanner.transit.model.trip.timetable;

import java.util.Arrays;

/**
 * A few timetable related int utility functions.
 */
class TimetableIntUtils {

  /**
   * Create a hash for an int [N x M] matrix. The size N x M is part of the
   * hash, and the matrix is represented using a single array.
   */
  static int matrixHashCode(int n, int m, int[] array) {
    return (n << 16) + m + 31 * Arrays.hashCode(array);
  }

  /**
   * Create a hash for two int [N x M] matrices. The size N x M is part of the
   * hash, and the two matrices are represented using single arrays.
   */
  static int matrixHashCode(int n, int m, int[] arr1, int[] arr2) {
    int result = matrixHashCode(n, m, arr1);
    return arr1 == arr2 ? result : 31 * result + Arrays.hashCode(arr2);
  }

  /**
   * Take a two-dimensional array and turn it into one dimension. Like this:
   * <pre>
   * Input:
   * [
   *   [ a b c ]  // row 0
   *   [ d e f ]  // row 1
   * ]
   *
   * Result:
   * [ a d b e c f ]
   * </pre>
   * The content is "flipped" to allow fast iteration over the column values.
   */
  static int[] collapseAndFlipMatrix(int[][] input) {
    final int nRows = input.length;
    final int nCols = input[0].length;

    int[] res = new int[nRows * nCols];

    int offset = 0;
    for (int[] row : input) {
      if (row.length != nCols) {
        throw new IllegalArgumentException(
          "Every row need to have the same number oc columns. Expected: " +
          nCols +
          ", was: " +
          row.length +
          ", array: " +
          Arrays.toString(row)
        );
      }
      for (int i = 0; i < row.length; ++i) {
        res[offset + i * nRows] = row[i];
      }
      ++offset;
    }
    return res;
  }

  /**
   * Calculate the y value corresponding to the given x value, assuming linear growth between
   * (x0,y0) to (x1,y1). The result is rounded down (floor).
   * <p>
   * This method is undefined if:
   * <pre>
   *   x0 >= x1
   *   x not in [x0..x1]   -- [inclusive, inclusive]
   * </pre>
   */
  static int interpolateFloor(final int x0, final int y0, final int x1, final int y1, final int x) {
    return y0 + (x - x0) * (y1 - y0) / (x1 - x0);
  }

  /**
   * Calculate the y value corresponding to the given x value, assuming linear growth between
   * (x0,y0) to (x1,y1). The result is rounded up (ceiling).
   * <p>
   * This method is undefined if:
   * <pre>
   *   x0 >= x1
   *   x not in [x0..x1]   -- [inclusive, inclusive]
   * </pre>
   */
  static int interpolateCeiling(
    final int x0,
    final int y0,
    final int x1,
    final int y1,
    final int x
  ) {
    return y1 - (x1 - x) * (y1 - y0) / (x1 - x0);
  }
}
