package org.opentripplanner.routing.linking;

import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertexBuilder;
import org.opentripplanner.transit.model.site.RegularStop;

public class TransitStopVertexBuilderFactory {

  public static TransitStopVertexBuilder ofStop(RegularStop stop) {
    return TransitStopVertex.of()
      .withId(stop.getId())
      .withPoint(stop.getGeometry())
      .withWheelchairAccessiblity(stop.getWheelchairAccessibility());
  }
}
