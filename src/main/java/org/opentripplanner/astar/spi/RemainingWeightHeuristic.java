package org.opentripplanner.astar.spi;

import java.io.Serializable;

/**
 * Interface for classes that provides an admissible estimate of (lower bound on) the weight of a
 * path to the target, starting from a given state.
 */
public interface RemainingWeightHeuristic<State extends AStarState<State, ?, ?>>
  extends Serializable {
  /**
   * A trivial heuristic that always returns 0, which is always admissible. For use in testing,
   * troubleshooting, and spatial analysis applications where there is no target.
   */
  @SuppressWarnings("rawtypes")
  RemainingWeightHeuristic TRIVIAL = s -> 0;

  double estimateRemainingWeight(State s);
}
