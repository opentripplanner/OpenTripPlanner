package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.Vertex;

public class AreaEdgeBuilder extends StreetEdgeBuilder<AreaEdgeBuilder> {

  private AreaEdgeList area;

  @Override
  public AreaEdge buildAndConnect() {
    return Edge.connectToGraph(new AreaEdge(this));
  }

  @Override
  public Vertex fromVertex() {
    return super.fromVertex();
  }

  @Override
  public Vertex toVertex() {
    return super.toVertex();
  }

  public AreaEdgeList area() {
    return area;
  }

  public AreaEdgeBuilder withArea(AreaEdgeList area) {
    this.area = area;
    return this;
  }
}
