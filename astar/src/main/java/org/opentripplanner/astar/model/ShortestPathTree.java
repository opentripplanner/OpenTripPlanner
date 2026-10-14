package org.opentripplanner.astar.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.astar.spi.AStarEdge;
import org.opentripplanner.astar.spi.AStarState;
import org.opentripplanner.astar.spi.AStarVertex;
import org.opentripplanner.astar.spi.DominanceFunction;

/**
 * This class keeps track which graph vertices have been visited and their associated states, so
 * that decisions can be made about whether new states should be enqueued for later exploration. It
 * also allows states to be retrieved for a given target vertex.
 * <p>
 * We no longer have different implementations of ShortestPathTree because the label-setting
 * (multi-state) approach used in turn restrictions, bike rental, etc. is a generalization of the
 * basic Dijkstra (single-state) approach. It is much more straightforward to use the more general
 * SPT implementation in all cases.
 * <p>
 * TODO: Is this still accurate?
 * Note that turn restrictions make all searches multi-state; however turn restrictions do not apply
 * when walking. The turn restriction handling is done in the base dominance function
 * implementation, and applies to all subclasses. It essentially splits each vertex into N vertices
 * depending on the incoming edge being taken.
 */
public class ShortestPathTree<
  State extends AStarState<State, Edge, Vertex>,
  Edge extends AStarEdge<State, Edge, Vertex>,
  Vertex extends AStarVertex<State, Edge, Vertex>
