package org.opentripplanner.raptor.rangeraptor.path;

import java.util.Objects;
import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.SearchDirection;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

/**
 * Pareto set comparators for {@link DestinationArrival}. The destination set is also the source of
 * the result set, so the criteria define which journeys are returned. See the section
 * "Destination pareto-set" in {@code org/opentripplanner/raptor/package.md} for the explanation.
 * <p>
 * All comparators use the number of transfers, and optionally the cost (c1, relaxed c1). The time
 * criteria depend on the {@link ParetoSetTime}:
 * <ul>
 *   <li><b>Timetable</b> ({@link ParetoSetTime#USE_TIMETABLE}): iteration departure time and
 *       arrival time. A journey that departs later is kept, even if it is slower.
 *   <li><b>Arrival time</b> ({@link ParetoSetTime#USE_ARRIVAL_TIME}): arrival time, and
 *       duration if cost is used. A journey is only kept if it arrives earlier or is faster.
 *   <li><b>Departure time</b> ({@link ParetoSetTime#USE_DEPARTURE_TIME}): departure time, and
 *       duration if cost is used. A journey is only kept if it departs later or is faster.
 * </ul>
 * The duration is {@code |arrivalTime - departureTime|}, where the departure time is the
 * time-shifted departure of the journey, see {@link TimeShiftedDepartureTime}. Duration is not
 * included without cost, this is how the search has always worked.
 * <p>
 * The time criteria are defined by what they mean for the journey, not for the search direction.
 * For a forward search the arrival time is the journey arrival time. For a reverse search the
 * roles are swapped: {@link DestinationArrival#arrivalTime()} is the journey departure time (the
 * better is the later) and the departure time of the search is the journey arrival time (the
 * better is the earlier). The direction argument selects this case. A reverse search does not
 * time-shift, it uses the iteration departure time.
 * <p>
 * Other notes:
 * <ul>
 *   <li>The egress time-penalty is already part of {@link DestinationArrival#arrivalTime()},
 *       since Raptor decorates the egress with the penalty and it is part of the egress duration.
 *       It must not be added again. See {@code EgressWithPenalty}.
 *   <li>Unlike the per-stop pareto sets, where an arrival can be dropped when it is not needed to
 *       prune later arrivals, a destination arrival that is dropped is a dropped result.
 * </ul>
 */
public final class DestinationArrivalParetoSetComparators {

  private DestinationArrivalParetoSetComparators() {}

