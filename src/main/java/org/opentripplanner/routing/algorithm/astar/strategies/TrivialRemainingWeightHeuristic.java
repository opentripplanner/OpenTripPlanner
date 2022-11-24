package org.opentripplanner.routing.algorithm.astar.strategies;

import java.util.Set;
import org.opentripplanner.astar.spi.AStarState;
import org.opentripplanner.astar.spi.AStarVertex;
import org.opentripplanner.astar.spi.RemainingWeightHeuristic;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;

/**
 * A trivial heuristic that always returns 0, which is always admissible. For use in testing,
 * troubleshooting, and spatial analysis applications where there is no target.
 */
public class TrivialRemainingWeightHeuristic<
  State extends AStarState<State, ?, Vertex>, Vertex extends AStarVertex<State, ?, Vertex>
>
  implements RemainingWeightHeuristic<State, Vertex> {

  @Override
  public void initialize(
    RouteRequest request,
    StreetMode streetMode,
    Set<Vertex> fromVertices,
    Set<Vertex> toVertices
  ) {}

  @Override
  public double estimateRemainingWeight(State s) {
    return 0;
  }
}
