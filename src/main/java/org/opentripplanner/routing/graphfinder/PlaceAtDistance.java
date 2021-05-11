package org.opentripplanner.routing.graphfinder;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A place of any of the types defined in PlaceType at a specified distance.
 *
 * @see PlaceType
 */
@ToString
@EqualsAndHashCode
public class PlaceAtDistance {

  public final Object place;
  public final double distance;

  public PlaceAtDistance(Object place, double distance) {
    this.place = place;
    this.distance = distance;
  }
}
