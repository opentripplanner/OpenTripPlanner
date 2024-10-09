package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.IntersectionVertex;

public class AreaEdgeBuilder extends StreetEdgeBuilder<AreaEdgeBuilder> {

  private AreaEdgeList area;

  @Override
  public AreaEdge buildAndConnect() {
    return Edge.connectToGraph(new AreaEdge(this));
  }

  @Override
  public IntersectionVertex fromVertex() {
    return (IntersectionVertex) super.fromVertex();
  }

  @Override
  public IntersectionVertex toVertex() {
    return (IntersectionVertex) super.toVertex();
  }

  public AreaEdgeList area() {
    return area;
  }

  public AreaEdgeBuilder withArea(AreaEdgeList area) {
    this.area = area;
    return this;
  }
}
