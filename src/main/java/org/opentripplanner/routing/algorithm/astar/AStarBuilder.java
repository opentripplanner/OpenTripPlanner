package org.opentripplanner.routing.algorithm.astar;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.routing.algorithm.astar.strategies.DurationSkipEdgeStrategy;
import org.opentripplanner.routing.algorithm.astar.strategies.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.astar.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.astar.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.astar.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.algorithm.astar.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

public class AStarBuilder {

  private final RemainingWeightHeuristic heuristic;
  private final SkipEdgeStrategy skipEdgeStrategy;
  private TraverseVisitor traverseVisitor;
  private RoutingContext routingContext;
  private SearchTerminationStrategy terminationStrategy;
  private DominanceFunction dominanceFunction;
  private Duration timeout;
  private Edge originBackEdge;

  public AStarBuilder(
    RemainingWeightHeuristic remainingWeightHeuristic,
    SkipEdgeStrategy strategy
  ) {
    this.heuristic = remainingWeightHeuristic;
    this.skipEdgeStrategy = strategy;
  }

  public static AStarBuilder oneToOne() {
    return new AStarBuilder(new EuclideanRemainingWeightHeuristic(), null);
  }

  public static AStarBuilder oneToOneMaxDuration(Duration maxDuration) {
    return new AStarBuilder(
      new EuclideanRemainingWeightHeuristic(),
      new DurationSkipEdgeStrategy(maxDuration)
    );
  }

  public static AStarBuilder allDirectionsMaxDuration(Duration maxDuration) {
    return allDirections(new DurationSkipEdgeStrategy(maxDuration));
  }

  public static AStarBuilder allDirections(SkipEdgeStrategy strategy) {
    return new AStarBuilder(new TrivialRemainingWeightHeuristic(), strategy);
  }

  public AStarBuilder setTraverseVisitor(TraverseVisitor traverseVisitor) {
    this.traverseVisitor = traverseVisitor;
    return this;
  }

  public AStarBuilder setContext(RoutingContext routingContext) {
    this.routingContext = routingContext;
    return this;
  }

  public AStarBuilder setTerminationStrategy(SearchTerminationStrategy terminationStrategy) {
    this.terminationStrategy = terminationStrategy;
    return this;
  }

  /** The function that compares paths converging on the same vertex to decide which ones continue to be explored. */
  public AStarBuilder setDominanceFunction(DominanceFunction dominanceFunction) {
    this.dominanceFunction = dominanceFunction;
    return this;
  }

  public AStarBuilder setTimeout(Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  public AStarBuilder setOriginBackEdge(Edge originBackEdge) {
    this.originBackEdge = originBackEdge;
    return this;
  }

  public ShortestPathTree getShortestPathTree() {
    return build().getShortestPathTree();
  }

  public List<GraphPath> getPathsToTarget() {
    return build().getPathsToTarget();
  }

  private AStar build() {
    return new AStar(
      heuristic,
      skipEdgeStrategy,
      traverseVisitor,
      routingContext,
      terminationStrategy,
      Optional.ofNullable(dominanceFunction).orElseGet(DominanceFunction.Pareto::new),
      timeout,
      originBackEdge
    );
  }
}
