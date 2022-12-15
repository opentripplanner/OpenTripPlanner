package org.opentripplanner.service.worldenvelope.model;

import java.util.Arrays;

/**
 * Calculate the median for a large set of doubles. This class is package local; It is only used
 * by the builder.
 */
class MedianCalcForDoubles {

  private final double[] array;
  private int index = 0;

  public MedianCalcForDoubles(int size) {
    if (size <= 0) {
      throw new IllegalStateException("The set must contain at least one element. n: " + size);
    }
    this.array = new double[size];
  }

  public void add(double v) {
    array[index++] = v;
  }

  /** Reset the index, so a new dataset with the same size can be added and then median calculated. */
  public void reset() {
    index = 0;
  }

  public double median() {
    int n = array.length;
    if (index != n) {
      throw new IllegalStateException(
        "The correct number of values are not added. Index: " + index + ", n: " + n
      );
    }
    Arrays.sort(array);

    if (n % 2 == 1) {
      return array[(n - 1) / 2];
    } else {
      int i = (n / 2) - 1;
      return (array[i] + array[i + 1]) / 2.0;
    }
  }
}
