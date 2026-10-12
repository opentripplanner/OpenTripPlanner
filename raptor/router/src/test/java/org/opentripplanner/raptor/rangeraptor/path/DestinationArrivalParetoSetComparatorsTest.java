package org.opentripplanner.raptor.rangeraptor.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost.NONE;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost.USE_C1;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost.USE_C1_RELAXED_IF_C2_IS_OPTIMAL;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_ARRIVAL_TIME;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_DEPARTURE_TIME;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_TIMETABLE;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_TIMETABLE_REVERSE;
import static org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalParetoSetComparators.paretoComparator;

import java.util.List;
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
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

/**
 * Verify each criterion of every time/cost combination in isolation: an arrival that is better in
 * one criterion only must dominate in that criterion if the criterion is part of the mode, and
 * must be ignored if it is not. This guards against copy-paste errors in the comparator variants.
 */
class DestinationArrivalParetoSetComparatorsTest {

  private static final int ITERATION = 1000;
  private static final int ARRIVAL = 2000;
  private static final int TRANSFERS = 2;
  private static final int C1 = 500;
  private static final int C2 = 5;

  private static final DominanceFunction C2_COMP = (left, right) -> left < right;
  private static final RelaxFunction RELAX_FN = GeneralizedCostRelaxFunction.of(1.0, 50);

  /** Direction of a criterion: which value is the better one. */
  private enum Dir {
    NOT_USED,
    LARGER_IS_BETTER,
    SMALLER_IS_BETTER,
  }

  private record Mode(ParetoSetTime time, Dir iterationDeparture, Dir arrivalTime) {}

  private static final List<Mode> MODES = List.of(
    new Mode(USE_ARRIVAL_TIME, Dir.NOT_USED, Dir.SMALLER_IS_BETTER),
    new Mode(USE_DEPARTURE_TIME, Dir.LARGER_IS_BETTER, Dir.LARGER_IS_BETTER),
    new Mode(USE_TIMETABLE, Dir.LARGER_IS_BETTER, Dir.SMALLER_IS_BETTER),
    new Mode(USE_TIMETABLE_REVERSE, Dir.SMALLER_IS_BETTER, Dir.LARGER_IS_BETTER)
  );

  static Stream<Arguments> allModes() {
    return MODES.stream().flatMap(m ->
      Stream.of(NONE, USE_C1, USE_C1_RELAXED_IF_C2_IS_OPTIMAL).map(c -> Arguments.of(m, c))
    );
  }

  @ParameterizedTest
  @MethodSource("allModes")
  void iterationDepartureTime(Mode mode, ParetoSetCost cost) {
    var subject = comparator(mode, cost);
    var base = arrival(ITERATION, ARRIVAL, TRANSFERS, C1, C2);
    assertCriterion(
      subject,
      mode.iterationDeparture(),
      base,
      arrival(ITERATION + 1, ARRIVAL, TRANSFERS, C1, C2),
      arrival(ITERATION - 1, ARRIVAL, TRANSFERS, C1, C2)
    );
  }

  @ParameterizedTest
  @MethodSource("allModes")
  void arrivalTime(Mode mode, ParetoSetCost cost) {
    var subject = comparator(mode, cost);
    var base = arrival(ITERATION, ARRIVAL, TRANSFERS, C1, C2);
    assertCriterion(
      subject,
      mode.arrivalTime(),
      base,
      arrival(ITERATION, ARRIVAL + 1, TRANSFERS, C1, C2),
      arrival(ITERATION, ARRIVAL - 1, TRANSFERS, C1, C2)
    );
  }

  @ParameterizedTest
  @MethodSource("allModes")
  void numberOfTransfers(Mode mode, ParetoSetCost cost) {
    var subject = comparator(mode, cost);
    var base = arrival(ITERATION, ARRIVAL, TRANSFERS, C1, C2);
    var fewer = arrival(ITERATION, ARRIVAL, TRANSFERS - 1, C1, C2);

    assertTrue(subject.leftDominanceExist(fewer, base));
    assertFalse(subject.leftDominanceExist(base, fewer));
  }

  @ParameterizedTest
  @MethodSource("allModes")
  void c1(Mode mode, ParetoSetCost cost) {
    var subject = comparator(mode, cost);
    var base = arrival(ITERATION, ARRIVAL, TRANSFERS, C1, C2);
    var cheaper = arrival(ITERATION, ARRIVAL, TRANSFERS, C1 - 1, C2);

    // No c2 advantage in this test, so relaxed c1 behaves as a strict c1 comparison
    boolean c1IsUsed = cost != NONE;
    assertEquals(c1IsUsed, subject.leftDominanceExist(cheaper, base));
    assertFalse(subject.leftDominanceExist(base, cheaper));
  }

