package org.opentripplanner.street.model.vertex;

import org.opentripplanner.routing.graph.Graph;

public class ExitVertex extends OsmVertex {

  private String exitName;

  public ExitVertex(String label, double x, double y, long nodeId) {
    super(label, x, y, nodeId);
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
