package org.opentripplanner.utils.lang;

import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * An integer range is an ordered tuple of integers where the 'start' is less than or equal
 * to the 'end'. The responsibility of this class is to perform various mathematical
 * operation on an ordered range/tuple of integers.
 */
public class IntRange {

  private final int startInclusive;
  private final int endInclusive;

  /**
   * private constructor to avoid calling this from the outside. This prevents us from using a
   * record.
   */
  private IntRange(int startInclusive, int endInclusive) {
    this.startInclusive = startInclusive;
    this.endInclusive = endInclusive;

    if (startInclusive > endInclusive) {
      throw new IllegalArgumentException(
        "The start of the range must be less then or equal to the end: " + this
      );
    }
  }

  public static IntRange ofInclusive(int startInclusive, int endInclusive) {
    return new IntRange(startInclusive, endInclusive);
  }

  public int startInclusive() {
    return startInclusive;
  }

  public int endInclusive() {
    return endInclusive;
  }

  /**
   * Add a constant value to both the start and end value of the range. This is the
   * same as shifting/transforming the "range" up.
   */
  public IntRange plus(int delta) {
    return new IntRange(startInclusive + delta, endInclusive + delta);
  }

  /**
   * Subtract a constant value from both the start and end value of the range. This is the
   * same as shifting/transforming the "range" down.
   */
  public IntRange minus(int value) {
    return plus(-value);
  }

  /**
   * Return the intersection between {@code this} and the {@code other} range. Two ranges
   * intersect if at least one value is in both. If not, {@code empty} is returned.
   */
  public Optional<IntRange> intersect(IntRange other) {
    int s = Math.max(startInclusive, other.startInclusive);
    int e = Math.min(endInclusive, other.endInclusive);
    return e < s ? Optional.empty() : Optional.of(new IntRange(s, e));
  }

  /**
   * Check if {@code start <= value <= end}
   */
  public boolean contains(int value) {
    return startInclusive <= value && value <= endInclusive;
  }

  /**
   * Check if {@code value < start or value > end }
   */
  public boolean isOutside(int value) {
    return value < startInclusive || value > endInclusive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IntRange other = (IntRange) o;
    return startInclusive == other.startInclusive && endInclusive == other.endInclusive;
  }

  @Override
  public int hashCode() {
    return Objects.hash(startInclusive, endInclusive);
  }

  @Override
  public String toString() {
    return toString(Integer::toString);
  }

  public String toString(IntFunction<String> valueToString) {
    var v1 = valueToString.apply(startInclusive);
    var v2 = valueToString.apply(endInclusive);
    // First argument must be a String not a character to force String concatenation
    return "[" + v1 + ", " + v2 + ']';
  }
}
