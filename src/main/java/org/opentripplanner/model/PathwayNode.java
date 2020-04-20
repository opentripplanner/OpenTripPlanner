/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

/**
 * A place where a station connects to the street network. Equivalent to GTFS stop location .
 */
public final class PathwayNode extends StationElement {

  public PathwayNode(
      FeedScopedId id,
      String name,
      String code,
      String description,
      WgsCoordinate coordinate,
      WheelChairBoarding wheelchairBoarding,
      StopLevel level
      ) {
    super(id, name, code, description, coordinate, wheelchairBoarding, level);
  }

  @Override
  public String toString() {
    return "<PathwayNode " + this.id + ">";
  }
}
