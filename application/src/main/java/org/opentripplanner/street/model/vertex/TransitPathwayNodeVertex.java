package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TransitPathwayNodeVertex extends StationElementVertex {

  public TransitPathwayNodeVertex(FeedScopedId id, WgsCoordinate coordinate, I18NString name) {
    super(id, coordinate.longitude(), coordinate.latitude(), name);
  }
}
