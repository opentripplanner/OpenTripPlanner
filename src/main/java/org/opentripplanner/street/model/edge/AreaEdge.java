package org.opentripplanner.street.model.edge;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

public class AreaEdge extends StreetEdge {

  private final AreaEdgeList area;

  private AreaEdge(
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

  // constructor without precomputed length
  private AreaEdge(
    IntersectionVertex startEndpoint,
    IntersectionVertex endEndpoint,
    LineString geometry,
    I18NString name,
    StreetTraversalPermission permissions,
    boolean back,
    AreaEdgeList area
  ) {
    super(startEndpoint, endEndpoint, geometry, name, permissions, back);
    this.area = area;
  }

  public static AreaEdge createAreaEdge(
    IntersectionVertex startEndpoint,
    IntersectionVertex endEndpoint,
    LineString geometry,
    I18NString name,
    double length,
    StreetTraversalPermission permissions,
    boolean back,
    AreaEdgeList area
  ) {
    return connectToGraph(
      new AreaEdge(startEndpoint, endEndpoint, geometry, name, length, permissions, back, area)
    );
  }

  public static AreaEdge createAreaEdge(
    IntersectionVertex startEndpoint,
    IntersectionVertex endEndpoint,
    LineString geometry,
    I18NString name,
    StreetTraversalPermission permissions,
    boolean back,
    AreaEdgeList area
  ) {
    return connectToGraph(
      new AreaEdge(startEndpoint, endEndpoint, geometry, name, permissions, back, area)
    );
  }

  public AreaEdgeList getArea() {
    return area;
  }
}
