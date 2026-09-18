package org.opentripplanner.street.model.vertex;

import org.opentripplanner.core.domain.model.i18n.I18NString;
import org.opentripplanner.core.domain.model.id.FeedScopedId;
import org.opentripplanner.street.geometry.WgsCoordinate;

public class TransitBoardingAreaVertex extends StationElementVertex {

  public TransitBoardingAreaVertex(FeedScopedId id, WgsCoordinate coordinate, I18NString name) {
    super(id, coordinate.longitude(), coordinate.latitude(), name);
  }
}
