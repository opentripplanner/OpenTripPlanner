package org.opentripplanner.raptor.rangeraptor.path;

import java.util.Objects;
import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

/**
 * Pareto set comparators for {@link DestinationArrival}.
 * <p>
 * Design mirrors the per-stop arrival pareto sets: the comparators use
 * {@link DestinationArrival#arrivalTime()},
 * {@link DestinationArrival#numberOfTransfers()}, and cost.
 * <p>
 * The egress time-penalty is already part of {@link DestinationArrival#arrivalTime()}: Raptor
 * decorates egress paths with the penalty, so it is included in the egress duration. It must not
 * be added again here. See {@code EgressWithPenalty}.
 * <p>
 * In timetable mode ({@link ParetoSetTime#USE_TIMETABLE}), {@code iterationDepartureTime} is
 * included as the primary criterion so that results from all departure iterations are kept, even
 * when arrival time and cost are equal. This is the normal mode of operation.
 * <p>
 * Without timetable mode, cross-iteration correctness follows naturally: Range Raptor explores
 * iterations latest → earliest, so the first arrival added for a given (arrivalTime, transfers,
 * c1) tuple is always from the latest-departing iteration. Equal-criteria arrivals from subsequent
 * (earlier-departing) iterations are rejected via the {@code NONE} case in the pareto set, exactly
 * as intermediate stop arrivals are handled.
 * <p>
 * In {@link ParetoSetTime#USE_DEPARTURE_TIME} mode (arrive-by / reverse searches),
 * {@code arrivalTime} holds the forward origin-departure time (time flows backward
 * in the reverse calculator), so the comparison is inverted: larger value = later departure =
 * better.
 */
public final class DestinationArrivalParetoSetComparators {

  private DestinationArrivalParetoSetComparators() {}

