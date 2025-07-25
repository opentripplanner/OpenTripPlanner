package org.opentripplanner.osm;

public enum TraverseDirection {
  FORWARD,
  BACKWARD;

  public String tagSuffix() {
    return ":" + name().toLowerCase();
  }

  public TraverseDirection reverse() {
    return switch (this) {
      case FORWARD -> BACKWARD;
      case BACKWARD -> FORWARD;
    };
  }
}
