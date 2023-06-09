package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;

class DefaultAccessEgressTest {

  private static final int STOP = 5;
  private static final State LAST_STATE = TestStateBuilder.ofWalking().streetEdge().build();

  private final DefaultAccessEgress subject = new DefaultAccessEgress(STOP, LAST_STATE);

  @Test
  void durationInSeconds() {
    // TODO - The value is ?
    int expected = 118215;
    assertEquals(expected, subject.durationInSeconds());
  }

  @Test
  void stop() {
    assertEquals(STOP, subject.stop());
  }

  @Test
  void generalizedCost() {
    // TODO - The value is ?
    int expected = 23642959;
    assertEquals(expected, subject.generalizedCost());
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

    var carRentalState = TestStateBuilder.ofCarRental().streetEdge().pickUpCar().build();
    subject = new DefaultAccessEgress(0, carRentalState);
    assertFalse(subject.isWalkOnly());
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
    assertEquals("Walk 1d8h50m15s $236429 ~ 5", subject.toString());
  }
}
