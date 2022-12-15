package org.opentripplanner.street.model.vertex;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;

/**
 * Represents a location on a street, somewhere between the two corners. This is used when computing
 * the first and last segments of a trip, for trips that start or end between two intersections.
 * Also for situating bus stops in the middle of street segments.
 */
public class StreetLocation extends StreetVertex {

  private boolean wheelchairAccessible;

  // maybe name should just be pulled from street being split
  public StreetLocation(String id, Coordinate nearestPoint, I18NString name) {
    // calling constructor with null graph means this vertex is temporary
    super(null, id, nearestPoint.x, nearestPoint.y, name);
  }

  //For tests only
  public StreetLocation(String id, Coordinate nearestPoint, String name) {
    // calling constructor with null graph means this vertex is temporary
    super(null, id, nearestPoint.x, nearestPoint.y, new NonLocalizedString(name));
  }

  public boolean isWheelchairAccessible() {
    return wheelchairAccessible;
  }

  public void setWheelchairAccessible(boolean wheelchairAccessible) {
    this.wheelchairAccessible = wheelchairAccessible;
  }

  @Override
  public I18NString getIntersectionName() {
    return super.getName();
  }

  @Override
  public int hashCode() {
    return getCoordinate().hashCode();
  }

  public boolean equals(Object o) {
    if (o instanceof StreetLocation) {
      StreetLocation other = (StreetLocation) o;
      return other.getCoordinate().equals(getCoordinate());
    }
    return false;
  }
}
