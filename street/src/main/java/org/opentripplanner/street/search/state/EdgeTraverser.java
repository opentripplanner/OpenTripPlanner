package org.opentripplanner.street.search.state;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;

/**
 * This is a very reduced version of the A* algorithm: from an initial state a number of edges are
 * traversed in sequential order. It doesn't take into account the potential other paths that are
 * possible.
 * <p>
 * This is not a general search algorithm! It's only useful for calculating cost and time of
 * traversing a predetermined set of edges.
 */
public class EdgeTraverser {

  /// Create a path from a set of edges and a street search request.
  ///
  /// @return Empty if the list of edges is empty or if the edges can't be traversed using the given
  /// request
  public static Optional<StreetPath> createPath(List<Edge> edges, StreetSearchRequest request) {
    if (edges.isEmpty()) {
      return Optional.empty();
    }

    var states = new ArrayList<State>(edges.size() + 1);
    var it = new StateIterator(createStartState(edges.getFirst().getFromVertex(), request), edges);
    try {
      it.forEachRemaining(states::add);
    } catch (CouldNotTraverseException _) {
      return Optional.empty();
    }
    return Optional.of(new StreetPath(states, edges));
  }

  /// Traverse a set of edges given a start state. Return the final state, empty if the edges cannot
  /// be traversed.
  public static Optional<State> traverseEdges(State s, List<Edge> edges) {
    var it = new StateIterator(s, edges);
    var state = s;
    while (it.hasNext()) {
      try {
        state = it.next();
      } catch (CouldNotTraverseException _) {
        return Optional.empty();
      }
    }
    return Optional.of(state);
  }

  private static State createStartState(Vertex vertex, StreetSearchRequest request) {
    return new StateEditor(vertex, request).makeState();
  }

  private static class StateIterator implements Iterator<State> {

    private final Iterator<Edge> edgeIterator;
    private State state;
    private boolean first;

    public StateIterator(State startState, List<Edge> edges) {
      edgeIterator = edges.iterator();
      first = true;
      state = startState;
    }

    @Override
    public boolean hasNext() {
      return first || edgeIterator.hasNext();
    }

    @Override
    public State next() {
      if (first) {
        first = false;
        return state;
      }

      var edge = edgeIterator.next();
      var nextState = edge.traverse(state);
      if (nextState.length > 1) {
        throw new IllegalStateException(
          "Expected only a single state returned from edge %s but received %s".formatted(
            edge,
            nextState.length
          )
        );
      } else if (State.isEmpty(nextState)) {
        throw new CouldNotTraverseException();
      }

      state = nextState[0];
      return state;
    }
  }

  private static class CouldNotTraverseException extends RuntimeException {}
}
