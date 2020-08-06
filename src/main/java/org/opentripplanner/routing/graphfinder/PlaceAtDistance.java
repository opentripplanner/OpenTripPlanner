package org.opentripplanner.routing.graphfinder;

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
}
