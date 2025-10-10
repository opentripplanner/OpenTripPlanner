package org.opentripplanner.astar.spi;

public interface DominanceFunction<State extends AStarState<State, ?, ?>> {
  boolean betterOrEqualAndComparable(State a, State b);
}
