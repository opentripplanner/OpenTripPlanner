package org.opentripplanner.routing.linking;

import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.street.geometry.GeometryUtils;
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

  public static TransitStopVertexBuilder ofStop(int id, Coordinate coordinate) {
    return TransitStopVertex.of()
      .withId(id(id))
      .withPoint(GeometryUtils.getGeometryFactory().createPoint(coordinate));
  }
}
