package org.opentripplanner.astar.spi;

/**
 * Interface for classes that provides an admissible estimate of (lower bound on) the weight of a
 * path to the target, starting from a given state.
 */
public interface RemainingWeightHeuristic<State extends AStarState<State, ?, ?>> {
  double estimateRemainingWeight(State s);
}
