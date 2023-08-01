package org.opentripplanner.raptor.rangeraptor.path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestRaptorPath;
import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

public class PathParetoSetComparatorsTest {

  final int BIG_VALUE = 999;
  final int SMALL_VALUE = 100;
  final int NO_VALUE = 0;

  private final DominanceFunction DOMINANCE_FUNCTION = (left, right) -> left > right;
  private final RelaxFunction RELAX_FUNCTION = GeneralizedCostRelaxFunction.of(1.5, 0);

  @Test
  public void testComparatorStandardArrivalTime() {
    var comparator = PathParetoSetComparators.paretoComparator(
      false,
      false,
      false,
      SearchDirection.FORWARD,
      RelaxFunction.NORMAL,
      null
    );

    verifyEndTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
  }

  @Test
  public void testComparatorStandardArrivalTimeAndC2() {
    var comparator = PathParetoSetComparators.paretoComparator(
      false,
      false,
      false,
      SearchDirection.FORWARD,
      RelaxFunction.NORMAL,
      DOMINANCE_FUNCTION
    );

    verifyEndTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
    verifyC2Comparator(comparator);
  }

  @Test
  public void testComparatorStandardDepartureTime() {
    var comparator = PathParetoSetComparators.paretoComparator(
      false,
      false,
      true,
      SearchDirection.FORWARD,
      RelaxFunction.NORMAL,
      null
    );

    verifyStartTimeComparator(comparator);

    verifyNumberOfTransfers(comparator);
  }

  @Test
  public void testComparatorStandardDepartureTimeAndC2() {
    var comparator = PathParetoSetComparators.paretoComparator(
      false,
      false,
      true,
      SearchDirection.FORWARD,
      RelaxFunction.NORMAL,
      DOMINANCE_FUNCTION
    );

    verifyStartTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
    verifyC2Comparator(comparator);
  }

  @Test
  public void testComparatorTimetable() {
    var comparator = PathParetoSetComparators.paretoComparator(
      false,
      true,
      false,
      SearchDirection.FORWARD,
      RelaxFunction.NORMAL,
      null
    );

    verifyRangeRaptorIterationDepartureTime(comparator);
    verifyEndTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
  }

  @Test
  public void testComparatorTimetableAndC2() {
    var comparator = PathParetoSetComparators.paretoComparator(
      false,
      true,
      false,
      SearchDirection.FORWARD,
      RelaxFunction.NORMAL,
      DOMINANCE_FUNCTION
    );

    verifyRangeRaptorIterationDepartureTime(comparator);
    verifyEndTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
    verifyC2Comparator(comparator);
  }

  @Test
  public void testComparatorTimetableAndC1() {
    var comparator = PathParetoSetComparators.paretoComparator(
      true,
      true,
      false,
      SearchDirection.FORWARD,
      RelaxFunction.NORMAL,
      null
    );

    verifyRangeRaptorIterationDepartureTime(comparator);
    verifyEndTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
    verifyDurationComparator(comparator);
    verifyC1Comparator(comparator);
  }

  @Test
  public void testComparatorTimetableAndRelaxedC1() {
    var comparator = PathParetoSetComparators.paretoComparator(
      true,
      true,
      false,
      SearchDirection.FORWARD,
      RELAX_FUNCTION,
      null
    );

    verifyRangeRaptorIterationDepartureTime(comparator);
    verifyEndTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
    verifyDurationComparator(comparator);
    verifyRelaxedC1Comparator(comparator);
  }

  @Test
  public void testComparatorWithC1() {
    var comparator = PathParetoSetComparators.paretoComparator(
      true,
      false,
      false,
      SearchDirection.FORWARD,
      RelaxFunction.NORMAL,
      null
    );

    verifyEndTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
    verifyDurationComparator(comparator);
    verifyC1Comparator(comparator);
  }

  @Test
  public void testComparatorDepartureTimeAndC1() {
    var comparator = PathParetoSetComparators.paretoComparator(
      true,
      false,
      true,
      SearchDirection.FORWARD,
      RelaxFunction.NORMAL,
      null
    );

    verifyStartTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
    verifyDurationComparator(comparator);
    verifyC1Comparator(comparator);
  }

  @Test
  public void comparatorArrivalTimeAndRelaxedC1() {
    var comparator = PathParetoSetComparators.paretoComparator(
      true,
      false,
      false,
      SearchDirection.FORWARD,
      RELAX_FUNCTION,
      null
    );

    verifyEndTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
    verifyDurationComparator(comparator);
    verifyRelaxedC1Comparator(comparator);
  }

  @Test
  public void testComparatorDepartureTimeAndRelaxedC1() {
    var comparator = PathParetoSetComparators.paretoComparator(
      true,
      false,
      true,
      SearchDirection.FORWARD,
      RELAX_FUNCTION,
      null
    );

    verifyStartTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
    verifyDurationComparator(comparator);
    verifyRelaxedC1Comparator(comparator);
  }

  @Test
  public void testComparatorTimetableAndC1AndC2() {
    var comparator = PathParetoSetComparators.paretoComparator(
      true,
      true,
      false,
      SearchDirection.FORWARD,
      RelaxFunction.NORMAL,
      DOMINANCE_FUNCTION
    );

    verifyRangeRaptorIterationDepartureTime(comparator);
    verifyEndTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
    verifyDurationComparator(comparator);
    verifyC1Comparator(comparator);
    verifyC2Comparator(comparator);
  }

