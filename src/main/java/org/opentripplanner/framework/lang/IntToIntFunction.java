package org.opentripplanner.framework.lang;

/**
 * Map a native int to a native int without boxing. This is used to avoid
 * the {@link java.util.function.ToIntFunction} an {@link java.util.function.IntFunction}
 * in code where performance matter.
 */
@FunctionalInterface
public interface IntToIntFunction {
  /**
   * Map the given value to a new value, both the source and result must be a native int.
   */
  int apply(int value);
}
