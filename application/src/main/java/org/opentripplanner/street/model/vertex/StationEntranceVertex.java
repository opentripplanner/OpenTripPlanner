package org.opentripplanner.street.model.vertex;

public class StationEntranceVertex extends OsmVertex {

  private final String code;

  public StationEntranceVertex(double x, double y, long nodeId, String code) {
    super(x, y, nodeId);
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public String toString() {
    return "StationEntranceVertex(" + super.toString() + ", code=" + code + ")";
  }
}
