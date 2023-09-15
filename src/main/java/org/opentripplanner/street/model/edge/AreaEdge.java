package org.opentripplanner.street.model.edge;

public class AreaEdge extends StreetEdge {

  private final AreaEdgeList area;

  protected AreaEdge(AreaEdgeBuilder builder) {
    super(builder);
    this.area = builder.area();
  }

  public AreaEdgeList getArea() {
    return area;
  }
}
