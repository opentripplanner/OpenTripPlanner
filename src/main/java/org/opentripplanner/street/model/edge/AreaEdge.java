package org.opentripplanner.street.model.edge;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
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
