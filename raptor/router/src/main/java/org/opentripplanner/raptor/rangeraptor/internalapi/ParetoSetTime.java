package org.opentripplanner.raptor.rangeraptor.internalapi;

/**
 * These are the different time configurations Raptor supports. Each configuration will
 * be used to change the pareto-function.
 * <p>
 * The destination pareto-set is described in the "Destination pareto-set" section of
 * {@code org/opentripplanner/raptor/package.md}. The time criteria are defined for the journey, not
 * for the search direction. In a reverse search the destination arrival time is the departure time
 * of the journey and the search departure time is the arrival time of the journey, the comparator
 * is given the search direction to handle this.
 */
public enum ParetoSetTime {
  /**
   * Uses iteration-departure-time and arrival-time as criteria in pareto function. Note!
   * iteration-departure-time is slightly different from the more precise departure-time.
   * <p>
   * In a forward search a later iteration-departure-time and an earlier arrival-time is better. In
   * a reverse search the directions are inverted.
   */
  USE_TIMETABLE,
  /**
   * Uses arrival-time as criteria in pareto function. If cost is used, the duration of the journey
   * is added as well, using the time-shifted departure time.
   */
  USE_ARRIVAL_TIME,
  /**
   * Uses the time-shifted departure-time as criteria in pareto function. If cost is used, the
   * duration of the journey is added as well.
   */
  USE_DEPARTURE_TIME,
}
