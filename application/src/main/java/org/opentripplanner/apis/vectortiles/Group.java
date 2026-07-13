package org.opentripplanner.apis.vectortiles;

/**
 * Enum for debug layer groups. In the debug UI they are shown on the layer selection menu.
 */
public enum Group {
  EDGES("Edges"),
  ELEVATION("Elevation"),
  WALK_SAFETY("Walk safety"),
  BICYCLE_SAFETY("Bicycle safety"),
  STOPS("Stops"),
  VERTICES("Vertices"),
  RENTAL("Rental"),
  PERMISSIONS("Permissions"),
  NO_THRU_TRAFFIC("No-thru traffic"),
  VERTICAL_TRANSPORTATION("Vertical transportation"),
  WHEELCHAIR("Wheelchair accessibility"),
  TRANSFERS("Transfers");

  private final String label;

  Group(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
