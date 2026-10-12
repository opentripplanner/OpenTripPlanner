package org.opentripplanner.raptor.rangeraptor.internalapi;

/**
 * These are the different time configurations Raptor supports. Each configuration will
 * be used to change the pareto-function.
 */
public enum ParetoSetTime {
  /**
   * Uses iteration-departure-time and arrival-time as criteria in pareto function. Note!
   * iteration-departure-time is slightly different from the more precise departure-time.
   * <p>
   * For forward searches: primary = iterationDepartureTime (prefer later origin departure),
   * secondary = arrivalTime (prefer earlier destination arrival).
   */
  USE_TIMETABLE,
  /**
   * Like {@link #USE_TIMETABLE} but for reverse (arrive-by) searches. In a reverse search the
   * {@link org.opentripplanner.raptor.rangeraptor.path.DestinationArrival} fields have swapped
   * semantics: {@code arrivalTime} holds the forward origin-departure time and
   * {@code iterationDepartureTime} holds the forward destination-arrival time. The comparator
   * criteria are correspondingly swapped and their directions inverted.
   */
  USE_TIMETABLE_REVERSE,
  /**
   * Uses arrival-time as criteria in pareto function.
   */
  USE_ARRIVAL_TIME,
  /**
   * Uses departure-time as criteria in pareto function.
   */
  USE_DEPARTURE_TIME,
}
