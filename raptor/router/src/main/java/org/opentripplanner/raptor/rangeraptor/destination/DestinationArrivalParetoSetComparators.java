package org.opentripplanner.raptor.rangeraptor.destination;

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
 * included without cost.
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
 *   <li>The destination set is the result of the search. The per-stop pareto sets are only a
 *       search aid, so they may drop an arrival that a later search step does not need. An
 *       arrival dropped from the destination set is lost from the result.
 *   <li>The per-stop sets have no departure time criterion; they rely on the iterations running
 *       from the latest to the earliest departure. The destination set holds the result of all
 *       iterations, so the departure time (or the duration) must be a criterion here.
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
    if (costConfig == ParetoSetCost.USE_C1_RELAXED_IF_C2_IS_OPTIMAL) {
      Objects.requireNonNull(relaxC1);
      Objects.requireNonNull(c2Comp);
    }

    boolean reverse = direction.isInReverse();

    return switch (costConfig) {
      case NONE -> switch (timeConfig) {
        case USE_TIMETABLE -> reverse
          ? (l, r) -> arriveLate(l, r) || iterDepartEarly(l, r) || fewTransfers(l, r)
          : (l, r) -> iterDepartLate(l, r) || arriveEarly(l, r) || fewTransfers(l, r);
        case USE_ARRIVAL_TIME -> reverse
          ? (l, r) -> departEarly(l, r) || fewTransfers(l, r)
          : (l, r) -> arriveEarly(l, r) || fewTransfers(l, r);
        case USE_DEPARTURE_TIME -> reverse
          ? (l, r) -> arriveLate(l, r) || fewTransfers(l, r)
          : (l, r) -> departLate(l, r) || fewTransfers(l, r);
      };
      case USE_C1 -> switch (timeConfig) {
        case USE_TIMETABLE -> reverse
          ? (l, r) -> arriveLate(l, r) || iterDepartEarly(l, r) || fewTransfers(l, r) || lowC1(l, r)
          : (l, r) ->
              iterDepartLate(l, r) || arriveEarly(l, r) || fewTransfers(l, r) || lowC1(l, r);
        case USE_ARRIVAL_TIME -> reverse
          ? (l, r) -> departEarly(l, r) || fewTransfers(l, r) || shortDuration(l, r) || lowC1(l, r)
          : (l, r) -> arriveEarly(l, r) || fewTransfers(l, r) || shortDuration(l, r) || lowC1(l, r);
        case USE_DEPARTURE_TIME -> reverse
          ? (l, r) -> arriveLate(l, r) || fewTransfers(l, r) || shortDuration(l, r) || lowC1(l, r)
          : (l, r) -> departLate(l, r) || fewTransfers(l, r) || shortDuration(l, r) || lowC1(l, r);
      };
      case USE_C1_RELAXED_IF_C2_IS_OPTIMAL -> switch (timeConfig) {
        case USE_TIMETABLE -> reverse
          ? (l, r) ->
              arriveLate(l, r) ||
              iterDepartEarly(l, r) ||
              fewTransfers(l, r) ||
              lowC1RelaxedIfC2IsOptimal(l, r, relaxC1, c2Comp)
          : (l, r) ->
              iterDepartLate(l, r) ||
              arriveEarly(l, r) ||
              fewTransfers(l, r) ||
              lowC1RelaxedIfC2IsOptimal(l, r, relaxC1, c2Comp);
        case USE_ARRIVAL_TIME -> reverse
          ? (l, r) ->
              departEarly(l, r) ||
              fewTransfers(l, r) ||
              shortDuration(l, r) ||
              lowC1RelaxedIfC2IsOptimal(l, r, relaxC1, c2Comp)
          : (l, r) ->
              arriveEarly(l, r) ||
              fewTransfers(l, r) ||
              shortDuration(l, r) ||
              lowC1RelaxedIfC2IsOptimal(l, r, relaxC1, c2Comp);
        case USE_DEPARTURE_TIME -> reverse
          ? (l, r) ->
              arriveLate(l, r) ||
              fewTransfers(l, r) ||
              shortDuration(l, r) ||
              lowC1RelaxedIfC2IsOptimal(l, r, relaxC1, c2Comp)
          : (l, r) ->
              departLate(l, r) ||
              fewTransfers(l, r) ||
              shortDuration(l, r) ||
              lowC1RelaxedIfC2IsOptimal(l, r, relaxC1, c2Comp);
      };
    };
  }

  private static int calculateDuration(DestinationArrival<?> a) {
    return Math.abs(a.arrivalTime() - a.departureTime());
  }

  /*
   * Criteria comparison methods/macros. The names describe the numeric direction of the better
   * value. The names should be short to make it easy to read the comparators above. The names
   * express the goal, not what the method does; hence do not start with a verb.
   */

  /** Forward search: the earlier destination arrival is the better one. */
  private static boolean arriveEarly(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.arrivalTime() < r.arrivalTime();
  }

  /** Reverse search: the arrival time is the journey departure time, later is better. */
  private static boolean arriveLate(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.arrivalTime() > r.arrivalTime();
  }

  /** Forward search: the time-shifted journey departure, later is better. */
  private static boolean departLate(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.departureTime() > r.departureTime();
  }

  /** Reverse search: the departure time of the search is the journey arrival, earlier is better. */
  private static boolean departEarly(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.departureTime() < r.departureTime();
  }

  /** Timetable, forward search: the iteration departure time, later is better. */
  private static boolean iterDepartLate(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.iterationDepartureTime() > r.iterationDepartureTime();
  }

  /** Timetable, reverse search: the iteration departure time is the journey arrival. */
  private static boolean iterDepartEarly(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.iterationDepartureTime() < r.iterationDepartureTime();
  }

  /** The journey duration; independent of the search direction. */
  private static boolean shortDuration(DestinationArrival<?> l, DestinationArrival<?> r) {
    return calculateDuration(l) < calculateDuration(r);
  }

  private static boolean fewTransfers(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.numberOfTransfers() < r.numberOfTransfers();
  }

  private static boolean lowC1(DestinationArrival<?> l, DestinationArrival<?> r) {
    return l.c1() < r.c1();
  }

  private static <T extends RaptorTripSchedule> boolean lowC1RelaxedIfC2IsOptimal(
    DestinationArrival<T> l,
    DestinationArrival<T> r,
    RelaxFunction relaxCost,
    DominanceFunction c2Comp
  ) {
    return c2Comp.leftDominateRight(l.c2(), r.c2())
      ? l.c1() < relaxCost.relax(r.c1())
      : lowC1(l, r);
  }
}
