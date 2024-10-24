package org.opentripplanner.street.model.vertex;

public class StationEntranceVertex extends OsmVertex {

  private final String entranceName;

  public StationEntranceVertex(double x, double y, long nodeId, String entranceName) {
    super(x, y, nodeId);
    this.entranceName = entranceName;
  }

  public String getEntranceName() {
    return entranceName;
  }

  public String toString() {
    return "StationEntranceVertex(" + super.toString() + ")";
  }
}
