package org.opentripplanner.raptor.rangeraptor.path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost.NONE;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost.USE_C1;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost.USE_C1_AND_C2;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost.USE_C1_RELAXED_IF_C2_IS_OPTIMAL;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost.USE_C1_RELAX_DESTINATION;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_ARRIVAL_TIME;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_DEPARTURE_TIME;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_TIMETABLE;
import static org.opentripplanner.raptor.rangeraptor.path.PathParetoSetComparators.paretoComparator;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor._data.api.TestRaptorPath;
import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

public class PathParetoSetComparatorsTest {

  private static final int ANY = 0;
  private static final int NORMAL = 100;
  private static final int SMALL = 50;
  private static final int LARGE = 999;
  private static final int RELAXED = 140;
  private static final int RELAXED_DELTA_LIMIT = 50;

  private static final DominanceFunction DOMINANCE_FN = (left, right) -> left < right;
  private static final DominanceFunction NO_COMP = null;

  private static final RelaxFunction RELAX_FN = GeneralizedCostRelaxFunction.of(
    1.0,
    RELAXED_DELTA_LIMIT
  );
  private static final RelaxFunction NO_RELAX_FN = RelaxFunction.NORMAL;

  static List<Arguments> testCases() {
    return List.of(
      // Arrival time
      Arguments.of(USE_ARRIVAL_TIME, NONE, NO_RELAX_FN, NO_COMP),
      Arguments.of(USE_ARRIVAL_TIME, USE_C1, NO_RELAX_FN, NO_COMP),
      Arguments.of(USE_ARRIVAL_TIME, USE_C1_AND_C2, NO_RELAX_FN, DOMINANCE_FN),
      // TODO: This is failing, error in implementation
      Arguments.of(USE_ARRIVAL_TIME, USE_C1_RELAXED_IF_C2_IS_OPTIMAL, RELAX_FN, DOMINANCE_FN),
      Arguments.of(USE_ARRIVAL_TIME, USE_C1_RELAX_DESTINATION, RELAX_FN, NO_COMP),
      // Departure time
      Arguments.of(USE_DEPARTURE_TIME, NONE, NO_RELAX_FN, NO_COMP),
      Arguments.of(USE_DEPARTURE_TIME, USE_C1_AND_C2, NO_RELAX_FN, DOMINANCE_FN),
      // TODO: This is failing, error in implementation
      Arguments.of(USE_DEPARTURE_TIME, USE_C1_RELAXED_IF_C2_IS_OPTIMAL, RELAX_FN, DOMINANCE_FN),
      Arguments.of(USE_DEPARTURE_TIME, USE_C1_RELAX_DESTINATION, RELAX_FN, NO_COMP),
      // Timetable
      Arguments.of(USE_TIMETABLE, NONE, NO_RELAX_FN, NO_COMP),
      Arguments.of(USE_TIMETABLE, USE_C1_AND_C2, NO_RELAX_FN, DOMINANCE_FN),
      // TODO: This is failing, error in implementation
      Arguments.of(USE_TIMETABLE, USE_C1_RELAXED_IF_C2_IS_OPTIMAL, RELAX_FN, DOMINANCE_FN),
      Arguments.of(USE_TIMETABLE, USE_C1_RELAX_DESTINATION, RELAX_FN, NO_COMP)
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  public void testComparator(
    ParetoSetTime time,
    ParetoSetCost cost,
    RelaxFunction relaxC1,
    DominanceFunction comp2
  ) {
    var comparator = paretoComparator(time, cost, relaxC1, comp2);
    verifyNumberOfTransfers(comparator);
    switch (time) {
      case USE_ARRIVAL_TIME:
        verifyEndTimeComparator(comparator);
        break;
      case USE_DEPARTURE_TIME:
        verifyStartTimeComparator(comparator);
        break;
      case USE_TIMETABLE:
        verifyIterationDepartureTime(comparator);
        verifyEndTimeComparator(comparator);
        break;
    }

    switch (cost) {
      case USE_C1:
        verifyC1Comparator(comparator);
        break;
      case USE_C1_AND_C2:
        verifyC1Comparator(comparator);
        verifyC2Comparator(comparator);
        break;
      case USE_C1_RELAXED_IF_C2_IS_OPTIMAL:
        verifyRelaxedC1IfC2Optimal(comparator);
        break;
      case USE_C1_RELAX_DESTINATION:
        verifyRelaxedC1Comparator(comparator);
        break;
      case NONE:
      default:
        break;
    }
  }

  /**
   * Verify that higher startTime always wins
   */
  private void verifyStartTimeComparator(
    ParetoComparator<RaptorPath<RaptorTripSchedule>> comparator
  ) {
    assertTrue(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, NORMAL, ANY, ANY, ANY, LARGE, ANY),
        new TestRaptorPath(ANY, SMALL, ANY, ANY, ANY, ANY, ANY)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, NORMAL, ANY, ANY, ANY, LARGE, ANY),
        new TestRaptorPath(ANY, LARGE, ANY, ANY, ANY, ANY, ANY)
      )
    );
  }

  /**
   * Verify that bigger rangeRaptorIterationDepartureTime always wins
   */
  private void verifyIterationDepartureTime(
    ParetoComparator<RaptorPath<RaptorTripSchedule>> comparator
  ) {
    // Verify that bigger rangeRaptorIterationDepartureTime always wins
    assertTrue(
      comparator.leftDominanceExist(
        new TestRaptorPath(NORMAL, ANY, ANY, ANY, ANY, LARGE, ANY),
        new TestRaptorPath(SMALL, ANY, ANY, ANY, ANY, ANY, ANY)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(NORMAL, ANY, ANY, ANY, ANY, LARGE, ANY),
        new TestRaptorPath(LARGE, ANY, ANY, ANY, ANY, ANY, ANY)
      )
    );
  }

  /**
   * Verify that lower endTime always wins
   */
  private void verifyEndTimeComparator(
    ParetoComparator<RaptorPath<RaptorTripSchedule>> comparator
  ) {
    //  Verify that lower endTime always wins
    assertTrue(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, NORMAL, ANY, ANY, LARGE, ANY),
        new TestRaptorPath(ANY, ANY, LARGE, ANY, ANY, SMALL, ANY)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, NORMAL, ANY, ANY, LARGE, ANY),
        new TestRaptorPath(ANY, ANY, SMALL, ANY, ANY, SMALL, ANY)
      )
    );
  }

  /**
   * Verify that lower numberOfTransfers always wins
   */
  private void verifyNumberOfTransfers(
    ParetoComparator<RaptorPath<RaptorTripSchedule>> comparator
  ) {
    assertTrue(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, ANY, ANY, NORMAL, LARGE, ANY),
        new TestRaptorPath(ANY, ANY, ANY, ANY, LARGE, SMALL, ANY)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, ANY, ANY, NORMAL, LARGE, ANY),
        new TestRaptorPath(ANY, ANY, ANY, ANY, SMALL, SMALL, ANY)
      )
    );
  }

  /**
   * Verify that lower duration always wins
   */
  private void verifyDurationComparator(
    ParetoComparator<RaptorPath<RaptorTripSchedule>> comparator
  ) {
    assertTrue(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, ANY, NORMAL, ANY, ANY, ANY),
        new TestRaptorPath(ANY, ANY, ANY, LARGE, ANY, ANY, ANY)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, ANY, NORMAL, ANY, ANY, ANY),
        new TestRaptorPath(ANY, ANY, ANY, SMALL, ANY, ANY, ANY)
      )
    );
  }

  /**
   * Verify that lower c1 always wins
   */
  private void verifyC1Comparator(ParetoComparator<RaptorPath<RaptorTripSchedule>> comparator) {
    assertTrue(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, NORMAL, ANY),
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, LARGE, ANY)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, NORMAL, ANY),
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, SMALL, ANY)
      )
    );
  }

  /**
   * Verify that relax function is used in a comparator. This method operates on assumption that ratio is 1.5 and slack is 0
   */
  private void verifyRelaxedC1Comparator(
    ParetoComparator<RaptorPath<RaptorTripSchedule>> comparator
  ) {
    assertTrue(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, RELAXED, ANY),
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, NORMAL, ANY)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, LARGE, ANY),
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, NORMAL, ANY)
      )
    );
  }

  /**
   * Verify that relax function is used in the c1 comparator, if and only if c2 is optimal.
   */
  private void verifyRelaxedC1IfC2Optimal(
    ParetoComparator<RaptorPath<RaptorTripSchedule>> comparator
  ) {
    assertTrue(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, SMALL, NORMAL),
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, NORMAL, NORMAL)
      ),
      "c1 is optimal, c2 is not => path is optimal"
    );
    assertTrue(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, RELAXED, SMALL),
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, NORMAL, NORMAL)
      ),
      "c2 is optimal, c1 is within relaxed limit => path is optimal"
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, RELAXED, ANY),
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, NORMAL, ANY)
      ),
      "c2 is not optimal, c1 is within relaxed limit => path is NOT optimal"
    );
    assertFalse(
      // c2 is optimal, but c1 is not within relaxed limit
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, LARGE, SMALL),
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, NORMAL, NORMAL)
      ),
      "c2 is optimal, c1 is not within relaxed limit => path is NOT optimal"
    );
    assertFalse(
      // c1 and c2 is not optimal (they are equal)
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, NORMAL, NORMAL),
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, NORMAL, NORMAL)
      ),
      "c1 and c2 is not optimal"
    );
  }

  /**
   * Verify that dominance function is used in a comparator. This method operates on an assumption
   * that dominance function is l.c2 > r.c2
   */
  private void verifyC2Comparator(ParetoComparator<RaptorPath<RaptorTripSchedule>> comparator) {
    // Verify that dominance function is used
    assertTrue(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, ANY, SMALL),
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, ANY, NORMAL)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, ANY, LARGE),
        new TestRaptorPath(ANY, ANY, ANY, ANY, ANY, ANY, NORMAL)
      )
    );
  }
}
