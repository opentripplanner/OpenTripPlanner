package org.opentripplanner.model;

/**
 * The direction of travel for a TripPattern.
 */
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
    switch (gtfsCode) {
      case 0:
        return Direction.OUTBOUND;
      case 1:
        return Direction.INBOUND;
      default:
        return Direction.UNKNOWN;
    }
  }
}
