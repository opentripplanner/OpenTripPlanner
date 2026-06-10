package org.opentripplanner.transit.model.timetable;

/**
 * Alterations specified on a Trip in the planned data. This only includes alterations in the
 * planned data, not realtime alterations from GTFS-RT or SIRI.
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
