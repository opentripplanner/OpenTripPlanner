package org.opentripplanner.raptor.rangeraptor.path;

import static org.opentripplanner.raptor.api.path.RaptorPath.compareArrivalTime;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareC1;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareDepartureTime;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareDuration;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareIterationDepartureTime;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareNumberOfTransfers;

import javax.annotation.Nonnull;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

/**
 * List of different pareto set comparators. Earlier we created these dynamically, but that affect
 * the performance, so it is better to have one function for each use case.
 * <p/>
 * All comparators include the "standard" set of criteria:
 * <ul>
 *     <li>Arrival Time</li>
 *     <li>Number of transfers</li>
 *     <li>Total travel duration time</li>
 * </ul>
 * The {@code travelDuration} is added as a criteria to the pareto comparator in addition to the
 * parameters used for each stop arrivals. The {@code travelDuration} is only needed at the
 * destination, because Range Raptor works in iterations backwards in time.
 */
public class PathParetoSetComparators {

  /** Prevent this utility class from instantiation. */
  private PathParetoSetComparators() {}

  /**
   * TODO This method should have unit tests.
   */
  public static <T extends RaptorTripSchedule> ParetoComparator<RaptorPath<T>> paretoComparator(
    boolean includeCost,
    boolean includeTimetable,
    boolean preferLateArrival,
    SearchDirection searchDirection,
    RelaxFunction relaxC1
  ) {
    boolean includeRelaxedCost = includeCost && !relaxC1.isNormal();
    boolean preferLatestDeparture = preferLateArrival != searchDirection.isInReverse();

    if (includeRelaxedCost) {
      if (includeTimetable) {
        return comparatorTimetableAndRelaxedC1(relaxC1);
      }
      if (preferLateArrival) {
        return comparatorDepartureTimeAndRelaxedC1(relaxC1);
      } else {
        return comparatorArrivalTimeAndRelaxedC1(relaxC1);
      }
    }

    if (includeCost) {
      if (includeTimetable) {
        return comparatorTimetableAndC1();
      }
      if (preferLatestDeparture) {
        return comparatorDepartureTimeAndC1();
      }
      return comparatorWithC1();
    }

    if (includeTimetable) {
      return comparatorTimetable();
    }
    if (preferLatestDeparture) {
      return comparatorStandardDepartureTime();
    }
    return comparatorStandardArrivalTime();
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorStandardArrivalTime() {
    return (l, r) -> compareArrivalTime(l, r) || compareNumberOfTransfers(l, r);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorStandardDepartureTime() {
    return (l, r) -> compareDepartureTime(l, r) || compareNumberOfTransfers(l, r);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorTimetable() {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorTimetableAndC1() {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(l, r);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorTimetableAndRelaxedC1(
    @Nonnull RelaxFunction relaxCost
  ) {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(relaxCost, l, r);
  }

  public static <T extends RaptorTripSchedule> ParetoComparator<RaptorPath<T>> comparatorWithC1() {
    return (l, r) ->
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(l, r);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorDepartureTimeAndC1() {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(l, r);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorArrivalTimeAndRelaxedC1(RelaxFunction relaxCost) {
    return (l, r) ->
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(relaxCost, l, r);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorDepartureTimeAndRelaxedC1(RelaxFunction relaxCost) {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(relaxCost, l, r);
  }
}
