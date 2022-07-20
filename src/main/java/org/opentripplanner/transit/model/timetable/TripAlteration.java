package org.opentripplanner.transit.model.timetable;

/**
 * Alterations specified on a Trip in the planned data. This is in some ways equivalent with GTFS-RT
 * scheduled relationship.
 */
public enum TripAlteration {
  CANCELLATION,
  PLANNED,
  EXTRA_JOURNEY,
  REPLACED;

  public boolean isCanceledOrReplaced() {
    return this == CANCELLATION || this == REPLACED;
  }
}
