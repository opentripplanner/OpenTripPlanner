package org.opentripplanner.street.model.vertex;

import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.basic.Accessibility;

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
