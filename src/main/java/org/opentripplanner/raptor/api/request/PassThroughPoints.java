package org.opentripplanner.raptor.api.request;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.opentripplanner.framework.tostring.ToStringBuilder;

/**
 * An ordered set of pass through points.
 */
public class PassThroughPoints {

  private final List<PassThroughPoint> points;

  public PassThroughPoints(List<PassThroughPoint> points) {
    this.points = List.copyOf(Objects.requireNonNull(points));
  }

  public Stream<PassThroughPoint> stream() {
    return points.stream();
  }

  public boolean isEmpty() {
    return points.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PassThroughPoints that = (PassThroughPoints) o;
    return Objects.equals(points, that.points);
  }

  @Override
  public int hashCode() {
    return Objects.hash(points);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(PassThroughPoints.class).addCol("points", points).toString();
  }
}
