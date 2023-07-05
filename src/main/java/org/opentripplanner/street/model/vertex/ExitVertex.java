package org.opentripplanner.street.model.vertex;

public class ExitVertex extends OsmVertex {

  private String exitName;

  public ExitVertex(double x, double y, long nodeId) {
    super(x, y, nodeId);
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
