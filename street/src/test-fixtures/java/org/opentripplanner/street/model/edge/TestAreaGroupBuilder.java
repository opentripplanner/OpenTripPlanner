package org.opentripplanner.street.model.edge;

import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

public class TestAreaGroupBuilder {

  private final List<IntersectionVertex> boundaryVertices;
  private Set<IntersectionVertex> visibilityVertices = Set.of();

  public TestAreaGroupBuilder(IntersectionVertex... boundaryVertices) {
    if (boundaryVertices.length < 3) {
      throw new IllegalArgumentException("An area must have at least three boundary vertices.");
    }
    this.boundaryVertices = List.of(boundaryVertices);
  }

  public TestAreaGroupBuilder withVisibilityVertices(IntersectionVertex... visibilityVertices) {
    this.visibilityVertices = Set.of(visibilityVertices);
    return this;
  }

  public AreaGroup build() {
    var coordinates = new Coordinate[boundaryVertices.size() + 1];
    for (int i = 0; i < boundaryVertices.size(); i++) {
      coordinates[i] = boundaryVertices.get(i).getCoordinate();
    }
    coordinates[boundaryVertices.size()] = coordinates[0];

    var polygon = GeometryUtils.getGeometryFactory().createPolygon(coordinates);
    var areaGroup = new AreaGroup(polygon);
    areaGroup.addVisibilityVertices(visibilityVertices);

    var area = new Area();
    area.setName(I18NString.of("test area"));
    area.setWalkSafety(0.5f);
    area.setBicycleSafety(0.5f);
    area.setPermission(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
    area.setGeometry(polygon);
    areaGroup.addArea(area);

    for (int i = 0; i < boundaryVertices.size(); i++) {
      var from = boundaryVertices.get(i);
      var to = boundaryVertices.get((i + 1) % boundaryVertices.size());
      connectAreaEdge(from, to, areaGroup, false);
      connectAreaEdge(to, from, areaGroup, true);
    }
    return areaGroup;
  }

  private static void connectAreaEdge(
    IntersectionVertex from,
    IntersectionVertex to,
    AreaGroup areaGroup,
    boolean back
  ) {
    var geometry = GeometryUtils.getGeometryFactory().createLineString(
      new Coordinate[] { from.getCoordinate(), to.getCoordinate() }
    );
    new AreaEdgeBuilder()
      .withFromVertex(from)
      .withToVertex(to)
      .withGeometry(geometry)
      .withName(I18NString.of("area boundary"))
      .withPermission(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE)
      .withBack(back)
      .withArea(areaGroup)
      .buildAndConnect();
  }
}
