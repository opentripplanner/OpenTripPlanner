package org.opentripplanner.routing.algorithm.astar.strategies;

import java.util.Set;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;

/**
 * A trivial heuristic that always returns 0, which is always admissible. For use in testing,
 * troubleshooting, and spatial analysis applications where there is no target.
 */
public class TrivialRemainingWeightHeuristic implements RemainingWeightHeuristic {

  @Override
  public void initialize(
    StreetMode streetMode,
    Set<Vertex> fromVertices,
    Set<Vertex> toVertices,
    boolean arriveBy,
    RoutingPreferences preferences
  ) {}

  @Override
  public double estimateRemainingWeight(State s) {
    return 0;
  }
}
