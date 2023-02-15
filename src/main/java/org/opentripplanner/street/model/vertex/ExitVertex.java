package org.opentripplanner.street.model.vertex;

import org.opentripplanner.routing.graph.Graph;

public class ExitVertex extends OsmVertex {

  private String exitName;

  public ExitVertex(Graph g, double x, double y, long nodeId) {
    super(g, x, y, nodeId);
  }

  public String getExitName() {
    return exitName;
  }

  public void setExitName(String exitName) {
    this.exitName = exitName;
  }

  public String toString() {
    return "ExitVertex(" + super.toString() + ")";
  }
}
