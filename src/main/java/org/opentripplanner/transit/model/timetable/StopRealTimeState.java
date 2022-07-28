package org.opentripplanner.transit.model.timetable;

/**
 * The realtime state of a stop in a trip.
 */
public enum StopRealTimeState {
  /**
   * Default state. Trip's realtime state should then determine the state of the stop.
   */
  DEFAULT,

  /**
   * The realtime data on the stop is inaccurate.
   */
  INACCURATE_PREDICTIONS,

  /**
   * The realtime feed has indicated that there is no data available for this stop.
   */
  NO_DATA,

  /**
   * The vehicle has arrived to the stop or already visited it and the times are no longer
   * estimates.
   */
  RECORDED,

  /**
   * The stop is cancelled.
   */
  CANCELLED,
}
