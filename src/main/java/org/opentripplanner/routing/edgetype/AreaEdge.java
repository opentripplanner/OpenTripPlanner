package org.opentripplanner.routing.edgetype;

import java.util.Set;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.util.I18NString;

public class AreaEdge extends StreetEdge {

  private static final long serialVersionUID = 6761687673982054612L;
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
