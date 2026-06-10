package org.opentripplanner.astar;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
import org.opentripplanner.astar.spi.StatisticsCallback;
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

  private final boolean arriveBy;
  private final Set<Vertex> initialVertices;

  @Nullable
  private final Set<Vertex> goalVertices;

  @Nullable
  private final RemainingWeightHeuristic<State> heuristic;

  private final Runnable preSearchHook;

  @Nullable
  private final SkipEdgeStrategy<State, Edge> skipEdgeStrategy;

  @Nullable
  private final SearchTerminationStrategy<State> terminationStrategy;

  @Nullable
  private final TraverseVisitor<State, Edge> traverseVisitor;

  private final StatisticsCallback<Vertex> statisticsCallback;
  private final Duration timeout;

  private final ShortestPathTree<State, Edge, Vertex> spt;
  private final BinHeap<State> pq;
  private final List<State> targetAcceptedStates;

  private State u;
  private int nVisited;

  /// Create an AStar search
  /// @param initialStates The initial states to start the search from.
  /// @param arriveBy If set to true we will do a backwards search by traversing the incoming edges from each vertex.
  /// @param dominanceFunction A dominance function that determines which states we should keep during the search.
  /// @param goalVertices The search stops once the first goal vertex is reached.
  /// @param heuristic An astar heuristic that estimates a lower bound of the weight to the destination. If set to null the search will be a basic Dijkstra search.
  /// @param timeout A timeout that exits the search.
  /// @param preSearchHook A runnable that is run before the search starts.
  /// @param statisticsCallback A pluggable callback for logging metrics.
  AStar(
    Collection<State> initialStates,
    boolean arriveBy,
    DominanceFunction<State> dominanceFunction,
    @Nullable Set<Vertex> goalVertices,
    @Nullable RemainingWeightHeuristic<State> heuristic,
    @Nullable TraverseVisitor<State, Edge> traverseVisitor,
    @Nullable SkipEdgeStrategy<State, Edge> skipEdgeStrategy,
    @Nullable SearchTerminationStrategy<State> terminationStrategy,
    Duration timeout,
    Runnable preSearchHook,
    StatisticsCallback<Vertex> statisticsCallback
  ) {
    this.heuristic = heuristic;
    this.skipEdgeStrategy = skipEdgeStrategy;
    this.traverseVisitor = traverseVisitor;
    this.initialVertices = initialStates
      .stream()
      .map(AStarState::getVertex)
      .collect(Collectors.toSet());
    this.goalVertices = goalVertices;
    this.arriveBy = arriveBy;
    this.terminationStrategy = terminationStrategy;
    this.timeout = Objects.requireNonNull(timeout);

    this.spt = new ShortestPathTree<>(Objects.requireNonNull(dominanceFunction));

    this.preSearchHook = Objects.requireNonNull(preSearchHook);
    this.statisticsCallback = Objects.requireNonNull(statisticsCallback);

    // Initialized with a reasonable size, see #4445
    this.pq = new BinHeap<>(1000);
    this.nVisited = 0;
    this.targetAcceptedStates = new ArrayList<>();

    for (State initialState : initialStates) {
      spt.add(initialState);
      pq.insert(initialState, initialState.getWeight());
    }
  }

  public ShortestPathTree<State, Edge, Vertex> getShortestPathTree() {
    runSearch();

    return spt;
  }

  public List<GraphPath<State, Edge, Vertex>> getPathsToTarget() {
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

    if (traverseVisitor != null) {
      traverseVisitor.visitVertex(u);
    }

    nVisited += 1;

    Vertex u_vertex = u.getVertex();

    Collection<Edge> edges = arriveBy ? u_vertex.getIncoming() : u_vertex.getOutgoing();
    for (Edge edge : edges) {
      if (skipEdgeStrategy != null && skipEdgeStrategy.shouldSkipEdge(u, edge)) {
        continue;
      }

      // Iterate over traversal results. When an edge leads nowhere (as indicated by
      // returning an empty array), the iteration is over.
      var states = edge.traverse(u);
      for (var v : states) {
        // Could be: for (State v : traverseEdge...)

        if (traverseVisitor != null) {
          traverseVisitor.visitEdge(edge);
        }

        double remaining_w = heuristic != null ? heuristic.estimateRemainingWeight(v) : 0;

        if (remaining_w < 0 || Double.isInfinite(remaining_w)) {
          continue;
        }
        double estimate = v.getWeight() + remaining_w;

        // spt.add returns true if the state is hopeful; enqueue state if it's hopeful
        if (spt.add(v)) {
          // report to the visitor if there is one
          if (traverseVisitor != null) {
            traverseVisitor.visitEnqueue();
          }
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
    statisticsCallback.searchStarted();
    long abortTime = DateUtils.absoluteTimeout(timeout);

    /* the core of the A* algorithm */
    while (!pq.empty()) {
      /*
       * Terminate based on timeout. We don't check the termination on every round, as it is
       * expensive to fetch the current time, compared to just running one more round.
       */
      if (nVisited % 128 == 0 && System.currentTimeMillis() > abortTime) {
        LOG.warn("Search timeout. origin={} target={}", initialVertices, goalVertices);
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
      if (goalVertices != null && goalVertices.contains(u.getVertex()) && u.isFinal()) {
        targetAcceptedStates.add(u);

        // Break out of the search if we've found the requested number of paths.
        // Currently, we can only find one path per search.
        break;
      }
    }

    statisticsCallback.searchFinished(initialVertices, goalVertices, nVisited);
  }
}
