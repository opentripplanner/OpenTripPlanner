package org.opentripplanner.street.model.vertex;

import org.opentripplanner.routing.graph.Graph;

public class LevelVertex extends OsmVertex {

  private final String level;

  public LevelVertex(Graph g, double x, double y, long nodeId, String level) {
    super(g, x, y, nodeId);
    this.level = level;
  }

  @Override
  public String getLabel() {
    return "%s:level:%s".formatted(super.getLabel(), level);
  }
}
