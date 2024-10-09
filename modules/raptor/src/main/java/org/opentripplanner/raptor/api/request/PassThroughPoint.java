package org.opentripplanner.raptor.api.request;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.opentripplanner.framework.lang.StringUtils;

/**
 * A collection of stop indexes used to define a pass through-point.
 */
public class PassThroughPoint {

  private final String name;
  private final int[] stops;

  public PassThroughPoint(@Nullable String name, int... stops) {
    Objects.requireNonNull(stops);
    if (stops.length == 0) {
      throw new IllegalArgumentException("At least one stop is required");
    }
    this.name = StringUtils.hasNoValue(name) ? null : name;
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

  /**
   * We want the {@code toString()} to be as easy to read as possible for value objects, so the
   * format for this class is:
   * <p>
   * {@code "(<NAME>, <LIST_OF_STOP_INDEXES>)"}, for example:
   * {@code "(PT-Name, 1, 12, 123)"} or without the name {@code "(1, 12, 123)"}
   */
  @Override
  public String toString() {
    return toString(Integer::toString);
  }

  public String toString(IntFunction<String> nameResolver) {
    StringBuilder buf = new StringBuilder("(");
    if (name != null) {
      buf.append(name).append(", ");
    }
    buf.append("stops: ");
    appendStops(buf, ", ", nameResolver);
    return buf.append(")").toString();
  }

  public void appendStops(StringBuilder buf, String sep, IntFunction<String> nameResolver) {
    boolean skipFirst = true;
    for (int stop : stops) {
      if (skipFirst) {
        skipFirst = false;
      } else {
        buf.append(sep);
      }
      buf.append(nameResolver.apply(stop));
    }
  }
}
