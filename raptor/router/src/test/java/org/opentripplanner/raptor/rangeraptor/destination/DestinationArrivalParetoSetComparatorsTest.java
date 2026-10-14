package org.opentripplanner.raptor.rangeraptor.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost.NONE;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost.USE_C1;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost.USE_C1_RELAXED_IF_C2_IS_OPTIMAL;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_ARRIVAL_TIME;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_DEPARTURE_TIME;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_TIMETABLE;
import static org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalParetoSetComparators.paretoComparator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.SearchDirection;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

/**
 * Verify the comparators using an independent oracle. For every time/cost combination, the
 * criteria used are listed in a table. The production comparator must give the same answer as the
 * oracle (left is better than right in at least one criterion) for every pair in a grid of values.
 * <p>
 * See the section "Destination pareto-set" in {@code org/opentripplanner/raptor/package.md}.
 */
class DestinationArrivalParetoSetComparatorsTest {

  private static final DominanceFunction C2_COMP = (left, right) -> left < right;
  private static final RelaxFunction RELAX_FN = GeneralizedCostRelaxFunction.of(1.0, 3);

  /** A criterion, true if the left arrival is better than the right arrival. */
  private enum Criterion {
    ITERATION_LATER((l, r) -> l.iterationDepartureTime() > r.iterationDepartureTime()),
    ITERATION_EARLIER((l, r) -> l.iterationDepartureTime() < r.iterationDepartureTime()),
    DEPARTURE_LATER((l, r) -> l.departureTime() > r.departureTime()),
    DEPARTURE_EARLIER((l, r) -> l.departureTime() < r.departureTime()),
    ARRIVAL_EARLIER((l, r) -> l.arrivalTime() < r.arrivalTime()),
    ARRIVAL_LATER((l, r) -> l.arrivalTime() > r.arrivalTime()),
    SHORTER_DURATION((l, r) -> duration(l) < duration(r)),
    FEWER_TRANSFERS((l, r) -> l.numberOfTransfers() < r.numberOfTransfers());

    private final BiPredicate<DestinationArrival<?>, DestinationArrival<?>> better;

    Criterion(BiPredicate<DestinationArrival<?>, DestinationArrival<?>> better) {
      this.better = better;
    }
  }

  private static int duration(DestinationArrival<?> a) {
    return Math.abs(a.arrivalTime() - a.departureTime());
  }

  /**
   * The time criteria for each mode, without cost and with cost. A timetable search compares the
   * iteration departure time and the arrival time, while the others compare the arrival (or
   * departure) time, and the duration if cost is used.
   */
  private record Mode(
    ParetoSetTime time,
    SearchDirection direction,
    List<Criterion> noCost,
    List<Criterion> withCost
  ) {}

  private static final List<Mode> MODES = List.of(
    new Mode(
      USE_TIMETABLE,
      SearchDirection.FORWARD,
      List.of(Criterion.ITERATION_LATER, Criterion.ARRIVAL_EARLIER),
      List.of(Criterion.ITERATION_LATER, Criterion.ARRIVAL_EARLIER)
    ),
    new Mode(
      USE_TIMETABLE,
      SearchDirection.REVERSE,
      List.of(Criterion.ARRIVAL_LATER, Criterion.ITERATION_EARLIER),
      List.of(Criterion.ARRIVAL_LATER, Criterion.ITERATION_EARLIER)
    ),
    new Mode(
      USE_ARRIVAL_TIME,
      SearchDirection.FORWARD,
      List.of(Criterion.ARRIVAL_EARLIER),
      List.of(Criterion.ARRIVAL_EARLIER, Criterion.SHORTER_DURATION)
    ),
    new Mode(
      USE_ARRIVAL_TIME,
      SearchDirection.REVERSE,
      List.of(Criterion.DEPARTURE_EARLIER),
      List.of(Criterion.DEPARTURE_EARLIER, Criterion.SHORTER_DURATION)
    ),
    new Mode(
      USE_DEPARTURE_TIME,
      SearchDirection.FORWARD,
      List.of(Criterion.DEPARTURE_LATER),
      List.of(Criterion.DEPARTURE_LATER, Criterion.SHORTER_DURATION)
    ),
    new Mode(
      USE_DEPARTURE_TIME,
      SearchDirection.REVERSE,
      List.of(Criterion.ARRIVAL_LATER),
      List.of(Criterion.ARRIVAL_LATER, Criterion.SHORTER_DURATION)
    )
  );

  static Stream<Arguments> allModes() {
    return MODES.stream().flatMap(m ->
      Stream.of(NONE, USE_C1, USE_C1_RELAXED_IF_C2_IS_OPTIMAL).map(c -> Arguments.of(m, c))
    );
  }

  @ParameterizedTest
  @MethodSource("allModes")
  void comparatorMatchesTheCriteriaOfTheMode(Mode mode, ParetoSetCost cost) {
    var subject = comparator(mode.time(), mode.direction(), cost);
    var criteria = cost == NONE ? mode.noCost() : mode.withCost();
    var arrivals = grid(mode.direction().isInReverse());

    for (var l : arrivals) {
      for (var r : arrivals) {
        boolean expected =
          criteria.stream().anyMatch(c -> c.better.test(l, r)) ||
          fewerTransfers(l, r) ||
          costIsBetter(cost, l, r);
        assertEquals(expected, subject.leftDominanceExist(l, r), "Left: " + l + ", Right: " + r);
      }
    }
  }