  @ParameterizedTest
  @MethodSource("allModes")
  void equalArrivalsDoNotDominateEachOther(Mode mode, ParetoSetCost cost) {
    var subject = comparator(mode, cost);
    var a = arrival(ITERATION, ARRIVAL, TRANSFERS, C1, C2);
    var b = arrival(ITERATION, ARRIVAL, TRANSFERS, C1, C2);

    assertFalse(subject.leftDominanceExist(a, b));
    assertFalse(subject.leftDominanceExist(b, a));
  }

  @ParameterizedTest
  @MethodSource("allModes")
  void c2IsIgnoredWhenRelaxedC1IsNotUsed(Mode mode, ParetoSetCost cost) {
    assumeTrue(cost != USE_C1_RELAXED_IF_C2_IS_OPTIMAL);
    var subject = comparator(mode, cost);
    var base = arrival(ITERATION, ARRIVAL, TRANSFERS, C1, C2);
    var betterC2 = arrival(ITERATION, ARRIVAL, TRANSFERS, C1, C2 - 1);

    assertFalse(subject.leftDominanceExist(betterC2, base));
    assertFalse(subject.leftDominanceExist(base, betterC2));
  }

  @ParameterizedTest
  @MethodSource("timeModes")
  void relaxedC1IsUsedWhenC2IsBetter(ParetoSetTime time) {
    var subject = paretoComparator(time, USE_C1_RELAXED_IF_C2_IS_OPTIMAL, RELAX_FN, C2_COMP);
    var base = arrival(ITERATION, ARRIVAL, TRANSFERS, C1, C2);
    int relaxedLimit = RELAX_FN.relax(C1);

    // Better c2 and c1 within the relaxed limit: c1 is accepted as "better"
    var withinLimit = arrival(ITERATION, ARRIVAL, TRANSFERS, relaxedLimit - 1, C2 - 1);
    assertTrue(subject.leftDominanceExist(withinLimit, base));

    // Better c2, but c1 is at the relaxed limit: no c1 advantage
    var atLimit = arrival(ITERATION, ARRIVAL, TRANSFERS, relaxedLimit, C2 - 1);
    assertFalse(subject.leftDominanceExist(atLimit, base));

    // Same c2: strict c1 comparison, even if c1 is within the relaxed limit
    var sameC2 = arrival(ITERATION, ARRIVAL, TRANSFERS, C1 + 1, C2);
    assertFalse(subject.leftDominanceExist(sameC2, base));

    // Worse c2: strict c1 comparison
    var worseC2 = arrival(ITERATION, ARRIVAL, TRANSFERS, C1 + 1, C2 + 1);
    assertFalse(subject.leftDominanceExist(worseC2, base));
  }

  static Stream<ParetoSetTime> timeModes() {
    return MODES.stream().map(Mode::time);
  }

  @Test
  void timeAndCostConfigAreRequired() {
    assertThrows(NullPointerException.class, () ->
      paretoComparator(null, NONE, RelaxFunction.NORMAL, null)
    );
    assertThrows(NullPointerException.class, () ->
      paretoComparator(USE_ARRIVAL_TIME, null, RelaxFunction.NORMAL, null)
    );
  }

  private static void assertCriterion(
    ParetoComparator<DestinationArrival<RaptorTripSchedule>> subject,
    Dir dir,
    DestinationArrival<RaptorTripSchedule> base,
    DestinationArrival<RaptorTripSchedule> larger,
    DestinationArrival<RaptorTripSchedule> smaller
  ) {
    switch (dir) {
      case NOT_USED -> {
        assertFalse(subject.dominanceExist(base, larger));
        assertFalse(subject.dominanceExist(base, smaller));
      }
      case LARGER_IS_BETTER -> {
        assertTrue(subject.leftDominanceExist(larger, base));
        assertFalse(subject.leftDominanceExist(base, larger));
        assertTrue(subject.leftDominanceExist(base, smaller));
        assertFalse(subject.leftDominanceExist(smaller, base));
      }
      case SMALLER_IS_BETTER -> {
        assertTrue(subject.leftDominanceExist(smaller, base));
        assertFalse(subject.leftDominanceExist(base, smaller));
        assertTrue(subject.leftDominanceExist(base, larger));
        assertFalse(subject.leftDominanceExist(larger, base));
      }
    }
  }

  private static ParetoComparator<DestinationArrival<RaptorTripSchedule>> comparator(
    Mode mode,
    ParetoSetCost cost
  ) {
    boolean relaxed = cost == USE_C1_RELAXED_IF_C2_IS_OPTIMAL;
    return paretoComparator(
      mode.time(),
      cost,
      relaxed ? RELAX_FN : RelaxFunction.NORMAL,
      relaxed ? C2_COMP : null
    );
  }

  private static DestinationArrival<RaptorTripSchedule> arrival(
    int iterationDepartureTime,
    int arrivalTime,
    int numberOfTransfers,
    int c1,
    int c2
  ) {
    var probe = DestinationArrival.<RaptorTripSchedule>createProbe();
    probe.setProbeValues(iterationDepartureTime, arrivalTime, numberOfTransfers, c1, c2);
    return probe;
  }
}
