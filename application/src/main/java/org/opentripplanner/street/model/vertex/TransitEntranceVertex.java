package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TransitEntranceVertex extends StationElementVertex {

  private final Accessibility wheelchairAccessibility;

  public TransitEntranceVertex(
    FeedScopedId id,
    WgsCoordinate coordinate,
    I18NString name,
    Accessibility accessibility
  ) {
    super(id, coordinate.longitude(), coordinate.latitude(), name);
    this.wheelchairAccessibility = accessibility;
  }

  public Accessibility getWheelchairAccessibility() {
    return wheelchairAccessibility;
  }
}
