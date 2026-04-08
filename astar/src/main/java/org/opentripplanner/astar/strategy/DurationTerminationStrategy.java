package org.opentripplanner.astar.strategy;

import java.time.Duration;
import org.opentripplanner.astar.spi.AStarState;
import org.opentripplanner.astar.spi.SearchTerminationStrategy;

/**
 * This termination strategy is used to terminate an a-star search after a fixed duration
 * has elapsed.
 */
public class DurationTerminationStrategy<State extends AStarState<State, ?, ?>>
  implements SearchTerminationStrategy<State> {

  private final double maxDuration_s;

  public DurationTerminationStrategy(Duration duration) {
    maxDuration_s = duration.toSeconds();
  }

  @Override
  public boolean shouldSearchTerminate(State current) {
    return current.getElapsedTimeSeconds() > maxDuration_s;
  }
}
