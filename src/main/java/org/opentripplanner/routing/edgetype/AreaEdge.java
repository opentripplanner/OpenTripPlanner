package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.transit.model.basic.I18NString;

public class AreaEdge extends StreetEdge {

  private final AreaEdgeList area;

  public AreaEdge(
    IntersectionVertex startEndpoint,
    IntersectionVertex endEndpoint,
    LineString geometry,
    I18NString name,
    double length,
    StreetTraversalPermission permissions,
    boolean back,
    AreaEdgeList area
  ) {
    super(startEndpoint, endEndpoint, geometry, name, length, permissions, back);
    this.area = area;
  }

  public AreaEdgeList getArea() {
    return area;
  }
}
