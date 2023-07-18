package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.i18n.I18NString;

/**
 * Represents a location on a street, somewhere between the two corners. This is used when computing
 * the first and last segments of a trip, for trips that start or end between two intersections.
 * Also for situating bus stops in the middle of street segments.
 */
public class StreetLocation extends StreetVertex {

  private final I18NString name;
  private boolean wheelchairAccessible;

  private final VertexLabel label;

  // maybe name should just be pulled from street being split
  public StreetLocation(String id, Coordinate nearestPoint, I18NString name) {
    // calling constructor with null graph means this vertex is temporary
    super(nearestPoint.x, nearestPoint.y);
    this.label = VertexLabel.string(id);
    this.name = name;
  }

  public boolean isWheelchairAccessible() {
    return wheelchairAccessible;
  }

  public void setWheelchairAccessible(boolean wheelchairAccessible) {
    this.wheelchairAccessible = wheelchairAccessible;
  }

  @Override
  public VertexLabel getLabel() {
    return label;
  }

  @Override
  public I18NString getIntersectionName() {
    return getName();
  }

  @Nonnull
  @Override
  public I18NString getName() {
    return name;
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
