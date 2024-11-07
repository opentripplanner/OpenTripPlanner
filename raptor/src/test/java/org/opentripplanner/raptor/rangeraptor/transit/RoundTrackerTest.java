package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;

public class RoundTrackerTest {

  private static final int ANY = 500;
  private IntConsumer setupIteration;
  private Consumer<Boolean> roundComplete;

  @Test
  public void testRoundTracker() {
    // Set number of rounds to 3, this include the access arrivals in round 0
    RoundTracker subject = new RoundTracker(3, 2, lifeCycle());

    setupIteration.accept(ANY);
    assertEquals(0, subject.round());

    assertTrue(subject.hasMoreRounds());
    assertEquals(0, subject.round());
    assertEquals(1, subject.nextRound());

    roundComplete.accept(false);
    // Verify the round counter is still correct in the "round complete" phase.
    assertEquals(1, subject.round());

    assertTrue(subject.hasMoreRounds());
    assertEquals(2, subject.nextRound());

    assertFalse(subject.hasMoreRounds());

    // Start over for new iteration
    setupIteration.accept(ANY);
    assertEquals(0, subject.round());
    assertTrue(subject.hasMoreRounds());
    assertEquals(1, subject.nextRound());
  }

  @Test
  public void testRoundTrackerWhenDestinationIsReached() {
    RoundTracker subject = new RoundTracker(10, 2, lifeCycle());

    setupIteration.accept(500);
    assertEquals(0, subject.round());

    assertTrue(subject.hasMoreRounds());
    assertEquals(0, subject.round());
    assertEquals(1, subject.nextRound());

    // Destination reached in round 1
    roundComplete.accept(true);

    assertTrue(subject.hasMoreRounds());
    assertEquals(2, subject.nextRound());

    assertTrue(subject.hasMoreRounds());
    assertEquals(3, subject.nextRound());

    assertFalse(subject.hasMoreRounds());
  }

  private WorkerLifeCycle lifeCycle() {
    return new WorkerLifeCycle() {
      @Override
      public void onRouteSearch(Consumer<Boolean> routeSearchWithDirectionSubscriber) {
        throw new IllegalStateException("Not expected");
      }

      @Override
      public void onSetupIteration(IntConsumer setupIteration) {
        RoundTrackerTest.this.setupIteration = setupIteration;
      }

      @Override
      public void onPrepareForNextRound(IntConsumer prepareForNextRound) {
        throw new IllegalStateException("Not expected");
      }

      @Override
      public void onTransitsForRoundComplete(Runnable transitsForRoundComplete) {
        throw new IllegalStateException("Not expected");
      }

      @Override
      public void onTransfersForRoundComplete(Runnable transfersForRoundComplete) {
        throw new IllegalStateException("Not expected");
      }

      @Override
      public void onRoundComplete(Consumer<Boolean> roundCompleteWithDestinationReached) {
        RoundTrackerTest.this.roundComplete = roundCompleteWithDestinationReached;
      }

      @Override
      public void onIterationComplete(Runnable iterationComplete) {
        throw new IllegalStateException("Not expected");
      }
    };
  }
}
