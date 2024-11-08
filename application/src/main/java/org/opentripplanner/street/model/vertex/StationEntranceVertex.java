package org.opentripplanner.street.model.vertex;

public class StationEntranceVertex extends OsmVertex {

  private final String code;
  private final boolean accessible;

  public StationEntranceVertex(double x, double y, long nodeId, String code, boolean accessible) {
    super(x, y, nodeId);
    this.code = code;
    this.accessible = accessible;
  }

  public String getCode() {
    return code;
  }

  public boolean isAccessible() {
    return accessible;
  }

  public String getId() {
    return Long.toString(nodeId);
  }

  public String toString() {
    return "StationEntranceVertex(" + super.toString() + ", code=" + code + ")";
  }
}
