package org.opentripplanner.astar;

import org.opentripplanner.astar.spi.AStarState;

public interface DominanceFunction<State extends AStarState<State, ?, ?>> {
  boolean betterOrEqualAndComparable(State a, State b);
}
