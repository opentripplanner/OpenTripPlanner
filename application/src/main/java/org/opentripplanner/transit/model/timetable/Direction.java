package org.opentripplanner.transit.model.timetable;

/**
 * The direction of travel for a TripPattern. This is mapped 1-to-1 in NeTEx, while in GTFS only
 * values 0 and 1 are available, so they are mapped to OUTBOUND and INBOUND.
 */
public enum Direction {
  UNKNOWN(-1),
  OUTBOUND(0),
  INBOUND(1),
  CLOCKWISE(0),
  ANTICLOCKWISE(1);

  public final int gtfsCode;

  Direction(int gtfsCode) {
    this.gtfsCode = gtfsCode;
  }
}
