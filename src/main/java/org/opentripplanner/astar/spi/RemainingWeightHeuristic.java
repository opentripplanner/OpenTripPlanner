package org.opentripplanner.astar.spi;

import java.io.Serializable;
import java.util.Set;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;

/**
 * Interface for classes that provides an admissible estimate of (lower bound on) the weight of a
 * path to the target, starting from a given state.
 */
public interface RemainingWeightHeuristic<
  State extends AStarState<State, ?, Vertex>, Vertex extends AStarVertex<State, ?, Vertex>
>
  extends Serializable {
  /**
   * Perform any one-time setup and pre-computation that will be needed by later calls to
   * computeForwardWeight/computeReverseWeight. We may want to start from multiple origin states, so
   * initialization cannot depend on the origin vertex or state.
   */
  void initialize(
    RouteRequest request,
    StreetMode streetMode,
    Set<Vertex> fromVertices,
    Set<Vertex> toVertices
  );

  double estimateRemainingWeight(State s);
}
