package org.opentripplanner.raptor.rangeraptor.standard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.rangeraptor.lifecycle.LifeCycleEventPublisher;
import org.opentripplanner.raptor.rangeraptor.lifecycle.LifeCycleSubscriptions;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;

class StdTransferEarlyPruningTest {

  private static final int EGRESS_STOP = 5;
  private static final int EGRESS_DURATION = 200;
  private static final int OTHER_STOP = 99;
  private static final int N_ROUNDS = 5;

  private LifeCycleSubscriptions subscriptions;
  private LifeCycleEventPublisher lifeCycle;
  private StdTransferEarlyPruning<TestTripSchedule> subject;

  @BeforeEach
  void setUp() {
    subscriptions = new LifeCycleSubscriptions();
    subject = new StdTransferEarlyPruning<TestTripSchedule>(
      List.of(TestAccessEgress.walk(EGRESS_STOP, EGRESS_DURATION)),
      N_ROUNDS,
      RaptorTransitCalculator.testDummyCalculator(true),
      subscriptions
    );
    lifeCycle = new LifeCycleEventPublisher(subscriptions);
  }

  @Test
  void noPruningBeforeAnyDestinationArrival() {
    lifeCycle.setupIteration(0);
    lifeCycle.prepareForNextRound(0);

    assertFalse(subject.exceedsBound(1000));
    assertFalse(subject.exceedsBound(100_000));
  }

  @Test
  void prunesOnceDestinationIsReached() {
    lifeCycle.setupIteration(0);
    lifeCycle.prepareForNextRound(0);

    // Arrive at egress stop at t=1000 → dest arrival = 1000 + 200 = 1200
    subject.updateArrival(EGRESS_STOP, 1000);

    assertFalse(subject.exceedsBound(1199));
    assertTrue(subject.exceedsBound(1200));
    assertTrue(subject.exceedsBound(1201));
  }

  @Test
  void withinIterationBoundPrunesAcrossRounds() {
    lifeCycle.setupIteration(0);
    lifeCycle.prepareForNextRound(0);
    // Round 0: reach egress stop → destArrival = 1200; bestDestCurrentIteration = 1200
    subject.updateArrival(EGRESS_STOP, 1000);

    // Round 1: no new egress arrival, but within-iteration bound from round 0 still applies
    lifeCycle.prepareForNextRound(1);

    assertFalse(subject.exceedsBound(1199));
    assertTrue(subject.exceedsBound(1200));
  }

  @Test
  void withinIterationBoundResetsAcrossIterations() {
    lifeCycle.setupIteration(0);
    lifeCycle.prepareForNextRound(0);
    // Iteration 0, round 0: reach egress stop → bestDestCurrentIteration = 1200
    subject.updateArrival(EGRESS_STOP, 1000);
    assertTrue(subject.exceedsBound(1200));

    // Start a new iteration: bestDestCurrentIteration resets; round 1 has no per-round bound
    lifeCycle.setupIteration(60);
    lifeCycle.prepareForNextRound(1);

    // Round 1 has no bound from any previous iteration or this iteration → no pruning
    assertFalse(subject.exceedsBound(1200));
    assertFalse(subject.exceedsBound(100_000));
  }

  @Test
  void perRoundBoundPrunesAcrossIterationsForSameRound() {
    lifeCycle.setupIteration(0);
    lifeCycle.prepareForNextRound(0);
    // Iteration 0, round 0: bestDestArrivalByRound[0] = 1200
    subject.updateArrival(EGRESS_STOP, 1000);

    // New iteration: within-iteration bound resets
    lifeCycle.setupIteration(60);
    lifeCycle.prepareForNextRound(0);

    // Round 0 still has the per-round bound from iteration 0 → pruning applies
    assertFalse(subject.exceedsBound(1199));
    assertTrue(subject.exceedsBound(1200));
  }

  @Test
  void perRoundBoundDoesNotPruneADifferentRound() {
    lifeCycle.setupIteration(0);
    lifeCycle.prepareForNextRound(0);
    // Iteration 0, round 0: bestDestArrivalByRound[0] = 1200
    subject.updateArrival(EGRESS_STOP, 1000);

    // New iteration, different round: per-round bound for round 1 is unreached → no pruning
    lifeCycle.setupIteration(60);
    lifeCycle.prepareForNextRound(1);

    assertFalse(subject.exceedsBound(1200));
    assertFalse(subject.exceedsBound(100_000));
  }

  @Test
  void nonEgressStopDoesNotUpdateBound() {
    lifeCycle.setupIteration(0);
    lifeCycle.prepareForNextRound(0);

    subject.updateArrival(OTHER_STOP, 1000);

    assertFalse(subject.exceedsBound(1000));
    assertFalse(subject.exceedsBound(100_000));
  }

  @Test
  void minEgressDurationUsedForMultiplePathsToSameStop() {
    // Three egress paths to the same stop; min duration is 200
    subscriptions = new LifeCycleSubscriptions();
    subject = new StdTransferEarlyPruning<TestTripSchedule>(
      List.of(
        TestAccessEgress.walk(EGRESS_STOP, 400),
        TestAccessEgress.walk(EGRESS_STOP, 200),
        TestAccessEgress.walk(EGRESS_STOP, 300)
      ),
      N_ROUNDS,
      RaptorTransitCalculator.testDummyCalculator(true),
      subscriptions
    );
    lifeCycle = new LifeCycleEventPublisher(subscriptions);

    lifeCycle.setupIteration(0);
    lifeCycle.prepareForNextRound(0);

    // destArrival = 1000 + min(400, 200, 300) = 1200
    subject.updateArrival(EGRESS_STOP, 1000);

    assertFalse(subject.exceedsBound(1199));
    assertTrue(subject.exceedsBound(1200));
  }

  @Test
  void tighterBoundUsedWhenBothBoundsAreSet() {
    // Iteration 0: reach egress stop at round 0 with a slow path; destArrival = 1400
    lifeCycle.setupIteration(0);
    lifeCycle.prepareForNextRound(0);
    subject.updateArrival(EGRESS_STOP, 1200);

    // Iteration 1: reach egress stop at round 0 with a faster path; destArrival = 1200
    lifeCycle.setupIteration(60);
    lifeCycle.prepareForNextRound(0);
    subject.updateArrival(EGRESS_STOP, 1000);

    // bestDestArrivalByRound[0] was updated to 1200 (tighter than 1400), so pruning at 1200
    assertFalse(subject.exceedsBound(1199));
    assertTrue(subject.exceedsBound(1200));
  }
}
