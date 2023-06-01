package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.search.state.TestStateBuilder;

class DefaultAccessEgressTest {

  @Test
  void containsDriving() {
    var state = TestStateBuilder.ofDriving().streetEdge().streetEdge().streetEdge().build();
    var access = new DefaultAccessEgress(0, state);
    assertTrue(access.getLastState().containsModeCar());
  }

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
}
