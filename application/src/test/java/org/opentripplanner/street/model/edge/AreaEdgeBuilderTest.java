package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.StreetVertex;

class AreaEdgeBuilderTest {

  private static final StreetVertex FROM_VERTEX = StreetModelForTest.V1;
  private static final StreetVertex TO_VERTEX = StreetModelForTest.V2;
  private static final StreetTraversalPermission STREET_TRAVERSAL_PERMISSION =
    StreetTraversalPermission.ALL;

  private static final I18NString NAME = I18NString.of("area-edge-name");
  private static final LineString GEOMETRY = GeometryUtils
    .getGeometryFactory()
    .createLineString(new Coordinate[] { FROM_VERTEX.getCoordinate(), TO_VERTEX.getCoordinate() });

  private static final AreaGroup AREA = new AreaGroup(null);

  @Test
  void buildAndConnect() {
    AreaEdge areaEdge = new AreaEdgeBuilder()
      .withFromVertex(FROM_VERTEX)
      .withToVertex(TO_VERTEX)
      .withGeometry(GEOMETRY)
      .withPermission(STREET_TRAVERSAL_PERMISSION)
      .withName(NAME)
      .withArea(AREA)
      .buildAndConnect();
    assertEquals(AREA, areaEdge.getArea());
  }
}
