package org.opentripplanner.raptor.rangeraptor.transit;

import org.opentripplanner.raptor.rangeraptor.internalapi.RoundProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;

/**
 * Round tracker to keep track of round index and when to stop exploring new rounds.
 * <p>
 * In round 0 the access paths with one leg are added. In round 1 the first transit and transfers is
 * added, ...
 */
public class RoundTracker implements RoundProvider {

  /**
   * The extra number of rounds/transfers we accept compared to the trip with the fewest number of
   * transfers. This is used to abort the search.
   */
  private final int numberOfAdditionalTransfers;

  /**
   * The current round in progress (round index).
   */
  private int round = 0;

  /**
   * The round upper limit for when to abort the search.
   * <p/>
   * This is default set to the maximum number of rounds limit, but as soon as the destination is
   * reach the {@link #numberOfAdditionalTransfers} is used to update the limit.
   * <p/>
   * The limit is inclusive, indicating the the last round to process.
   */
  private int roundMaxLimit;

  public RoundTracker(int nRounds, int numberOfAdditionalTransfers, WorkerLifeCycle lifeCycle) {
    // The 'roundMaxLimit' is inclusive, while the 'nRounds' is exclusive; Hence subtract 1.
    this.roundMaxLimit = nRounds;
    this.numberOfAdditionalTransfers = numberOfAdditionalTransfers;
    lifeCycle.onSetupIteration(t -> setupIteration());
    lifeCycle.onRoundComplete(this::roundComplete);
  }

  /** Is there more rounds to process (or is the upper limit reached). */
  public boolean hasMoreRounds() {
    return round + 1 < roundMaxLimit;
  }

  /** Increment round counter */
  public int nextRound() {
    return ++round;
  }

  /**
   * Return the current round, the round in process.
   */
  public int round() {
    return round;
  }

  /**
   * Return true if this round is the fist round, calculating the first transit path. Access is
   * calculated in round zero (0).
   */
  public boolean isFirstRound() {
    return round == 1;
  }

  /**
   * Before each iteration, initialize the round to 0.
   */
  private void setupIteration() {
    round = 0;
  }

  /**
   * Set the round limit based on the 'numberOfAdditionalTransfers' parameter.
   */
  private void roundComplete(boolean destinationReached) {
    if (destinationReached) {
      recalculateMaxLimitBasedOnDestinationReachedInCurrentRound();
    }
  }

  /* private methods */

  private void recalculateMaxLimitBasedOnDestinationReachedInCurrentRound() {
    // Rounds start at 0 (access arrivals), and round is not incremented jet
    roundMaxLimit = Math.min(roundMaxLimit, round + numberOfAdditionalTransfers + 1);
  }
}
