package org.opentripplanner.street.model.edge;

public class LinearPlatformEdge extends StreetEdge {

  public final LinearPlatform platform;

  protected LinearPlatformEdge(LinearPlatformEdgeBuilder builder) {
    super(builder);
    platform = builder.platform();
  }
}
