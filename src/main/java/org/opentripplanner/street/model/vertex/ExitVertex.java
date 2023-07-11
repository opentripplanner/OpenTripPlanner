package org.opentripplanner.street.model.vertex;

public class ExitVertex extends OsmVertex {

  private final String exitName;

  public ExitVertex(double x, double y, long nodeId, String exitName) {
    super(x, y, nodeId);
    this.exitName = exitName;
  }

  public String getExitName() {
    return exitName;
  }

  public String toString() {
    return "ExitVertex(" + super.toString() + ")";
  }
}
