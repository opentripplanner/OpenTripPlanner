package org.opentripplanner.astar.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
  private final SegmentedIdentityMap<Edge, Object> edgeStates;
  private final Map<Vertex, List<State>> initialStates;
  private final boolean isReverse;

  public ShortestPathTree(DominanceFunction<State> dominanceFunction, boolean isReverse) {
    this.dominanceFunction = dominanceFunction;
    // Initialized with a reasonable size, see #4445
    edgeStates = new SegmentedIdentityMap<>(25_000);
    initialStates = new HashMap<>(10);
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
    Edge backEdge = newState.getBackEdge();
    if (backEdge == null) {
      Vertex vertex = newState.getVertex();
      initialStates.computeIfAbsent(vertex, k -> new ArrayList<>()).add(newState);
      return true;
    }

    Object existing = edgeStates.get(backEdge);

    // if the vertex has no states, store directly (no list wrapper)
    if (existing == null) {
      edgeStates.put(backEdge, newState);
      return true;
    }

    // Single-state fast path (99% of vertices)
    if (!(existing instanceof List)) {
      State oldState = (State) existing;
      // order is important, because in the case of a tie we want to reject the new state
      if (dominanceFunction.betterOrEqualAndComparable(oldState, newState)) {
        return false;
      }
      if (dominanceFunction.betterOrEqualAndComparable(newState, oldState)) {
        edgeStates.put(backEdge, newState);
        return true;
      }
      // Co-dominant: promote to list
      List<State> list = new ArrayList<>(2);
      list.add(oldState);
      list.add(newState);
      edgeStates.put(backEdge, list);
      return true;
    }

    // Multi-state path: existing iterator-based dominance logic
    List<State> states = (List<State>) existing;
    Iterator<State> it = states.iterator();
    while (it.hasNext()) {
      State oldState = it.next();
      // order is important, because in the case of a tie we want to reject the new state
      if (dominanceFunction.betterOrEqualAndComparable(oldState, newState)) {
        return false;
      }
      if (dominanceFunction.betterOrEqualAndComparable(newState, oldState)) {
        it.remove();
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
    var best = getBestState(initialStates.get(dest));
    if (best != null) {
      return best;
    }
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
    Edge backEdge = state.getBackEdge();
    if (backEdge == null) {
      Vertex vertex = state.getVertex();
      return initialStates.containsKey(vertex) && initialStates.get(vertex).contains(state);
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
    ArrayList<State> allStates = new ArrayList<>(edgeStates.size());
    edgeStates.forEachValue(value -> {
      if (value instanceof List) {
        allStates.addAll((List<State>) value);
      } else {
        allStates.add((State) value);
      }
    });
    for (List<State> states : initialStates.values()) {
      allStates.addAll(states);
    }
    return allStates;
  }

  public String toString() {
    int size = edgeStates.size() + initialStates.size();
    return "ShortestPathTree(" + size + " vertices)";
  }
}
