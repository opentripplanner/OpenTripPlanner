package org.opentripplanner.raptor.rangeraptor.path;

import static org.opentripplanner.raptor.api.path.RaptorPath.compareArrivalTime;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareC1;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareC2;
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
    boolean includeC1,
    boolean includeC2,
    boolean includeTimetable,
    boolean preferLateArrival,
    SearchDirection searchDirection,
    RelaxFunction relaxC1
  ) {
    boolean includeRelaxedCost = includeC1 && !relaxC1.isNormal();
    boolean preferLatestDeparture = preferLateArrival != searchDirection.isInReverse();

    if (includeRelaxedCost) {
      if (includeTimetable) {
        if (includeC2) {
          return comparatorTimetableAndRelaxedC1AndC2(relaxC1);
        } else {
          return comparatorTimetableAndRelaxedC1(relaxC1);
        }
      }
      if (preferLateArrival) {
        if (includeC2) {
          return comparatorDepartureTimeAndRelaxedC1AndC2(relaxC1);
        } else {
          return comparatorDepartureTimeAndRelaxedC1(relaxC1);
        }
      } else {
        if (includeC2) {
          return comparatorArrivalTimeAndRelaxedC1AndC2(relaxC1);
        } else {
          return comparatorArrivalTimeAndRelaxedC1(relaxC1);
        }
      }
    }

    if (includeC1) {
      if (includeTimetable) {
        if (includeC2) {
          return comparatorTimetableAndC1AndC2();
        } else {
          return comparatorTimetableAndC1();
        }
      }
      if (preferLatestDeparture) {
        if (includeC2) {
          return comparatorDepartureTimeAndC1AndC2();
        } else {
          return comparatorDepartureTimeAndC1();
        }
      }
      if (includeC2) {
        return comparatorWithC1AndC2();
      } else {
        return comparatorWithC1();
      }
    }

    if (includeTimetable) {
      if (includeC2) {
        return comparatorTimetableAndC2();
      } else {
        return comparatorTimetable();
      }
    }
    if (preferLatestDeparture) {
      if (includeC2) {
        return comparatorStandardDepartureTimeAndC2();
      } else {
        return comparatorStandardDepartureTime();
      }
    }
    if (includeC2) {
      return comparatorStandardArrivalTimeAndC2();
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
  > ParetoComparator<RaptorPath<T>> comparatorStandardArrivalTimeAndC2() {
    return (l, r) -> compareArrivalTime(l, r) || compareNumberOfTransfers(l, r) || compareC2(l, r);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorStandardDepartureTime() {
    return (l, r) -> compareDepartureTime(l, r) || compareNumberOfTransfers(l, r);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorStandardDepartureTimeAndC2() {
    return (l, r) ->
      compareDepartureTime(l, r) || compareNumberOfTransfers(l, r) || compareC2(l, r);
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
  > ParetoComparator<RaptorPath<T>> comparatorTimetableAndC2() {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareC2(l, r);
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

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorTimetableAndC1AndC2() {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(l, r) ||
      compareC2(l, r);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorTimetableAndRelaxedC1AndC2(
    @Nonnull RelaxFunction relaxCost
  ) {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(relaxCost, l, r) ||
      compareC2(l, r);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorWithC1AndC2() {
    return (l, r) ->
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(l, r) ||
      compareC2(l, r);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorDepartureTimeAndC1AndC2() {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(l, r) ||
      compareC2(l, r);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorArrivalTimeAndRelaxedC1AndC2(
    RelaxFunction relaxCost
  ) {
    return (l, r) ->
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(relaxCost, l, r) ||
      compareC2(l, r);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorDepartureTimeAndRelaxedC1AndC2(
    RelaxFunction relaxCost
  ) {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(relaxCost, l, r) ||
      compareC2(l, r);
  }
}
