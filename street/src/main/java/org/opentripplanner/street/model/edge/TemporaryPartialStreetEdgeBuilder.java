package org.opentripplanner.street.model.edge;

public class TemporaryPartialStreetEdgeBuilder
  extends StreetEdgeBuilder<TemporaryPartialStreetEdgeBuilder> {

  private StreetEdge parentEdge;

  @Override
  public TemporaryPartialStreetEdge buildAndConnect() {
    return Edge.connectToGraph(new TemporaryPartialStreetEdge(this));
  }

  public StreetEdge parentEdge() {
    return parentEdge;
  }

  public TemporaryPartialStreetEdgeBuilder withParentEdge(StreetEdge parentEdge) {
    this.parentEdge = parentEdge;
    withPermission(parentEdge.getPermission());
    return this;
  }
}
