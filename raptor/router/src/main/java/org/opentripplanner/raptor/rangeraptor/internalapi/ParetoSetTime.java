package org.opentripplanner.raptor.rangeraptor.internalapi;

/**
 * The time criteria used in the pareto-function. This describes which time values the search
 * compares, nothing else. How the time criteria are combined with the other criteria and the
 * search direction is described in {@code DestinationArrivalParetoSetComparators} and in the
 * "Destination pareto-set" section of {@code org/opentripplanner/raptor/package.md}.
 */
public enum ParetoSetTime {
  /**
   * Uses iteration-departure-time and arrival-time. Note! The iteration-departure-time is slightly
   * different from the more precise departure-time.
   */
  USE_TIMETABLE,
  /** Uses arrival-time. */
  USE_ARRIVAL_TIME,
  /** Uses the time-shifted departure-time. */
  USE_DEPARTURE_TIME,
}
