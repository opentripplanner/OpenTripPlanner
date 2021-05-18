package org.opentripplanner.routing.graphfinder;

import java.util.Objects;
import org.opentripplanner.model.base.ToStringBuilder;

/**
 * A place of any of the types defined in PlaceType at a specified distance.
 *
 * @see PlaceType
 */
public class PlaceAtDistance {

  public final Object place;
  public final double distance;

  public PlaceAtDistance(Object place, double distance) {
    this.place = place;
    this.distance = distance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    final PlaceAtDistance that = (PlaceAtDistance) o;
    return Double.compare(that.distance, distance) == 0
            && Objects.equals(place, that.place);
  }

  @Override
  public int hashCode() {
    return Objects.hash(place, distance);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass())
            .addObj("place", place)
            .addNum("distance", distance)
            .toString();
  }
}
