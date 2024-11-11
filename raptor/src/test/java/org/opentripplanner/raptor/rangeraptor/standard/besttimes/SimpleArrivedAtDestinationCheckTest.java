package org.opentripplanner.raptor.rangeraptor.standard.besttimes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.rangeraptor.lifecycle.LifeCycleEventPublisher;
import org.opentripplanner.raptor.rangeraptor.lifecycle.LifeCycleSubscriptions;
import org.opentripplanner.raptor.rangeraptor.transit.ForwardTransitCalculator;

class SimpleArrivedAtDestinationCheckTest {

  private static final int ANY_TIME = 999;

  @Test
  void arrivedAtDestinationCurrentRound() {
    // Replace this with dependency injection
    var lifeCycleSubscriptions = new LifeCycleSubscriptions();
    var bestTimes = new BestTimes(
      3,
      new ForwardTransitCalculator<TestTripSchedule>(),
      lifeCycleSubscriptions
    );
    var subject = new SimpleArrivedAtDestinationCheck(bestTimes, new int[] { 1 }, new int[] { 2 });
    var lifeCycle = new LifeCycleEventPublisher(lifeCycleSubscriptions);

    lifeCycle.setupIteration(0);
    lifeCycle.prepareForNextRound(0);
    assertFalse(subject.arrivedAtDestinationCurrentRound());
    lifeCycle.roundComplete(true);

    lifeCycle.prepareForNextRound(1);
    bestTimes.updateNewBestTime(0, ANY_TIME);
    bestTimes.updateNewBestTime(1, ANY_TIME);
    assertFalse(subject.arrivedAtDestinationCurrentRound());
    lifeCycle.roundComplete(false);

    lifeCycle.prepareForNextRound(2);
    bestTimes.updateNewBestTime(2, ANY_TIME);
    assertTrue(subject.arrivedAtDestinationCurrentRound());
    lifeCycle.roundComplete(true);

    lifeCycle.prepareForNextRound(3);
    bestTimes.updateNewBestTime(0, ANY_TIME);
    assertFalse(subject.arrivedAtDestinationCurrentRound());
    lifeCycle.roundComplete(false);

    lifeCycle.prepareForNextRound(4);
    bestTimes.updateBestTransitArrivalTime(1, ANY_TIME);
    assertTrue(subject.arrivedAtDestinationCurrentRound());
    lifeCycle.roundComplete(true);

    lifeCycle.prepareForNextRound(5);
    bestTimes.updateNewBestTime(0, ANY_TIME);
    assertFalse(subject.arrivedAtDestinationCurrentRound());
  }
}
