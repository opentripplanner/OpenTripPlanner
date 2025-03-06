package org.opentripplanner.raptor.rangeraptor.path;

import static org.opentripplanner.raptor.api.path.RaptorPath.compareArrivalTime;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareC1;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareDepartureTime;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareDurationInclusivePenalty;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareIterationDepartureTime;
import static org.opentripplanner.raptor.api.path.RaptorPath.compareNumberOfTransfers;

import java.util.Objects;
import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime;
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
 * Optional features are :
 * <ul>
 *     <li>Prefer late arrival - arriveBy search</li>
 *     <li>Include c1 - include c1 in pareto function (generalized-cost).</li>
 *     <li>Include c2 - include c2 in pareto function (custom criteria).</li>
 *     <li>Relax c1 - accept c1 values which is slightly worse than the best result.</li>
 *     <li>Relax c1, if c2 is optimal</li>
 * </ul>
 * The {@code travelDuration} is added as a criteria to the pareto comparator in addition to the
 * parameters used for each stop-arrival. The {@code travelDuration} is only needed at the
 * destination, because Range Raptor works in iterations backwards in time.
 */
public class PathParetoSetComparators {

  /** Prevent this utility class from instantiation. */
  private PathParetoSetComparators() {}

  /**
   * Create pareto-set comparison function.
   *
   * @param timeConfig Which time information (arrival-time, departure-time, or timetable) to include in comparator.
   * @param costConfig Supported configurations of c1, c2 and relaxed cost(c1).
   * @param relaxC1    Relax function for the generalized cost
   * @param c2Comp     Dominance function for accumulated criteria TWO. If function is null,
   *                   C2 will not be included in the comparison.
   */
  public static <T extends RaptorTripSchedule> ParetoComparator<RaptorPath<T>> paretoComparator(
    ParetoSetTime timeConfig,
    ParetoSetCost costConfig,
    RelaxFunction relaxC1,
    DominanceFunction c2Comp
  ) {
    Objects.requireNonNull(timeConfig);
    Objects.requireNonNull(costConfig);

    return switch (costConfig) {
      case NONE -> switch (timeConfig) {
        case USE_TIMETABLE -> comparatorTimetable();
        case USE_ARRIVAL_TIME -> comparatorStandardArrivalTime();
        case USE_DEPARTURE_TIME -> comparatorStandardDepartureTime();
      };
      case USE_C1 -> switch (timeConfig) {
        case USE_TIMETABLE -> comparatorTimetableAndC1();
        case USE_ARRIVAL_TIME -> comparatorArrivalTimeAndC1();
        case USE_DEPARTURE_TIME -> comparatorDepartureTimeAndC1();
      };
      case USE_C1_AND_C2 -> switch (timeConfig) {
        case USE_TIMETABLE -> comparatorTimetableAndC1AndC2(c2Comp);
        case USE_ARRIVAL_TIME -> comparatorWithC1AndC2(c2Comp);
        case USE_DEPARTURE_TIME -> comparatorDepartureTimeAndC1AndC2(c2Comp);
      };
      case USE_C1_RELAXED_IF_C2_IS_OPTIMAL -> switch (timeConfig) {
        case USE_TIMETABLE -> comparatorTimetableAndRelaxedC1IfC2IsOptimal(relaxC1, c2Comp);
        case USE_ARRIVAL_TIME -> comparatorArrivalTimeAndRelaxedC1IfC2IsOptimal(relaxC1, c2Comp);
        case USE_DEPARTURE_TIME -> comparatorDepartureTimeAndRelaxedC1IfC2IsOptimal(
          relaxC1,
          c2Comp
        );
      };
      case USE_C1_RELAX_DESTINATION -> switch (timeConfig) {
        case USE_TIMETABLE -> comparatorTimetableAndRelaxedC1(relaxC1);
        case USE_ARRIVAL_TIME -> comparatorArrivalTimeAndRelaxedC1(relaxC1);
        case USE_DEPARTURE_TIME -> comparatorDepartureTimeAndRelaxedC1(relaxC1);
      };
    };
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    RaptorPath<T>
  > comparatorStandardArrivalTime() {
    return (l, r) -> compareArrivalTime(l, r) || compareNumberOfTransfers(l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    RaptorPath<T>
  > comparatorStandardDepartureTime() {
    return (l, r) -> compareDepartureTime(l, r) || compareNumberOfTransfers(l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    RaptorPath<T>
  > comparatorTimetable() {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    RaptorPath<T>
  > comparatorTimetableAndC1() {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDurationInclusivePenalty(l, r) ||
      compareC1(l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    RaptorPath<T>
  > comparatorTimetableAndRelaxedC1(final RelaxFunction relaxCost) {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDurationInclusivePenalty(l, r) ||
      compareC1(relaxCost, l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    RaptorPath<T>
  > comparatorArrivalTimeAndC1() {
    return (l, r) ->
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDurationInclusivePenalty(l, r) ||
      compareC1(l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    RaptorPath<T>
  > comparatorDepartureTimeAndC1() {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDurationInclusivePenalty(l, r) ||
      compareC1(l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    RaptorPath<T>
  > comparatorArrivalTimeAndRelaxedC1(RelaxFunction relaxCost) {
    return (l, r) ->
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDurationInclusivePenalty(l, r) ||
      compareC1(relaxCost, l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    RaptorPath<T>
  > comparatorDepartureTimeAndRelaxedC1(RelaxFunction relaxCost) {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDurationInclusivePenalty(l, r) ||
      compareC1(relaxCost, l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    RaptorPath<T>
  > comparatorTimetableAndC1AndC2(DominanceFunction c2Comp) {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDurationInclusivePenalty(l, r) ||
      compareC1(l, r) ||
      c2Comp.leftDominateRight(l.c2(), r.c2());
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    RaptorPath<T>
  > comparatorTimetableAndRelaxedC1IfC2IsOptimal(
    RelaxFunction relaxCost,
    DominanceFunction c2Comp
  ) {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDurationInclusivePenalty(l, r) ||
      compareC1RelaxedIfC2IsOptimal(l, r, relaxCost, c2Comp);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    RaptorPath<T>
  > comparatorWithC1AndC2(DominanceFunction c2Comp) {
    return (l, r) ->
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDurationInclusivePenalty(l, r) ||
      compareC1(l, r) ||
      c2Comp.leftDominateRight(l.c2(), r.c2());
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    RaptorPath<T>
  > comparatorDepartureTimeAndC1AndC2(DominanceFunction c2Comp) {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDurationInclusivePenalty(l, r) ||
      compareC1(l, r) ||
      c2Comp.leftDominateRight(l.c2(), r.c2());
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    RaptorPath<T>
  > comparatorArrivalTimeAndRelaxedC1IfC2IsOptimal(
    RelaxFunction relaxCost,
    DominanceFunction c2Comp
  ) {
    return (l, r) ->
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDurationInclusivePenalty(l, r) ||
      compareC1RelaxedIfC2IsOptimal(l, r, relaxCost, c2Comp);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    RaptorPath<T>
  > comparatorDepartureTimeAndRelaxedC1IfC2IsOptimal(
    RelaxFunction relaxCost,
    DominanceFunction c2Comp
  ) {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareDurationInclusivePenalty(l, r) ||
      compareC1RelaxedIfC2IsOptimal(l, r, relaxCost, c2Comp);
  }

  private static <T extends RaptorTripSchedule> boolean compareC1RelaxedIfC2IsOptimal(
    RaptorPath<T> l,
    RaptorPath<T> r,
    RelaxFunction relaxCost,
    DominanceFunction c2Comp
  ) {
    return c2Comp.leftDominateRight(l.c2(), r.c2()) ? compareC1(relaxCost, l, r) : compareC1(l, r);
  }
}
