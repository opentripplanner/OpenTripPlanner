package org.opentripplanner.street.model.edge;

public class AreaEdge extends StreetEdge {

  private final AreaGroup area;

  protected AreaEdge(AreaEdgeBuilder builder) {
    super(builder);
    this.area = builder.area();
  }

  public AreaGroup getArea() {
    return area;
  }
}
