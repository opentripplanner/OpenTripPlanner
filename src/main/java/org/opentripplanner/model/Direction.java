package org.opentripplanner.model;

public enum Direction {
  UNKNOWN(-1),
  OUTBOUND(0),
  INBOUND(1),
  CLOCKWISE(0),
  ANTICLOCKWISE(1);

  Direction(int gtfsCode) {
    this.gtfsCode = gtfsCode;
  }

  public final int gtfsCode;

  public static Direction valueOfGtfsCode(int gtfsCode) {
    // Because the enum constant share gtfsCode values, this mapping will depend on the order
    // they are declared.
    for (Direction value : values()) {
      if (value.gtfsCode == gtfsCode) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown GTFS direction type: " + gtfsCode);
  }
}
