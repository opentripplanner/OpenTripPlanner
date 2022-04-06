package org.opentripplanner.routing.algorithm.astar.strategies;

import org.opentripplanner.routing.core.State;

/**
 * Immediately terminates the search once the condition has been reached. This can be useful for
 * checking that the required number of targets have been reached, but not for limiting searches but
 * distance or duration, as it will not continue searching along other paths once the condition has
 * been met.
 */
public interface SearchTerminationStrategy {
  /**
   * @param current the current shortest path tree vertex
   * @return true if the specified search should be terminated
   */
  boolean shouldSearchTerminate(State current);
}
