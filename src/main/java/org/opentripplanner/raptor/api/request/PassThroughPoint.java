package org.opentripplanner.raptor.api.request;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * A collection of stop indexes used to define a pass through-point.
 */
public class PassThroughPoint {

  private final int[] stops;

  public PassThroughPoint(int[] stops) {
    Objects.requireNonNull(stops);
    if (stops.length == 0) {
      throw new IllegalArgumentException("At least one stop is required");
    }
    this.stops = Arrays.copyOf(stops, stops.length);
  }

  /**
   * This is a convenient accessor method used inside Raptor. It converts the list stops to a
   * bit-set. Add other access methods if needed.
   */
  public BitSet asBitSet() {
    return IntStream.of(stops).collect(BitSet::new, BitSet::set, BitSet::or);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PassThroughPoint that = (PassThroughPoint) o;
    return Arrays.equals(stops, that.stops);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(stops);
  }

  @Override
  public String toString() {
    return "(stops: " + Arrays.toString(stops) + ")";
  }
}
