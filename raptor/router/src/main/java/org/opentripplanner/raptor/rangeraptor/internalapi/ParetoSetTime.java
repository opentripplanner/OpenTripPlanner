package org.opentripplanner.raptor.rangeraptor.internalapi;

/**
 * These are the different time configurations Raptor supports. Each configuration will
 * be used to change the pareto-function.
 * <p>
 * The destination pareto-set is described in the "Destination pareto-set" section of
 * {@code org/opentripplanner/raptor/package.md}. In short: A timetable search keeps a journey that
 * departs later, even if it is slower, while the other modes only keep it if it is faster (or
 * arrives earlier, or departs later without cost). The {@code _REVERSE} variants are used for
 * reverse searches, where the time criteria have swapped meaning.
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
   * semantics: {@code arrivalTime} holds the origin departure time of the journey and
   * {@code iterationDepartureTime} holds the journey arrival time. The comparator
   * criteria are correspondingly swapped and their directions inverted.
   */
  USE_TIMETABLE_REVERSE,
  /**
   * Uses arrival-time as criteria in pareto function. If cost is used, the duration of the journey
   * is added as well, using the time-shifted departure time.
   */
  USE_ARRIVAL_TIME,
  /**
   * Like {@link #USE_ARRIVAL_TIME} but for reverse searches. The journey arrival time is the
   * departure time of the reverse search.
   */
  USE_ARRIVAL_TIME_REVERSE,
  /**
   * Uses the time-shifted departure-time as criteria in pareto function. If cost is used, the
   * duration of the journey is added as well.
   */
  USE_DEPARTURE_TIME,
  /**
   * Like {@link #USE_DEPARTURE_TIME} but for reverse searches. The journey departure time is the
   * arrival time of the reverse search.
   */
  USE_DEPARTURE_TIME_REVERSE,
}
