package org.opentripplanner.street.search.state;

import java.util.Collection;
import java.util.Optional;
import org.opentripplanner.street.model.edge.Edge;

/**
 * This is a very reduced version of the A* algorithm: from an initial state a number of edges are
 * traversed in sequential order. It doesn't take into account the potential other paths that are
 * possible.
 * <p>
 * This is not a general search algorithm! It's only useful for calculating cost and time of
 * traversing a predetermined set of edges.
 */
public class EdgeTraverser {

  public static Optional<State> traverseEdges(final State s, final Collection<Edge> edges) {
    var state = s;
    for (Edge e : edges) {
      var afterTraversal = e.traverse(state);
      if (afterTraversal.length > 1) {
        throw new IllegalStateException(
          "Expected only a single state returned from edge %s but received %s".formatted(
              e,
              afterTraversal.length
            )
        );
      }
      if (State.isEmpty(afterTraversal)) {
        return Optional.empty();
      } else {
        state = afterTraversal[0];
      }
    }
    return Optional.ofNullable(state);
  }
}
