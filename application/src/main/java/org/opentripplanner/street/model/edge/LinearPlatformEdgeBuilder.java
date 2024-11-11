package org.opentripplanner.street.model.edge;

public class LinearPlatformEdgeBuilder extends StreetEdgeBuilder<LinearPlatformEdgeBuilder> {

  private LinearPlatform platform;

  @Override
  public LinearPlatformEdge buildAndConnect() {
    return Edge.connectToGraph(new LinearPlatformEdge(this));
  }

  public LinearPlatform platform() {
    return platform;
  }

  public LinearPlatformEdgeBuilder withPlatform(LinearPlatform platform) {
    this.platform = platform;
    return this;
  }
}
