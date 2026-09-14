package org.opentripplanner.raptor.rangeraptor.support;

import java.util.function.IntUnaryOperator;
import org.opentripplanner.raptor.rangeraptor.internalapi.SingleCriteriaStopArrivals;

public final class IntArraySingleCriteriaArrivals implements SingleCriteriaStopArrivals {

  private final int unreached;
  private final int[] values;

  public IntArraySingleCriteriaArrivals(int unreached, int[] values) {
    this.unreached = unreached;
    this.values = values;
  }

  public static IntArraySingleCriteriaArrivals create(
    int size,
    int unreached,
    IntUnaryOperator mapValue
  ) {
    int[] array = new int[size];
    for (int i = 0; i < size; ++i) {
      array[i] = mapValue.applyAsInt(i);
    }
    return new IntArraySingleCriteriaArrivals(unreached, array);
  }

  @Override
  public boolean isReached(int stop) {
    return value(stop) != unreached;
  }

  @Override
  public int value(int stop) {
    return values[stop];
  }
}
