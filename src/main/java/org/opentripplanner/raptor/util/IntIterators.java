package org.opentripplanner.raptor.util;

import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.spi.IntIterator;

public class IntIterators {

  private static final IntIterator EMPTY = new IntIterator() {
    @Override
    public int next() {
      return RaptorConstants.NOT_FOUND;
    }

    @Override
    public boolean hasNext() {
      return false;
    }
  };

  /** This is private to forbid construction. */
  private IntIterators() {
    /* NOOP*/
  }

  /* Static factories */

  /**
   * Create an int iterator incrementing by 1.
   *
   * @param startValue the start value (inclusive)
   * @param endValue   the end value (exclusive)
   * @return the iterator
   */
  public static IntIterator intIncIterator(final int startValue, final int endValue) {
    return new IntIterator() {
      private int i = startValue;

      @Override
      public int next() {
        return i++;
      }

      @Override
      public boolean hasNext() {
        return i < endValue;
      }
    };
  }

  /**
   * Create an int iterator incrementing by 1.
   *
   * @param startValue the start value (inclusive)
   * @param endValue   the end value (exclusive)
   * @param increment  the value to add for each iteration
   * @return the iterator
   */
  public static IntIterator intIncIterator(
    final int startValue,
    final int endValue,
    final int increment
  ) {
    return new IntIterator() {
      private int i = startValue;

      @Override
      public int next() {
        return i += increment;
      }

      @Override
      public boolean hasNext() {
        return i < endValue;
      }
    };
  }

  /**
   * Create an int iterator decrementing by 1.
   *
   * @param startValue the start value (exclusive)
   * @param endValue   the end value (inclusive)
   */
  public static IntIterator intDecIterator(final int startValue, final int endValue) {
    return new IntIterator() {
      private int i = startValue - 1;

      @Override
      public int next() {
        return i--;
      }

      @Override
      public boolean hasNext() {
        return i >= endValue;
      }
    };
  }

  /**
   * Create an int iterator decrementing by the given decrement value.
   *
   * @param startValue the start value (exclusive)
   * @param endValue   the end value (inclusive)
   * @param decrement  the value to subtract for each iteration, must be a positive integer.
   */
  public static IntIterator intDecIterator(
    final int startValue,
    final int endValue,
    final int decrement
  ) {
    return new IntIterator() {
      private int i = startValue - decrement;

      @Override
      public int next() {
        int tmp = i;
        i -= decrement;
        return tmp;
      }

      @Override
      public boolean hasNext() {
        return i >= endValue;
      }
    };
  }

  /** Return a single value once. */
  public static IntIterator singleValueIterator(final int value) {
    return intIncIterator(value, value + 1);
  }

  /**
   * Return an empty iterator. All calls to {@link IntIterator#hasNext()} will return {@code false}.
   */
  public static IntIterator empty() {
    return EMPTY;
  }
}
