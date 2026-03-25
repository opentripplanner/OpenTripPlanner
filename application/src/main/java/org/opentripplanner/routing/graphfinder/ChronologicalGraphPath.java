package org.opentripplanner.routing.graphfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.state.State;

/**
 * Extracts the traversed edges and effective walk distance from a {@link State} chain produced by
 * an A* street search. The state chain is a linked list from the final state back to the origin via
 * {@link State#getBackState()}/{@link State#getBackEdge()}.
 * <p>
 * This utility encapsulates the direction-dependent ordering: for depart-after searches the chain
 * yields edges in reverse chronological order (newest first), while for arriveBy searches the chain
 * already yields edges in chronological order.
 * Implementation note: an earlier design relied on {@link org.opentripplanner.astar.model.GraphPath}
 * to extract the list of edges in chronological order / reverse chronological order.
 * The current implementation is optimized for reducing memory allocation.
 */
class ChronologicalGraphPath {

  private final List<Edge> edges;
  private final double effectiveWalkDistance;

  private ChronologicalGraphPath(List<Edge> edges, double effectiveWalkDistance) {
    this.edges = edges;
    this.effectiveWalkDistance = effectiveWalkDistance;
  }

  /**
   * Walk the state chain and collect edges in chronological order (origin → destination), summing
   * up the effective walk distance along the way.
   */
  static ChronologicalGraphPath of(State state) {
    double walkDistance = 0.0;
    var edges = new ArrayList<Edge>();
    for (State cur = state; cur != null; cur = cur.getBackState()) {
      Edge backEdge = cur.getBackEdge();
      if (backEdge != null && cur.getBackState() != null) {
        walkDistance += backEdge.getEffectiveWalkDistance();
        edges.add(backEdge);
      }
    }
    // For depart-after, edges are in reverse chronological order; reverse to chronological.
    // For arriveBy, the A* searched backward so edges are already chronological.
    if (!state.getRequest().arriveBy()) {
      Collections.reverse(edges);
    }
    return new ChronologicalGraphPath(edges, walkDistance);
  }

  List<Edge> edges() {
    return edges;
  }

  double effectiveWalkDistance() {
    return effectiveWalkDistance;
  }
}