  /**
   * @param timeConfig Which time criteria to use, see {@link ParetoSetTime}.
   * @param direction  The search direction. The time criteria are defined for the journey, so the
   *                   direction decides which {@link DestinationArrival} field and numeric
   *                   direction each criterion maps to.
   * @param costConfig Supported configurations of c1, c2 and relaxed cost(c1).
   * @param relaxC1    Relax function for the generalized cost, used with
   *                   {@link ParetoSetCost#USE_C1_RELAXED_IF_C2_IS_OPTIMAL}.
   * @param c2Comp     Dominance function for c2, used with
   *                   {@link ParetoSetCost#USE_C1_RELAXED_IF_C2_IS_OPTIMAL}.
   */
  public static <T extends RaptorTripSchedule> ParetoComparator<
    DestinationArrival<T>
  > paretoComparator(
    ParetoSetTime timeConfig,
    SearchDirection direction,
    ParetoSetCost costConfig,
    RelaxFunction relaxC1,
    DominanceFunction c2Comp
  ) {
    Objects.requireNonNull(timeConfig);
    Objects.requireNonNull(direction);
    Objects.requireNonNull(costConfig);

    boolean reverse = direction.isInReverse();

    return switch (costConfig) {
      case NONE -> switch (timeConfig) {
        case USE_TIMETABLE -> reverse
          ? (l, r) -> arriveLate(l, r) || iterDepartEarly(l, r) || fewerTransfers(l, r)
          : (l, r) -> iterDepartLater(l, r) || arriveEarly(l, r) || fewerTransfers(l, r);
        case USE_ARRIVAL_TIME -> reverse
          ? (l, r) -> departEarly(l, r) || fewerTransfers(l, r)
          : (l, r) -> arriveEarly(l, r) || fewerTransfers(l, r);
        case USE_DEPARTURE_TIME -> reverse
          ? (l, r) -> arriveLate(l, r) || fewerTransfers(l, r)
          : (l, r) -> departLater(l, r) || fewerTransfers(l, r);
      };
      case USE_C1 -> switch (timeConfig) {
        case USE_TIMETABLE -> reverse
          ? (l, r) ->
              arriveLate(l, r) || iterDepartEarly(l, r) || fewerTransfers(l, r) || lowerC1(l, r)
          : (l, r) ->
              iterDepartLater(l, r) || arriveEarly(l, r) || fewerTransfers(l, r) || lowerC1(l, r);
        case USE_ARRIVAL_TIME -> reverse
          ? (l, r) ->
              departEarly(l, r) || fewerTransfers(l, r) || shorterDuration(l, r) || lowerC1(l, r)
          : (l, r) ->
              arriveEarly(l, r) || fewerTransfers(l, r) || shorterDuration(l, r) || lowerC1(l, r);
        case USE_DEPARTURE_TIME -> reverse
          ? (l, r) ->
              arriveLate(l, r) || fewerTransfers(l, r) || shorterDuration(l, r) || lowerC1(l, r)
          : (l, r) ->
              departLater(l, r) || fewerTransfers(l, r) || shorterDuration(l, r) || lowerC1(l, r);
      };
      case USE_C1_RELAXED_IF_C2_IS_OPTIMAL -> switch (timeConfig) {
        case USE_TIMETABLE -> reverse
          ? (l, r) ->
              arriveLate(l, r) ||
              iterDepartEarly(l, r) ||
              fewerTransfers(l, r) ||
              lowerC1RelaxedIfC2IsOptimal(l, r, relaxC1, c2Comp)
          : (l, r) ->
              iterDepartLater(l, r) ||
              arriveEarly(l, r) ||
              fewerTransfers(l, r) ||
              lowerC1RelaxedIfC2IsOptimal(l, r, relaxC1, c2Comp);
        case USE_ARRIVAL_TIME -> reverse
          ? (l, r) ->
              departEarly(l, r) ||
              fewerTransfers(l, r) ||
              shorterDuration(l, r) ||
              lowerC1RelaxedIfC2IsOptimal(l, r, relaxC1, c2Comp)
          : (l, r) ->
              arriveEarly(l, r) ||
              fewerTransfers(l, r) ||
              shorterDuration(l, r) ||
              lowerC1RelaxedIfC2IsOptimal(l, r, relaxC1, c2Comp);
        case USE_DEPARTURE_TIME -> reverse
          ? (l, r) ->
              arriveLate(l, r) ||
              fewerTransfers(l, r) ||
              shorterDuration(l, r) ||
              lowerC1RelaxedIfC2IsOptimal(l, r, relaxC1, c2Comp)
          : (l, r) ->
              departLater(l, r) ||
              fewerTransfers(l, r) ||
              shorterDuration(l, r) ||
              lowerC1RelaxedIfC2IsOptimal(l, r, relaxC1, c2Comp);
      };
    };
  }

  /* The criteria. The names describe the numeric direction of the better value. */

  /** Forward search: the earlier destination arrival is the better one. */
  private static boolean arriveEarly(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.arrivalTime() < r.arrivalTime();
  }

  /** Reverse search: the arrival time is the journey departure time, later is better. */
  private static boolean arriveLate(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.arrivalTime() > r.arrivalTime();
  }

  /** Forward search: the time-shifted journey departure, later is better. */
  private static boolean departLater(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.departureTime() > r.departureTime();
  }

  /** Reverse search: the departure time of the search is the journey arrival, earlier is better. */
  private static boolean departEarly(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.departureTime() < r.departureTime();
  }

  /** Timetable, forward search: the iteration departure time, later is better. */
  private static boolean iterDepartLater(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.iterationDepartureTime() > r.iterationDepartureTime();
  }

  /** Timetable, reverse search: the iteration departure time is the journey arrival. */
  private static boolean iterDepartEarly(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.iterationDepartureTime() < r.iterationDepartureTime();
  }

  /** The journey duration; independent of the search direction. */
  private static boolean shorterDuration(DestinationArrival<?> l, DestinationArrival<?> r) {
    return duration(l) < duration(r);
  }

  private static int duration(DestinationArrival<?> a) {
    return Math.abs(a.arrivalTime() - a.departureTime());
  }

  private static boolean fewerTransfers(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.numberOfTransfers() < r.numberOfTransfers();
  }

  private static boolean lowerC1(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.c1() < r.c1();
  }

  private static <T extends RaptorTripSchedule> boolean lowerC1RelaxedIfC2IsOptimal(
    DestinationArrival<T> l,
    DestinationArrival<T> r,
    RelaxFunction relaxCost,
    DominanceFunction c2Comp
  ) {
    return c2Comp.leftDominateRight(l.c2(), r.c2())
      ? l.c1() < relaxCost.relax(r.c1())
      : lowerC1(l, r);
  }
}
