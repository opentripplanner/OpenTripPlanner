package org.opentripplanner.transit.model.site;

import org.opentripplanner.transit.model.base.WgsCoordinate;
import org.opentripplanner.transit.model.base.WheelchairAccessibility;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.util.I18NString;

/**
 * A place where a station connects to the street network. Equivalent to GTFS stop location .
 */
public final class PathwayNode extends StationElement {

  public PathwayNode(
    FeedScopedId id,
    I18NString name,
    String code,
    I18NString description,
    WgsCoordinate coordinate,
    WheelchairAccessibility wheelchairAccessibility,
    StopLevel level
  ) {
    super(id, name, code, description, coordinate, wheelchairAccessibility, level);
  }
}
