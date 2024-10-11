package org.opentripplanner.transit.model.timetable;

/**
 * The real-time state of a trip
 */
public enum RealTimeState {
  /**
   * The trip information comes from the GTFS feed, i.e. no real-time update has been applied.
   */
  SCHEDULED,

  /**
   * The trip information has been updated, but the trip pattern stayed the same as the trip pattern
   * of the scheduled trip.
   */
  UPDATED,

  /**
   * The trip has been canceled by a real-time update.
   */
  CANCELED,

  /**
   * The trip has been added using a real-time update, i.e. the trip was not present in the GTFS
   * feed.
   */
  ADDED,

  /**
   * The trip information has been updated and resulted in a different trip pattern compared to the
   * trip pattern of the scheduled trip.
   */
  MODIFIED,

  /**
   * The trip should not be visible to the end user. Either it has been set as deleted in the
   * real-time feed, or it has been replaced by another trip on another pattern.
   */
  DELETED,
}
