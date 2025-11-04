package org.opentripplanner.astar.strategy;

import java.util.function.Predicate;
import org.opentripplanner.astar.spi.AStarState;
import org.opentripplanner.astar.spi.SearchTerminationStrategy;

/**
 * This termination strategy is used to terminate an a-star search after a number of states matching
 * some criteria has been found. For example it can be used to limit a search to a maximum number of
 * stops.
 */
public class MaxCountTerminationStrategy<State extends AStarState<State, ?, ?>>
  implements SearchTerminationStrategy<State> {

  private final int maxCount;
  private final Predicate<State> shouldIncreaseCount;
  private int count;

  /**
   * @param maxCount Terminate the search after this many matching states have been reached.
   * @param shouldIncreaseCount A predicate to check if a state should increase the count or not.
   */
  public MaxCountTerminationStrategy(int maxCount, Predicate<State> shouldIncreaseCount) {
    this.maxCount = maxCount;
    this.shouldIncreaseCount = shouldIncreaseCount;
    this.count = 0;
  }

  @Override
  public boolean shouldSearchTerminate(State current) {
    if (shouldIncreaseCount.test(current)) {
      count++;
    }
    return count >= maxCount;
  }
}
