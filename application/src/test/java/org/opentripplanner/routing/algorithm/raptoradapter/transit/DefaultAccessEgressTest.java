package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;

class DefaultAccessEgressTest {

  private static final int STOP = 5;
  private static final State LAST_STATE = TestStateBuilder.ofWalking().streetEdge().build();
  public static final Duration TIME_PENALTY = Duration.ofSeconds(1);
  public static final Cost COST_PENALTY = Cost.costOfSeconds(11);
  public static final TimeAndCost PENALTY = new TimeAndCost(TIME_PENALTY, COST_PENALTY);

  private final DefaultAccessEgress subject = new DefaultAccessEgress(STOP, LAST_STATE);
  private final RoutingAccessEgress subjectWithPenalty = subject.withPenalty(PENALTY);

  @Test
  void canNotAddPenaltyTwice() {
    assertThrows(IllegalStateException.class, () -> subjectWithPenalty.withPenalty(PENALTY));
  }

  @Test
  void durationInSeconds() {
    int expected = (int) LAST_STATE.getElapsedTimeSeconds();
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

  @Test
  void getLastState() {
    assertEquals(LAST_STATE, subject.getLastState());
  }

  /**
   * @deprecated TODO - This test dos not test a single line in DefaultAccessEgress. If the
   *                    test have value move it to where it belong (StateTest ?).
   */
  @Deprecated
  @Test
  void containsDriving() {
    var state = TestStateBuilder.ofDriving().streetEdge().streetEdge().streetEdge().build();
    var access = new DefaultAccessEgress(0, state);
    assertTrue(access.getLastState().containsModeCar());
  }

  /**
   * @deprecated TODO - This test dos not test a single line in DefaultAccessEgress. If the
   *                    test have value move it to where it belong (StateTest ?).
   */
  @Deprecated
  @Test
  void walking() {
    var state = TestStateBuilder.ofWalking().streetEdge().streetEdge().streetEdge().build();
    var access = new DefaultAccessEgress(0, state);
    assertFalse(access.getLastState().containsModeCar());
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
    assertEquals("Walk 1d8h50m15s C₁236_440 w/penalty(1s $11) ~ 5", subjectWithPenalty.toString());
  }
}