> {

  public final DominanceFunction<State> dominanceFunction;

  // Value is either a single State (common case) or List<State> (multi-state vertices)
  private final SegmentedIdentityMap<Vertex, Object> vertexStates;
  private final SegmentedIdentityMap<Edge, Object> edgeStates;
  private final boolean isReverse;

  public ShortestPathTree(DominanceFunction<State> dominanceFunction, boolean isReverse) {
    this.dominanceFunction = dominanceFunction;
    // Initialized with a reasonable size, see #4445
    vertexStates = new SegmentedIdentityMap<>(25_000);
    edgeStates = new SegmentedIdentityMap<>(25_000);
    this.isReverse = isReverse;
  }

  /** @return a single optimal, optionally back-optimized path to the given vertex. */
  public GraphPath<State, Edge, Vertex> getPath(Vertex dest) {
    State s = getState(dest);
    if (s == null) {
      return null;
    } else {
      return new GraphPath<>(s);
    }
  }

  private void addEdge(State newState) {
    Edge backEdge = newState.getBackEdge();
    Object edgeExisting = edgeStates.get(backEdge);

    if (edgeExisting == null) {
      edgeStates.put(backEdge, newState);
    } else {
      if (!(edgeExisting instanceof List)) {
        List list = new ArrayList(2);
        list.add(edgeExisting);
        list.add(newState);
        edgeStates.put(backEdge, list);
      } else {
        List list = (List) edgeExisting;
        list.add(newState);
      }
    }
  }

  /**
   * The add method checks a new State to see if it is non-dominated and thus worth visiting later.
   * If so, the method returns 'true' indicating that the state is deemed useful and should be
   * enqueued for later exploration. The method will also perform implementation-specific actions
   * that track dominant or optimal states.
   *
   * @param newState the State to add to the SPT, if it is deemed non-dominated
   * @return a boolean value indicating whether the state was added to the tree and should therefore
   * be enqueued
   */
  @SuppressWarnings("unchecked")
  public boolean add(State newState) {
    Vertex vertex = newState.getVertex();
    Edge backEdge = newState.getBackEdge();
    Object edgeExisting = edgeStates.get(backEdge);
    Object vertexExisting = vertexStates.get(vertex);

    if (edgeExisting == null) {
      if (vertexExisting == null) {
        vertexStates.put(vertex, newState);
        return true;
      }

      if (vertexExisting instanceof Boolean) {
        addEdge(newState);
        return true;
      }

      State oldState = (State) vertexExisting;
      if (dominanceFunction.comparable(oldState, newState)) {
        if (backEdge != null && oldState.getBackEdge() == newState.getBackEdge()) {
          if (dominanceFunction.betterOrEqual(oldState, newState)) {
            return false;
          }
          if (dominanceFunction.betterOrEqual(newState, oldState)) {
            vertexStates.put(vertex, newState);
            return true;
          }
        } else {
          if (dominanceFunction.significantlyBetter(oldState, newState)) {
            return false;
          }
          if (dominanceFunction.significantlyBetter(newState, oldState)) {
            vertexStates.put(vertex, newState);
            return true;
          }
        }
      }

      addEdge(oldState);
      addEdge(newState);
      vertexStates.put(vertex, Boolean.FALSE);
      return true;
    }

    // Single-state fast path (99% of vertices)
    if (!(edgeExisting instanceof List)) {
      State oldState = (State) edgeExisting;
      if (dominanceFunction.comparable(oldState, newState)) {
        // order is important, because in the case of a tie we want to reject the new state
        if (dominanceFunction.betterOrEqual(oldState, newState)) {
          return false;
        }
        if (dominanceFunction.betterOrEqual(newState, oldState)) {
          edgeStates.put(backEdge, newState);
          return true;
        }
      }
      // Co-dominant: promote to list
      List<State> list = new ArrayList<>(2);
      list.add(oldState);
      list.add(newState);
      edgeStates.put(backEdge, list);
      return true;
    }

    // Multi-state path: existing iterator-based dominance logic
    List<State> states = (List<State>) edgeExisting;
    Iterator<State> it = states.iterator();
    while (it.hasNext()) {
      State oldState = it.next();
      if (dominanceFunction.comparable(oldState, newState)) {
        // order is important, because in the case of a tie we want to reject the new state
        if (dominanceFunction.betterOrEqual(oldState, newState)) {
          return false;
        }
        if (dominanceFunction.betterOrEqual(newState, oldState)) {
          it.remove();
        }
      }
    }
    states.add(newState);
    return true;
  }

  /**
   * Returns the 'best' state for the given Vertex, where 'best' depends on the implementation.
   *
   * @param dest the vertex of interest
   * @return a 'best' state at that vertex
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public State getState(Vertex dest) {
    // Testing initialStates must be first. It is possible to have a followed edge back into an
    // initial state, so putting the test for initialStates last in case best is still null will
    // not work correctly. But also initial states can all be non-final, in which case we have
    // to fall through.
    var vertexExisting = vertexStates.get(dest);
    if (vertexExisting != null && !(vertexExisting instanceof Boolean)) {
      if (vertexExisting instanceof List) {
        var best = getBestState((List<State>) vertexExisting);
        if (best != null) {
          return best;
        }
      } else {
        var state = (State) vertexExisting;
        if (state.isFinal()) {
          return state;
        }
      }
    }
    State best = null;
    var edges = isReverse ? dest.getOutgoing() : dest.getIncoming();
    for (Edge edge : edges) {
      State candidate = getState(edge);
      if (
        candidate != null &&
        (best == null || dominanceFunction.betterOrEqualAndComparable(candidate, best))
      ) {
        best = candidate;
      }
    }
    return best;
  }

  public State getState(Edge dest) {
    Object existing = edgeStates.get(dest);
    if (existing == null) {
      return null;
    }
    if (!(existing instanceof List)) {
      State s = (State) existing;
      return s.isFinal() ? s : null;
    }
    return getBestState((List<State>) existing);
  }

  private State getBestState(List<State> states) {
    if (states == null) {
      return null;
    }
    State ret = null;
    for (State s : states) {
      if ((ret == null || s.getWeight() < ret.getWeight()) && s.isFinal()) {
        ret = s;
      }
    }
    return ret;
  }

  /**
   * The visit method should be called upon extracting a State from a priority queue. It checks
   * whether the State is still worth visiting (i.e. whether it has been dominated since it was
   * enqueued) and informs the ShortestPathTree that this State's outgoing edges have been relaxed.
   * A state may remain in the priority queue after being dominated, and such sub-optimal states
   * must be caught as they come out of the queue to avoid unnecessary branching.
   * <p>
   * So this function checks that a state coming out of the queue is still in the Pareto-optimal set
   * for this vertex, which indicates that it has not been ruled out as a state on an optimal path.
   * Many shortest path algorithms will decrease the key of a vertex in the priority queue when it
   * is updated, but we store states in the queue rather than vertices, and states do not get
   * updated or change their weight.
   * TODO consider just removing states from the priority queue.
   * <p>
   * When the Fibonacci heap was replaced with a binary heap, the decrease-key operation was
   * removed for the same reason: both improve theoretical run time complexity, at the cost of
   * high constant factors and more complex code.
   * <p>
   * So there can be dominated (useless) states in the queue. When they come out we want to
   * ignore them rather than spend time branching out from them.
   *
   * @param state - the state about to be visited
   * @return - whether this state is still considered worth visiting.
   */
  @SuppressWarnings("unchecked")
  public boolean visit(State state) {
    Object vertexExisting = vertexStates.get(state.getVertex());
    if (vertexExisting != null && !(vertexExisting instanceof Boolean)) {
      if (!(vertexExisting instanceof List)) {
        return vertexExisting == state;
      }
      for (State s : (List<State>) vertexExisting) {
        if (s == state) {
          return true;
        }
      }
    }

    Object existing = edgeStates.get(state.getBackEdge());
    if (!(existing instanceof List)) {
      return existing == state;
    }
    for (State s : (List<State>) existing) {
      if (s == state) {
        return true;
      }
    }
    return false;
  }

  /** @return every state in this tree */
  @SuppressWarnings("unchecked")
  public Collection<State> getAllStates() {
    ArrayList<State> allStates = new ArrayList<>(vertexStates.size() + edgeStates.size());
    vertexStates.forEachValue(value -> {
      if (value instanceof List) {
        allStates.addAll((List<State>) value);
      } else if (!(value instanceof Boolean)) {
        allStates.add((State) value);
      }
    });
    edgeStates.forEachValue(value -> {
      if (value instanceof List) {
        allStates.addAll((List<State>) value);
      } else {
        allStates.add((State) value);
      }
    });
    return allStates;
  }

  public String toString() {
    int size = edgeStates.size() + vertexStates.size();
    return "ShortestPathTree(" + size + " vertices)";
  }
}
