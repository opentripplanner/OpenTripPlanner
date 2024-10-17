package org.opentripplanner.transit.model.timetable;

/**
 * The direction of travel for a TripPattern. This is mapped 1-to-1 in NeTEx, while in GTFS only
 * values 0 and 1 are available, so they are mapped to OUTBOUND and INBOUND. When mapping from the
 * model to the REST API, CLOCKWISE and ANTICLOCKWISE are also mapped to 0 and 1 (as they would also
 * fit the description in the GTFS specification).
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
