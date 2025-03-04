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

class TemporaryPartialStreetEdgeBuilderTest {

  private static final StreetVertex FROM_VERTEX = StreetModelForTest.V1;
  private static final StreetVertex TO_VERTEX = StreetModelForTest.V2;
  public static final StreetEdge PARENT_EDGE = StreetModelForTest.streetEdge(
    StreetModelForTest.V3,
    StreetModelForTest.V4
  );
  private static final StreetTraversalPermission STREET_TRAVERSAL_PERMISSION =
    StreetTraversalPermission.ALL;

  private static final I18NString NAME = I18NString.of("temporary-partial-street-edge-name");
  private static final LineString GEOMETRY = GeometryUtils.getGeometryFactory()
    .createLineString(new Coordinate[] { FROM_VERTEX.getCoordinate(), TO_VERTEX.getCoordinate() });

  @Test
  void buildAndConnect() {
    TemporaryPartialStreetEdge tpse = new TemporaryPartialStreetEdgeBuilder()
      .withFromVertex(FROM_VERTEX)
      .withToVertex(TO_VERTEX)
      .withGeometry(GEOMETRY)
      .withPermission(STREET_TRAVERSAL_PERMISSION)
      .withName(NAME)
      .withParentEdge(PARENT_EDGE)
      .buildAndConnect();
    assertEquals(PARENT_EDGE, tpse.getParentEdge());
    assertEquals(GEOMETRY, tpse.getGeometry());
  }
}
