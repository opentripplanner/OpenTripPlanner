package org.opentripplanner.routing.algorithm.astar.strategies;

import java.io.Serializable;
import java.util.Set;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;

/**
 * Interface for classes that provides an admissible estimate of (lower bound on) the weight of a
 * path to the target, starting from a given state.
 */
public interface RemainingWeightHeuristic extends Serializable {
  /**
   * Perform any one-time setup and pre-computation that will be needed by later calls to
   * computeForwardWeight/computeReverseWeight. We may want to start from multiple origin states, so
   * initialization cannot depend on the origin vertex or state.
   */
  void initialize(
    StreetMode streetMode,
    Set<Vertex> fromVertices,
    Set<Vertex> toVertices,
    boolean arriveBy,
    RoutingPreferences preferences
  );

  double estimateRemainingWeight(State s);
}
