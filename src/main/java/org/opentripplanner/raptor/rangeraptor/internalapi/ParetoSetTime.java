package org.opentripplanner.raptor.rangeraptor.internalapi;

/**
 * These are the different time configurations Raptor supports. Each configuration will
 * be used to change the pareto-function.
 */
public enum ParetoSetTime {
  /**
   * Uses iteration-departure-time and arrival-time as criteria in pareto function. Note!
   * iteration-departure-time is slightly different from the more precise departure-time.
   */
  USE_TIMETABLE,
  /**
   * Uses arrival-time as criteria in pareto function.
   */
  USE_ARRIVAL_TIME,
  /**
   * Uses departure-time as criteria in pareto function.
   */
  USE_DEPARTURE_TIME,
}
