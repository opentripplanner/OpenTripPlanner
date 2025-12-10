package org.opentripplanner.street.model.vertex;

import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.basic.Accessibility;

public class TransitBoardingAreaVertex extends StationElementVertex {

  private final boolean wheelchairAccessible;

  public TransitBoardingAreaVertex(
    FeedScopedId id,
    WgsCoordinate coordinate,
    I18NString name,
    Accessibility accessibility
  ) {
    super(id, coordinate.longitude(), coordinate.latitude(), name);
    this.wheelchairAccessible = accessibility != Accessibility.NOT_POSSIBLE;
  }

  public boolean isWheelchairAccessible() {
    return wheelchairAccessible;
  }
}