  @Test
  public void testComparatorTimetableAndRelaxedC1AndC2() {
    var comparator = PathParetoSetComparators.paretoComparator(
      true,
      true,
      false,
      SearchDirection.FORWARD,
      RELAX_FUNCTION,
      DOMINANCE_FUNCTION
    );

    verifyRangeRaptorIterationDepartureTime(comparator);
    verifyEndTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
    verifyDurationComparator(comparator);
    verifyRelaxedC1Comparator(comparator);
    verifyC2Comparator(comparator);
  }

  @Test
  public void testComparatorWithC1AndC2() {
    var comparator = PathParetoSetComparators.paretoComparator(
      true,
      false,
      false,
      SearchDirection.FORWARD,
      RelaxFunction.NORMAL,
      DOMINANCE_FUNCTION
    );

    verifyEndTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
    verifyDurationComparator(comparator);
    verifyC1Comparator(comparator);
    verifyC2Comparator(comparator);
  }

  @Test
  public void testComparatorDepartureTimeAndC1AndC2() {
    var comparator = PathParetoSetComparators.paretoComparator(
      true,
      false,
      true,
      SearchDirection.FORWARD,
      RelaxFunction.NORMAL,
      DOMINANCE_FUNCTION
    );

    verifyStartTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
    verifyDurationComparator(comparator);
    verifyC1Comparator(comparator);
    verifyC2Comparator(comparator);
  }

  @Test
  public void testComparatorArrivalTimeAndRelaxedC1AndC2() {
    var comparator = PathParetoSetComparators.paretoComparator(
      true,
      false,
      false,
      SearchDirection.FORWARD,
      RELAX_FUNCTION,
      DOMINANCE_FUNCTION
    );

    verifyEndTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
    verifyDurationComparator(comparator);
    verifyRelaxedC1Comparator(comparator);
    verifyC2Comparator(comparator);
  }

  @Test
  public void testComparatorDepartureTimeAndRelaxedC1AndC2() {
    var comparator = PathParetoSetComparators.paretoComparator(
      true,
      false,
      true,
      SearchDirection.FORWARD,
      RELAX_FUNCTION,
      DOMINANCE_FUNCTION
    );

    verifyStartTimeComparator(comparator);
    verifyNumberOfTransfers(comparator);
    verifyDurationComparator(comparator);
    verifyRelaxedC1Comparator(comparator);
    verifyC2Comparator(comparator);
  }

  /**
   * Verify that higher startTime always wins
   */
  private void verifyStartTimeComparator(
    ParetoComparator<RaptorPath<RaptorTripSchedule>> comparator
  ) {
    assertTrue(
      comparator.leftDominanceExist(
        new TestRaptorPath(NO_VALUE, BIG_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE),
        new TestRaptorPath(NO_VALUE, SMALL_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(NO_VALUE, SMALL_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE),
        new TestRaptorPath(NO_VALUE, BIG_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE)
      )
    );
  }

  /**
   * Verify that bigger rangeRaptorIterationDepartureTime always wins
   */
  private void verifyRangeRaptorIterationDepartureTime(
    ParetoComparator<RaptorPath<RaptorTripSchedule>> comparator
  ) {
    // Verify that bigger rangeRaptorIterationDepartureTime always wins
    assertTrue(
      comparator.leftDominanceExist(
        new TestRaptorPath(BIG_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE),
        new TestRaptorPath(SMALL_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(SMALL_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE),
        new TestRaptorPath(BIG_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE)
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
        new TestRaptorPath(NO_VALUE, NO_VALUE, SMALL_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE),
        new TestRaptorPath(NO_VALUE, NO_VALUE, BIG_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(NO_VALUE, NO_VALUE, BIG_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE),
        new TestRaptorPath(NO_VALUE, NO_VALUE, SMALL_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE)
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
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, SMALL_VALUE, NO_VALUE, NO_VALUE),
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, BIG_VALUE, NO_VALUE, NO_VALUE)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, BIG_VALUE, NO_VALUE, NO_VALUE),
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, SMALL_VALUE, NO_VALUE, NO_VALUE)
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
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, SMALL_VALUE, NO_VALUE, NO_VALUE, NO_VALUE),
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, BIG_VALUE, NO_VALUE, NO_VALUE, NO_VALUE)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, BIG_VALUE, NO_VALUE, NO_VALUE, NO_VALUE),
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, SMALL_VALUE, NO_VALUE, NO_VALUE, NO_VALUE)
      )
    );
  }

  /**
   * Verify that lower c1 always wins
   */
  private void verifyC1Comparator(ParetoComparator<RaptorPath<RaptorTripSchedule>> comparator) {
    assertTrue(
      comparator.leftDominanceExist(
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, SMALL_VALUE, NO_VALUE),
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, BIG_VALUE, NO_VALUE)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, BIG_VALUE, NO_VALUE),
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, SMALL_VALUE, NO_VALUE)
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
        new TestRaptorPath(
          NO_VALUE,
          NO_VALUE,
          NO_VALUE,
          NO_VALUE,
          NO_VALUE,
          (int) (SMALL_VALUE * (1.4)),
          NO_VALUE
        ),
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, SMALL_VALUE, NO_VALUE)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(
          NO_VALUE,
          NO_VALUE,
          NO_VALUE,
          NO_VALUE,
          NO_VALUE,
          (int) (SMALL_VALUE * (1.6)),
          NO_VALUE
        ),
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, SMALL_VALUE, NO_VALUE)
      )
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
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, BIG_VALUE),
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, SMALL_VALUE)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, SMALL_VALUE),
        new TestRaptorPath(NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, BIG_VALUE)
      )
    );
  }
}
