package org.opentripplanner.ext.restapi.model;

import org.opentripplanner.transit.model.timetable.RealTimeState;

/**
 * The real-time state of a trip
 */
public enum ApiRealTimeState {
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
  MODIFIED;

  public static ApiRealTimeState RealTimeState(RealTimeState other) {
    switch (other) {
      case SCHEDULED:
        return ApiRealTimeState.SCHEDULED;
      case UPDATED:
        return ApiRealTimeState.UPDATED;
      case CANCELED:
        return ApiRealTimeState.CANCELED;
      case ADDED:
        return ApiRealTimeState.ADDED;
      case MODIFIED:
        return ApiRealTimeState.MODIFIED;
      default:
        throw new IllegalArgumentException();
    }
  }
}
