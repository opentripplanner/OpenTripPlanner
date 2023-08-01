package org.opentripplanner.raptor.rangeraptor.path;

import static org.opentripplanner.raptor.api.path.RaptorPath.compareArrivalTime;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareC1;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareDepartureTime;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareDuration;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareIterationDepartureTime;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareNumberOfTransfers;

import javax.annotation.Nonnull;
import org.opentripplanner.raptor.api.model.DominanceFunction;
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
   * Create pareto-set comparison function.
   * @param includeC1 Whether to include generalized cost as a criteria.
   * @param includeTimetable // TODO: 2023-07-31 What is this parameter doing exactly?
   * @param preferLateArrival // TODO: 2023-07-31 What is this parameter doing exactly?
   * @param relaxC1 Relax function for the generalized cost
   * @param c2Comp Dominance function for accumulated criteria TWO. If function is null, C2 will not be included in the comparison.
   */
  public static <T extends RaptorTripSchedule> ParetoComparator<RaptorPath<T>> paretoComparator(
    final boolean includeC1,
    final boolean includeTimetable,
    final boolean preferLateArrival,
    final SearchDirection searchDirection,
    final RelaxFunction relaxC1,
    final DominanceFunction c2Comp
  ) {
    boolean includeRelaxedCost = includeC1 && !relaxC1.isNormal();
    boolean preferLatestDeparture = preferLateArrival != searchDirection.isInReverse();

    if (includeRelaxedCost) {
      if (includeTimetable) {
        if (c2Comp != null) {
          return comparatorTimetableAndRelaxedC1AndC2(relaxC1, c2Comp);
        } else {
          return comparatorTimetableAndRelaxedC1(relaxC1);
        }
      }
      if (preferLateArrival) {
        if (c2Comp != null) {
          return comparatorDepartureTimeAndRelaxedC1AndC2(relaxC1, c2Comp);
        } else {
          return comparatorDepartureTimeAndRelaxedC1(relaxC1);
        }
      } else {
        if (c2Comp != null) {
          return comparatorArrivalTimeAndRelaxedC1AndC2(relaxC1, c2Comp);
        } else {
          return comparatorArrivalTimeAndRelaxedC1(relaxC1);
        }
      }
    }

    if (includeC1) {
      if (includeTimetable) {
        if (c2Comp != null) {
          return comparatorTimetableAndC1AndC2(c2Comp);
        } else {
          return comparatorTimetableAndC1();
        }
      }
      if (preferLatestDeparture) {
        if (c2Comp != null) {
          return comparatorDepartureTimeAndC1AndC2(c2Comp);
        } else {
          return comparatorDepartureTimeAndC1();
        }
      }
      if (c2Comp != null) {
        return comparatorWithC1AndC2(c2Comp);
      } else {
        return comparatorWithC1();
      }
    }

    if (includeTimetable) {
      if (c2Comp != null) {
        return comparatorTimetableAndC2(c2Comp);
      } else {
        return comparatorTimetable();
      }
    }
    if (preferLatestDeparture) {
      if (c2Comp != null) {
        return comparatorStandardDepartureTimeAndC2(c2Comp);
      } else {
        return comparatorStandardDepartureTime();
      }
    }
    if (c2Comp != null) {
      return comparatorStandardArrivalTimeAndC2(c2Comp);
    }
    return comparatorStandardArrivalTime();
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorStandardArrivalTime() {
    return (l, r) -> compareArrivalTime(l, r) || compareNumberOfTransfers(l, r);
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorStandardArrivalTimeAndC2(
    @Nonnull final DominanceFunction c2Comp
  ) {
    return (l, r) ->
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      c2Comp.leftDominateRight(l.c2(), r.c2());
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorStandardDepartureTime() {
    return (l, r) -> compareDepartureTime(l, r) || compareNumberOfTransfers(l, r);
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorStandardDepartureTimeAndC2(
    @Nonnull final DominanceFunction c2Comp
  ) {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      c2Comp.leftDominateRight(l.c2(), r.c2());
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorTimetable() {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r);
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorTimetableAndC2(
    @Nonnull final DominanceFunction c2Comp
  ) {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      c2Comp.leftDominateRight(l.c2(), r.c2());
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorTimetableAndC1() {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(l, r);
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorTimetableAndRelaxedC1(
    @Nonnull final RelaxFunction relaxCost
  ) {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(relaxCost, l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<RaptorPath<T>> comparatorWithC1() {
    return (l, r) ->
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(l, r);
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorDepartureTimeAndC1() {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(l, r);
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorArrivalTimeAndRelaxedC1(
    @Nonnull final RelaxFunction relaxCost
  ) {
    return (l, r) ->
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(relaxCost, l, r);
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorDepartureTimeAndRelaxedC1(
    @Nonnull final RelaxFunction relaxCost
  ) {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(relaxCost, l, r);
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorTimetableAndC1AndC2(
    @Nonnull final DominanceFunction c2Comp
  ) {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(l, r) ||
      c2Comp.leftDominateRight(l.c2(), r.c2());
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorTimetableAndRelaxedC1AndC2(
    @Nonnull final RelaxFunction relaxCost,
    @Nonnull final DominanceFunction c2Comp
  ) {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(relaxCost, l, r) ||
      c2Comp.leftDominateRight(l.c2(), r.c2());
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorWithC1AndC2(@Nonnull final DominanceFunction c2Comp) {
    return (l, r) ->
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(l, r) ||
      c2Comp.leftDominateRight(l.c2(), r.c2());
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorDepartureTimeAndC1AndC2(
    @Nonnull final DominanceFunction c2Comp
  ) {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(l, r) ||
      c2Comp.leftDominateRight(l.c2(), r.c2());
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorArrivalTimeAndRelaxedC1AndC2(
    @Nonnull final RelaxFunction relaxCost,
    @Nonnull final DominanceFunction c2Comp
  ) {
    return (l, r) ->
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(relaxCost, l, r) ||
      c2Comp.leftDominateRight(l.c2(), r.c2());
  }

  private static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorDepartureTimeAndRelaxedC1AndC2(
    @Nonnull final RelaxFunction relaxCost,
    @Nonnull final DominanceFunction c2Comp
  ) {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDuration(l, r) ||
      compareC1(relaxCost, l, r) ||
      c2Comp.leftDominateRight(l.c2(), r.c2());
  }
}
