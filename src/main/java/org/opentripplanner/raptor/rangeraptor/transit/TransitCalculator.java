package org.opentripplanner.raptor.rangeraptor.transit;

import static org.opentripplanner.raptor.api.model.RaptorConstants.TIME_NOT_SET;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * The transit calculator is used to calculate transit related stuff, like calculating
 * <em>earliest boarding time</em> and time-shifting the access paths.
 * <p/>
 * The calculator is shared between the state, worker and path mapping code. This make the
 * calculations consistent and let us hide the request parameters. Hiding the request parameters
 * ensure that this calculator is used.
 * <p>
 * There is one calculator for FORWARD search and one for REVERSE search. The documentation and
 * argument names uses a search-direction agnostic vocabulary. We try to use the terms "source" and
 * "target", in stead of "from/to" and "board/alight".
 * <ul>
 * <li>
 *     In a FORWARD search the "source" means "from" and "TARGET" means "to".
 * </li>
 * <li>
 *     In a BACKWARD search the "source" means "to" and "TARGET" means "from". The traversal of the
 *     graph happens from the destination towards the origin - backwards in time. The "from/to"
 *     refer to the "natural way" we think about a journey, while "source/target" the destination
 *     is the source and the origin is the target in a BACKWARD search.
 * </li>
 * </ul>
 * "Source" and "target" may apply to stop-arrival, trip, board-/aligh-slack, and so on.
 * <p>
 * For a BACKWARD search the "source" means "from" (stop-arrival, trip, and so on).
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface TransitCalculator<T extends RaptorTripSchedule> extends TimeCalculator {
  /**
   * For a forward search return the trip arrival time at stop position including alightSlack. For a
   * reverse search return the next trips departure time at stop position with the boardSlack
   * added.
   *
   * @param trip                  the current boarded trip
   * @param stopPositionInPattern the stop position/index
   */
  int stopArrivalTime(T trip, int stopPositionInPattern, int slack);

  /**
   * Return {@code true} if it is allowed/possible to board at a particular stop index, on a normal
   * search. For a backwards search, it checks for alighting instead. This should include checks
   * like: Does the pattern allow boarding at the given stop? Is this accessible to wheelchairs (if
   * requested).
   */
  boolean boardingPossibleAt(RaptorTripPattern pattern, int stopPos);

  /**
   * Same as {@link #boardingPossibleAt(RaptorTripPattern, int)}, but for switched
   * alighting/boarding.
   */
  boolean alightingPossibleAt(RaptorTripPattern pattern, int stopPos);

  /**
   * Selects the earliest or latest possible departure time depending on the direction. For forward
   * search it will be the earliest possible departure time, while for reverse search it uses the
   * latest arrival time.
   * <p>
   * Returns {@link RaptorConstants#TIME_NOT_SET} if transfer
   * is not possible after the requested departure time
   */
  int departureTime(RaptorAccessEgress accessPath, int departureTime);

  /**
   * This method helps with calculating the egress departure time.
   * <ul>
   *   <li>It will add transit slack if egress has rides</li>
   *   <li>And time-shift the egress departure/arrival time</li>
   * </ul>
   * It will add transit slack (egress
   * leaves on-board) and then time-shift the egress.
   * <p>
   * It returns the calculated departure time or
   * {@link RaptorConstants#TIME_NOT_SET}
   * if boarding is not possible.
   */
  default int calculateEgressDepartureTime(
    int arrivalTime,
    RaptorAccessEgress egressPath,
    int transferSlack
  ) {
    return calculateEgressDepartureTime(this, arrivalTime, egressPath, transferSlack, true);
  }

  /**
   * This method does the same as {@link #calculateEgressDepartureTime(int, RaptorAccessEgress, int)},
   * but instead of time-shifting egress with opening hours the wait-time is ignored and
   * only the egress duration is added.
   */
  default int calculateEgressDepartureTimeWithoutTimeShift(
    int arrivalTime,
    RaptorAccessEgress egressPath,
    int transferSlack
  ) {
    return calculateEgressDepartureTime(this, arrivalTime, egressPath, transferSlack, false);
  }

  private static int calculateEgressDepartureTime(
    TransitCalculator<?> calculator,
    int arrivalTime,
    RaptorAccessEgress egressPath,
    int transferSlack,
    boolean includeTimeShift
  ) {
    int departureTime = arrivalTime;

    if (egressPath.hasRides()) {
      departureTime = calculator.plusDuration(departureTime, transferSlack);
    }
    int timeShiftedDepartureTime = calculator.searchForward()
      ? egressPath.earliestDepartureTime(departureTime)
      : egressPath.latestArrivalTime(departureTime);

    if (timeShiftedDepartureTime == TIME_NOT_SET) {
      return TIME_NOT_SET;
    }
    return includeTimeShift ? timeShiftedDepartureTime : departureTime;
  }
}