  public static <T extends RaptorTripSchedule> ParetoComparator<
    DestinationArrival<T>
  > paretoComparator(
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
        case USE_TIMETABLE_REVERSE -> comparatorTimetableReverse();
        case USE_ARRIVAL_TIME -> comparatorArrivalTime();
        case USE_DEPARTURE_TIME -> comparatorDepartureTime();
      };
      case USE_C1 -> switch (timeConfig) {
        case USE_TIMETABLE -> comparatorTimetableAndC1();
        case USE_TIMETABLE_REVERSE -> comparatorTimetableReverseAndC1();
        case USE_ARRIVAL_TIME -> comparatorArrivalTimeAndC1();
        case USE_DEPARTURE_TIME -> comparatorDepartureTimeAndC1();
      };
      case USE_C1_RELAXED_IF_C2_IS_OPTIMAL -> switch (timeConfig) {
        case USE_TIMETABLE -> comparatorTimetableAndRelaxedC1IfC2IsOptimal(relaxC1, c2Comp);
        case USE_TIMETABLE_REVERSE -> comparatorTimetableReverseAndRelaxedC1IfC2IsOptimal(
          relaxC1,
          c2Comp
        );
        case USE_ARRIVAL_TIME -> comparatorArrivalTimeAndRelaxedC1IfC2IsOptimal(relaxC1, c2Comp);
        case USE_DEPARTURE_TIME -> comparatorDepartureTimeAndRelaxedC1IfC2IsOptimal(
          relaxC1,
          c2Comp
        );
      };
    };
  }

  /**
   * Timetable comparator for reverse (arrive-by) searches.
   * <p>
   * In a reverse search {@code arrivalTime} holds the forward origin-departure
   * time (prefer larger = later departure) and {@code iterationDepartureTime} holds the forward
   * destination-arrival time (prefer smaller = earlier arrival). Both roles and directions are
   * therefore inverted relative to {@link #comparatorTimetable()}.
   */
  private static <T extends RaptorTripSchedule> ParetoComparator<
    DestinationArrival<T>
  > comparatorTimetableReverse() {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareIterationDepartureTimeReverse(l, r) ||
      compareNumberOfTransfers(l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    DestinationArrival<T>
  > comparatorTimetableReverseAndC1() {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareIterationDepartureTimeReverse(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareC1(l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    DestinationArrival<T>
  > comparatorTimetableReverseAndRelaxedC1IfC2IsOptimal(
    RelaxFunction relaxCost,
    DominanceFunction c2Comp
  ) {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareIterationDepartureTimeReverse(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareC1RelaxedIfC2IsOptimal(l, r, relaxCost, c2Comp);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    DestinationArrival<T>
  > comparatorArrivalTime() {
    return (l, r) -> compareArrivalTime(l, r) || compareNumberOfTransfers(l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    DestinationArrival<T>
  > comparatorDepartureTime() {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareIterationDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    DestinationArrival<T>
  > comparatorTimetable() {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    DestinationArrival<T>
  > comparatorArrivalTimeAndC1() {
    return (l, r) -> compareArrivalTime(l, r) || compareNumberOfTransfers(l, r) || compareC1(l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    DestinationArrival<T>
  > comparatorDepartureTimeAndC1() {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareIterationDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareC1(l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    DestinationArrival<T>
  > comparatorTimetableAndC1() {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareC1(l, r);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    DestinationArrival<T>
  > comparatorArrivalTimeAndRelaxedC1IfC2IsOptimal(
    RelaxFunction relaxCost,
    DominanceFunction c2Comp
  ) {
    return (l, r) ->
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareC1RelaxedIfC2IsOptimal(l, r, relaxCost, c2Comp);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    DestinationArrival<T>
  > comparatorDepartureTimeAndRelaxedC1IfC2IsOptimal(
    RelaxFunction relaxCost,
    DominanceFunction c2Comp
  ) {
    return (l, r) ->
      compareDepartureTime(l, r) ||
      compareIterationDepartureTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareC1RelaxedIfC2IsOptimal(l, r, relaxCost, c2Comp);
  }

  private static <T extends RaptorTripSchedule> ParetoComparator<
    DestinationArrival<T>
  > comparatorTimetableAndRelaxedC1IfC2IsOptimal(
    RelaxFunction relaxCost,
    DominanceFunction c2Comp
  ) {
    return (l, r) ->
      compareIterationDepartureTime(l, r) ||
      compareArrivalTime(l, r) ||
      compareNumberOfTransfers(l, r) ||
      compareC1RelaxedIfC2IsOptimal(l, r, relaxCost, c2Comp);
  }

  private static boolean compareArrivalTime(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.arrivalTime() < r.arrivalTime();
  }

  /**
   * In a reverse (arrive-by) search, {@code arrivalTime} holds the forward
   * origin-departure time. Larger = later departure = better for an arrive-by search.
   */
  private static boolean compareDepartureTime(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.arrivalTime() > r.arrivalTime();
  }

  private static boolean compareIterationDepartureTime(
    DestinationArrival<?> l,
    DestinationArrival<?> r
  ) {
    return l.iterationDepartureTime() > r.iterationDepartureTime();
  }

  /** In a reverse search, smaller iterationDepartureTime = earlier forward destination arrival = better. */
  private static boolean compareIterationDepartureTimeReverse(
    DestinationArrival<?> l,
    DestinationArrival<?> r
  ) {
    return l.iterationDepartureTime() < r.iterationDepartureTime();
  }

  private static boolean compareNumberOfTransfers(
    DestinationArrival<?> l,
    DestinationArrival<?> r
  ) {
    return l.numberOfTransfers() < r.numberOfTransfers();
  }

  private static boolean compareC1(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.c1() < r.c1();
  }

  private static <T extends RaptorTripSchedule> boolean compareC1RelaxedIfC2IsOptimal(
    DestinationArrival<T> l,
    DestinationArrival<T> r,
    RelaxFunction relaxCost,
    DominanceFunction c2Comp
  ) {
    return c2Comp.leftDominateRight(l.c2(), r.c2())
      ? l.c1() < relaxCost.relax(r.c1())
      : compareC1(l, r);
  }
}
