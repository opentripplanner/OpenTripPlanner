package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorCostConverter;
import org.opentripplanner.routing.cost.CostLimit;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;

class DefaultAccessEgressTest {

  private static final int STOP = 5;
  private static final State FINAL_STATE = TestStateBuilder.ofWalking().streetEdge().build();
  public static final Duration TIME_PENALTY = Duration.ofSeconds(1);
  public static final Cost COST_PENALTY = Cost.costOfSeconds(11);
  public static final TimeAndCost PENALTY = new TimeAndCost(TIME_PENALTY, COST_PENALTY);

  private final DefaultAccessEgress subject = new DefaultAccessEgress(STOP, FINAL_STATE);
  private final RoutingAccessEgress subjectWithPenalty = subject.withPenalty(PENALTY);

  @Test
  void canNotAddPenaltyTwice() {
    assertThrows(IllegalStateException.class, () -> subjectWithPenalty.withPenalty(PENALTY));
  }

  @Test
  void durationInSeconds() {
    int expected = (int) FINAL_STATE.getElapsedTimeSeconds();
    assertEquals(expected, subject.durationInSeconds());
    assertEquals(expected, subjectWithPenalty.durationInSeconds());
  }

  @Test
  void timePenalty() {
    int expected = (int) TIME_PENALTY.toSeconds();
    assertEquals(RaptorConstants.TIME_NOT_SET, subject.timePenalty());
    assertEquals(expected, subjectWithPenalty.timePenalty());
  }

  @Test
  void stop() {
    assertEquals(STOP, subject.stop());
  }

  @Test
  void generalizedCost() {
    // TODO - The value is ?
    int expected = 23642959;
    assertEquals(expected, subject.c1());
    assertEquals(expected + COST_PENALTY.toCentiSeconds(), subjectWithPenalty.c1());
  }

  @Test
  void hasOpeningHours() {
    assertFalse(subject.hasOpeningHours());
  }

  /**
   * A long access leg — e.g. a walk over steep, wheelchair-inaccessible stairs — can accumulate a
   * generalized cost so large that converting it straight to Raptor's {@code int} cost overflows to
   * a negative value, crashing the whole request (issue #7679). The cost must instead be capped to
   * {@link CostLimit#MAX_COST} so a valid (finite, non-negative) cost is produced.
   */
  @Test
  void generalizedCostDoesNotOverflow() {
    var builder = TestStateBuilder.ofWalking();
    for (int i = 0; i < 20; i++) {
      builder.streetEdge();
    }
    var state = builder.build();

    // Precondition: the raw weight must exceed the cap, otherwise this test proves nothing.
    assertTrue(
      state.getWeight() > CostLimit.MAX_COST,
      () -> "test fixture weight " + state.getWeight() + " must exceed the cap to be meaningful"
    );

    var subject = new DefaultAccessEgress(STOP, state);

    assertEquals(RaptorCostConverter.toRaptorCost((double) CostLimit.MAX_COST), subject.c1());
    assertTrue(subject.c1() > 0, "capped access cost must stay positive");
  }

  @Test
  void getFinalState() {
    assertEquals(FINAL_STATE, subject.getFinalState());
  }

  @Test
  void containsModeWalkOnly() {
    var stateWalk = TestStateBuilder.ofWalking().build();
    var subject = new DefaultAccessEgress(0, stateWalk);
    assertTrue(subject.isWalkOnly());

    var carRentalState = TestStateBuilder.ofCarRental().streetEdge().pickUpCarFromStation().build();
    subject = new DefaultAccessEgress(0, carRentalState);
    assertFalse(subject.isWalkOnly());
  }

  @Test
  void penalty() {
    assertEquals(TimeAndCost.ZERO, subject.penalty());
    assertEquals(PENALTY, subjectWithPenalty.penalty());
  }

  @Test
  void earliestDepartureTime() {
    assertEquals(89, subject.earliestDepartureTime(89));
  }

  @Test
  void latestArrivalTime() {
    assertEquals(89, subject.latestArrivalTime(89));
  }

  @Test
  void testToString() {
    assertEquals("Walk 1d8h50m15s C₁236_429 ~ 5", subject.toString());
    assertEquals(
      "Walk 1d8h50m15s C₁236_440 Pₜ1 w/penalty(1s $11) ~ 5",
      subjectWithPenalty.toString()
    );
  }

  /**
   * Verify that the scalar values extracted during DefaultAccessEgress construction
   * (duration, generalized cost, walk-only mode) are identical for reversed and unreversed
   * State chains. This invariant allows deferring State.reverse() from AccessEgressMapper
   * to GraphPath construction, where it is only applied to winning paths rather than all
   * candidates.
   */
  @Test
  void scalarValuesAreIdenticalForReversedAndUnreversedState() {
    var state = TestStateBuilder.ofWalking().streetEdge().streetEdge().streetEdge().build();

    var fromUnreversed = new DefaultAccessEgress(STOP, state);
    var fromReversed = new DefaultAccessEgress(STOP, state.reverse());

    assertEquals(fromUnreversed.durationInSeconds(), fromReversed.durationInSeconds());
    assertEquals(fromUnreversed.c1(), fromReversed.c1());
    assertEquals(fromUnreversed.isWalkOnly(), fromReversed.isWalkOnly());
  }
}
