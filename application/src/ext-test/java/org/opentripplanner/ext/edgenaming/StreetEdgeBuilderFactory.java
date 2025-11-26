package org.opentripplanner.ext.edgenaming;

import java.util.Arrays;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;

/**
 * Helper class for creating {@link StreetEdgeBuilder} instances.
 */
class StreetEdgeBuilderFactory {

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