  @Test
  void durationIsNotACriterionForTimetableSearches() {
    var subject = comparator(USE_TIMETABLE, SearchDirection.FORWARD, USE_C1);
    // Same iteration and arrival, but a shorter duration: the departure time is not used
    var base = arrival(10, 1, 20, 0, 5, 0);
    var shorter = arrival(10, 9, 20, 0, 5, 0);

    assertEquals(false, subject.dominanceExist(base, shorter));
  }

  /**
   * A journey that leaves later and arrives later, with the same duration, is only interesting
   * for a timetable search. The duration is the same, so the journey is dominated if the
   * duration is used.
   */
  @Test
  void laterDepartureWithSameDurationIsOnlyKeptByTimetable() {
    var first = arrival(10, 10, 20, 0, 5, 0);
    var shifted = arrival(11, 11, 21, 0, 5, 0);

    var timetable = comparator(USE_TIMETABLE, SearchDirection.FORWARD, USE_C1);
    assertEquals(true, timetable.leftDominanceExist(shifted, first));
    assertEquals(true, timetable.leftDominanceExist(first, shifted));

    var arrivalTimeSearch = comparator(USE_ARRIVAL_TIME, SearchDirection.FORWARD, USE_C1);
    assertEquals(true, arrivalTimeSearch.leftDominanceExist(first, shifted));
    assertEquals(false, arrivalTimeSearch.leftDominanceExist(shifted, first));
  }

  @Test
  void durationIsNotUsedWithoutCost() {
    var subject = comparator(USE_ARRIVAL_TIME, SearchDirection.FORWARD, NONE);
    var longer = arrival(10, 1, 20, 0, 5, 0);
    var shorter = arrival(10, 9, 20, 0, 5, 0);

    assertEquals(false, subject.dominanceExist(longer, shorter));
  }

  @Test
  void timeAndCostConfigAreRequired() {
    assertThrows(NullPointerException.class, () ->
      paretoComparator(null, SearchDirection.FORWARD, NONE, RelaxFunction.NORMAL, null)
    );
    assertThrows(NullPointerException.class, () ->
      paretoComparator(USE_ARRIVAL_TIME, SearchDirection.FORWARD, null, RelaxFunction.NORMAL, null)
    );
  }

  /* Test helpers */

  private static boolean fewerTransfers(DestinationArrival<?> l, DestinationArrival<?> r) {
    return Criterion.FEWER_TRANSFERS.better.test(l, r);
  }

  private static boolean costIsBetter(
    ParetoSetCost cost,
    DestinationArrival<?> l,
    DestinationArrival<?> r
  ) {
    return switch (cost) {
      case NONE -> false;
      case USE_C1 -> l.c1() < r.c1();
      case USE_C1_RELAXED_IF_C2_IS_OPTIMAL -> C2_COMP.leftDominateRight(l.c2(), r.c2())
        ? l.c1() < RELAX_FN.relax(r.c1())
        : l.c1() < r.c1();
    };
  }

  private static ParetoComparator<DestinationArrival<RaptorTripSchedule>> comparator(
    ParetoSetTime time,
    SearchDirection direction,
    ParetoSetCost cost
  ) {
    boolean relaxed = cost == USE_C1_RELAXED_IF_C2_IS_OPTIMAL;
    return paretoComparator(
      time,
      direction,
      cost,
      relaxed ? RELAX_FN : RelaxFunction.NORMAL,
      relaxed ? C2_COMP : null
    );
  }

  /**
   * A forward journey departs before it arrives. In a reverse search the times are reversed.
   * Each criterion has a small domain so the grid is complete, but not too big.
   */
  private static List<DestinationArrival<RaptorTripSchedule>> grid(boolean reverse) {
    var list = new ArrayList<DestinationArrival<RaptorTripSchedule>>();
    for (int iteration : new int[] { 100, 101 }) {
      for (int departure : new int[] { 110, 112 }) {
        for (int arrivalTime : new int[] { 120, 123 }) {
          for (int transfers : new int[] { 0, 1 }) {
            for (int c1 : new int[] { 10, 12, 20 }) {
              for (int c2 : new int[] { 0, 1 }) {
                list.add(
                  reverse
                    ? arrival(iteration, 200 - departure, 200 - arrivalTime, transfers, c1, c2)
                    : arrival(iteration, departure, arrivalTime, transfers, c1, c2)
                );
              }
            }
          }
        }
      }
    }
    return list;
  }

  private static DestinationArrival<RaptorTripSchedule> arrival(
    int iterationDepartureTime,
    int departureTime,
    int arrivalTime,
    int numberOfTransfers,
    int c1,
    int c2
  ) {
    var probe = DestinationArrival.<RaptorTripSchedule>createProbe();
    probe.setProbeValues(
      iterationDepartureTime,
      departureTime,
      arrivalTime,
      numberOfTransfers,
      c1,
      c2
    );
    return probe;
  }
}
