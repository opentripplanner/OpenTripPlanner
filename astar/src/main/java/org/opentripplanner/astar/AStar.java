package org.opentripplanner.astar;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.astar.model.BinHeap;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.spi.AStarEdge;
import org.opentripplanner.astar.spi.AStarState;
import org.opentripplanner.astar.spi.AStarVertex;
import org.opentripplanner.astar.spi.DominanceFunction;
import org.opentripplanner.astar.spi.RemainingWeightHeuristic;
import org.opentripplanner.astar.spi.SearchTerminationStrategy;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.utils.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Find the shortest path between graph vertices using A*. A basic Dijkstra search is a special case
 * of AStar where the heuristic is always zero.
 */
public class AStar<
  State extends AStarState<State, Edge, Vertex>,
  Edge extends AStarEdge<State, Edge, Vertex>,
  Vertex extends AStarVertex<State, Edge, Vertex>
> {

  private static final Logger LOG = LoggerFactory.getLogger(AStar.class);

  /**
   * No-op visitor used when no visitor is provided, avoiding null checks in hot path
   */
  private static final TraverseVisitor NO_OP_VISITOR = new TraverseVisitor() {
    @Override
    public void visitEdge(AStarEdge edge) {}

    @Override
    public void visitVertex(AStarState state) {}

    @Override
    public void visitEnqueue() {}
  };

  /**
   * No-op skip edge strategy used when no strategy is provided, avoiding null checks in hot path
   */
  private static final SkipEdgeStrategy NO_OP_SKIP_STRATEGY = (SkipEdgeStrategy) (_, _) -> false;

  private final boolean arriveBy;
  private final Set<Vertex> fromVertices;
  private final Set<Vertex> toVertices;
  private final RemainingWeightHeuristic<State> heuristic;
  private final Runnable preSearchHook;
  private final SkipEdgeStrategy<State, Edge> skipEdgeStrategy;
  private final SearchTerminationStrategy<State> terminationStrategy;
  private final TraverseVisitor<State, Edge> traverseVisitor;
  private final Duration timeout;

  private final ShortestPathTree<State, Edge, Vertex> spt;
  private final BinHeap<State> pq;
  private final List<State> targetAcceptedStates;

  private State u;
  private int nVisited;

  @SuppressWarnings("unchecked")
  AStar(
    RemainingWeightHeuristic<State> heuristic,
    Runnable preSearchHook,
    SkipEdgeStrategy<State, Edge> skipEdgeStrategy,
    TraverseVisitor<State, Edge> traverseVisitor,
    boolean arriveBy,
    Set<Vertex> fromVertices,
    Set<Vertex> toVertices,
    SearchTerminationStrategy<State> terminationStrategy,
    DominanceFunction<State> dominanceFunction,
    Duration timeout,
    Collection<State> initialStates
  ) {
    this.heuristic = heuristic;
    // Use no-op instances instead of null to avoid null checks in hot path
    this.skipEdgeStrategy = Objects.requireNonNullElse(skipEdgeStrategy, NO_OP_SKIP_STRATEGY);
    this.traverseVisitor = Objects.requireNonNullElse(traverseVisitor, NO_OP_VISITOR);
    this.fromVertices = fromVertices;
    this.toVertices = toVertices;
    this.arriveBy = arriveBy;
    this.terminationStrategy = terminationStrategy;
    this.timeout = Objects.requireNonNull(timeout);

    this.spt = new ShortestPathTree<>(dominanceFunction);
    this.preSearchHook = preSearchHook;

    // Initialized with a reasonable size, see #4445
    this.pq = new BinHeap<>(1000);
    this.nVisited = 0;
    this.targetAcceptedStates = new ArrayList<>();

    for (State initialState : initialStates) {
      spt.add(initialState);
      pq.insert(initialState, initialState.getWeight());
    }
  }

  ShortestPathTree<State, Edge, Vertex> getShortestPathTree() {
    runSearch();

    return spt;
  }

  List<GraphPath<State, Edge, Vertex>> getPathsToTarget() {
    runSearch();

    return targetAcceptedStates
      .stream()
      .filter(State::isFinal)
      .map(GraphPath::new)
      .collect(Collectors.toList());
  }

  private boolean iterate() {
    // get the lowest-weight state in the queue
    u = pq.extract_min();

    // check that this state has not been dominated
    // and mark vertex as visited
    if (!spt.visit(u)) {
      // state has been dominated since it was added to the priority queue, so it is
      // not in any optimal path. drop it on the floor and try the next one.
      return false;
    }

    traverseVisitor.visitVertex(u);

    nVisited += 1;

    Vertex u_vertex = u.getVertex();

    Edge[] edges = arriveBy ? u_vertex.getIncomingRaw() : u_vertex.getOutgoingRaw();
    for (Edge edge : edges) {
      if (skipEdgeStrategy.shouldSkipEdge(u, edge)) {
        continue;
      }

      // Iterate over traversal results. When an edge leads nowhere (as indicated by
      // returning an empty array), the iteration is over.
      var states = edge.traverse(u);
      for (var v : states) {
        // Could be: for (State v : traverseEdge...)

        traverseVisitor.visitEdge(edge);

        double remaining_w = heuristic.estimateRemainingWeight(v);

        if (remaining_w < 0 || Double.isInfinite(remaining_w)) {
          continue;
        }
        double estimate = v.getWeight() + remaining_w;

        // spt.add returns true if the state is hopeful; enqueue state if it's hopeful
        if (spt.add(v)) {
          traverseVisitor.visitEnqueue();
          pq.insert(v, estimate);
        }
      }
    }

    return true;
  }

  private void runSearch() {
    // execute the hook before the search begins so that it can be checked if the request
    // has already timed out.
    preSearchHook.run();
    long abortTime = DateUtils.absoluteTimeout(timeout);

    /* the core of the A* algorithm */
    while (!pq.empty()) {
      /*
       * Terminate based on timeout. We don't check the termination on every round, as it is
       * expensive to fetch the current time, compared to just running one more round.
       */
      if (timeout != null && nVisited % 100 == 0 && System.currentTimeMillis() > abortTime) {
        LOG.warn("Search timeout. origin={} target={}", fromVertices, toVertices);
        break;
      }

      /*
       * Get next best state and, if it hasn't already been dominated, add adjacent states to queue.
       * If it has been dominated, the iteration is over; don't bother checking for termination condition.
       *
       * Note that termination is checked after adjacent states are added. This presents the negligible inefficiency
       * that adjacent states are generated for a state which could be the last one you need to check. The advantage
       * of this is that the algorithm is always left in a restartable state, which is useful for debugging or
       * potential future variations.
       */
      if (!iterate()) {
        continue;
      }

      if (terminationStrategy != null) {
        if (terminationStrategy.shouldSearchTerminate(u)) {
          break;
        }
      }
      if (toVertices != null && toVertices.contains(u.getVertex()) && u.isFinal()) {
        targetAcceptedStates.add(u);

        // Break out of the search if we've found the requested number of paths.
        // Currently,  we can only find one path per search.
        LOG.debug("total vertices visited {}", nVisited);
        break;
      }
    }
  }
}
