package org.opentripplanner.utils.lang;

import java.util.Objects;

/**
 * An double range is a continous range from the start value(inclusive) to the end value(exclusive).
 * The start value must be smaller then the end value. The start is inclusive and the end excusive.
 * This allows the range to contain exactly one number like 2.0: [2.0 - 2.0), and that to ranges
 * can be added together nicely: [1.0 - 2.4) + [2.4 - 3.0) = [1.0 - 3.0).
 */
public class DoubleRange {

  private final double start;
  private final double end;

  /**
   * This constructor is private, use the builder to create a new double range.
   */
  private DoubleRange(double startInclusive, double endExclusive) {
    this.start = startInclusive;
    this.end = endExclusive;

    if (start > end) {
      throw new IllegalArgumentException(
        "The start of the range must be less then or equal to the end: " + this
      );
    }
  }

  public static DoubleRange of(double start, double end) {
    return new DoubleRange(start, end);
  }

  public double start() {
    return start;
  }

  public double end() {
    return end;
  }

  /**
   * Check if {@code start <= value <= end}
   */
  public boolean contains(double value) {
    return start <= value && value < end;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    var other = (DoubleRange) o;
    return start == other.start && end == other.end;
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
  public String toString() {
    return "[" + start + " - " + end + ')';
  }
}
