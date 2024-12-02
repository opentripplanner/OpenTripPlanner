package org.opentripplanner.street.search.state;

import java.util.Collection;
import java.util.Optional;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.strategy.DominanceFunctions;

/**
 * This is a very reduced version of the A* algorithm: from an initial state a number of edges are
 * traversed in sequential order. It doesn't take into account the potential other paths that are
 * possible.
 * <p>
 * This is not a general search algorithm! It's only useful for calculating cost and time of
 * traversing a predetermined set of edges.
 */
public class EdgeTraverser {

  public static Optional<State> traverseEdges(
    final Collection<State> initialStates,
    final Collection<Edge> edges
  ) {
    return traverseEdges(initialStates.toArray(new State[0]), edges);
  }

  public static Optional<State> traverseEdges(
    final State[] initialStates,
    final Collection<Edge> edges
  ) {
    if (edges.isEmpty()) {
      return Optional.of(initialStates[0]);
    }

    // The shortest path tree is used to prune dominated parallel states. For example,
    // CAR_PICKUP can return both a CAR/WALK state after each traversal of which only
    // the optimal states need to be continued.
    var dominanceFunction = new DominanceFunctions.MinimumWeight();
    var spt = new ShortestPathTree<>(dominanceFunction);
    for (State initialState : initialStates) {
      spt.add(initialState);
    }

    Vertex lastVertex = null;
    var isArriveBy = initialStates[0].getRequest().arriveBy();
    for (Edge e : edges) {
      var vertex = isArriveBy ? e.getToVertex() : e.getFromVertex();
      var fromStates = spt.getStates(vertex);
      if (fromStates == null || fromStates.isEmpty()) {
        return Optional.empty();
      }

      for (State fromState : fromStates) {
        var newToStates = e.traverse(fromState);
        for (State newToState : newToStates) {
          spt.add(newToState);
        }
      }

      lastVertex = isArriveBy ? e.getFromVertex() : e.getToVertex();
    }

    return Optional.ofNullable(lastVertex).map(spt::getState);
  }
}
