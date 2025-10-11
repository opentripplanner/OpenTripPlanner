package org.opentripplanner.graph_builder.module.osm.naming;

import java.util.Arrays;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;

class NamerTestUtils {

  public static StreetEdgeBuilder<?> edgeBuilder(WgsCoordinate... c) {
    var coordinates = Arrays.stream(c).toList();
    var ls = GeometryUtils.makeLineString(c);
    return new StreetEdgeBuilder<>()
      .withFromVertex(
        StreetModelForTest.intersectionVertex(coordinates.getFirst().asJtsCoordinate())
      )
      .withToVertex(StreetModelForTest.intersectionVertex(coordinates.getLast().asJtsCoordinate()))
      .withGeometry(ls);
  }
}
