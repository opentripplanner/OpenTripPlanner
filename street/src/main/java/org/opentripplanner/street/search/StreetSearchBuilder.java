package org.opentripplanner.street.search;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.astar.AStar;
import org.opentripplanner.astar.AStarBuilder;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.spi.DominanceFunction;
import org.opentripplanner.astar.spi.RemainingWeightHeuristic;
import org.opentripplanner.astar.spi.SearchTerminationStrategy;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.astar.strategy.PathComparator;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;

public class StreetSearchBuilder {

  private final AStarBuilder<State, Edge, Vertex> aStarBuilder;
  private StreetSearchRequest request;
  private Set<Vertex> fromVertices;
  private Set<Vertex> toVertices;
  private RemainingWeightHeuristic<State> heuristic;

  public static StreetSearchBuilder of() {
    return new StreetSearchBuilder();
  }

  private StreetSearchBuilder() {
    aStarBuilder = new AStarBuilder<State, Edge, Vertex>().withDominanceFunction(
      new DominanceFunctions.Pareto()
    );
  }

  public StreetSearchBuilder withRequest(StreetSearchRequest request) {
    this.request = request;
    aStarBuilder.withTimeout(request.timeout());
    aStarBuilder.withArriveBy(request.arriveBy());
    return this;
  }

  public StreetSearchBuilder withFrom(Set<Vertex> fromVertices) {
    this.fromVertices = fromVertices;
    return this;
  }

  public StreetSearchBuilder withFrom(Vertex fromVertex) {
    fromVertices = Collections.singleton(fromVertex);
    return this;
  }

  public StreetSearchBuilder withTo(Set<Vertex> toVertices) {
    this.toVertices = toVertices;
    return this;
  }

  public StreetSearchBuilder withTo(Vertex toVertex) {
    toVertices = Collections.singleton(toVertex);
    return this;
  }

  public StreetSearchBuilder withHeuristic(RemainingWeightHeuristic<State> heuristic) {
    this.heuristic = heuristic;
    return this;
  }

  public StreetSearchBuilder withDominanceFunction(DominanceFunction<State> dominanceFunction) {
    aStarBuilder.withDominanceFunction(dominanceFunction);
    return this;
  }

  public StreetSearchBuilder withSkipEdgeStrategy(SkipEdgeStrategy<State, Edge> skipEdgeStrategy) {
    aStarBuilder.withSkipEdgeStrategy(skipEdgeStrategy);
    return this;
  }

  public StreetSearchBuilder withTraverseVisitor(TraverseVisitor<State, Edge> traverseVisitor) {
    aStarBuilder.withTraverseVisitor(traverseVisitor);
    return this;
  }

  public StreetSearchBuilder withTerminationStrategy(
    SearchTerminationStrategy<State> terminationStrategy
  ) {
    aStarBuilder.withTerminationStrategy(terminationStrategy);
    return this;
  }

  /**
   * Set a function that will be called before the search begins. Useful for checking that
   * a timeout has not been reached before the search begins.
   */
  public StreetSearchBuilder withPreStartHook(Runnable hook) {
    aStarBuilder.withPreStartHook(hook);
    return this;
  }

  /// Run the street search, returning nothing
  public void run() {
    buildAstar().getShortestPathTree();
  }

  /// Run the search returning the shortestPathTree
  public ShortestPathTree<State, Edge, Vertex> getShortestPathTree() {
    return buildAstar().getShortestPathTree();
  }

  /// Run the street search, returning all paths found
  public List<StreetPath> getPathsToTarget() {
    return buildAstar()
      .getPathsToTarget()
      .stream()
      .sorted(new PathComparator(request.arriveBy()))
      .map(StreetPath::new)
      .toList();
  }

  private AStar<State, Edge, Vertex> buildAstar() {
    Objects.requireNonNull(request);
    var arriveBy = request.arriveBy();
    var originVertices = arriveBy ? toVertices : fromVertices;
    var goalVertices = arriveBy ? fromVertices : toVertices;
    var initialStates = State.getInitialStates(originVertices, request);
    var heuristic = initializedHeuristic(goalVertices);

    return aStarBuilder
      .withGoalVertices(goalVertices)
      .withInitialStates(initialStates)
      .withHeuristic(heuristic)
      .build();
  }

  @Nullable
  private RemainingWeightHeuristic<State> initializedHeuristic(Set<Vertex> goalVertices) {
    if (heuristic instanceof EuclideanRemainingWeightHeuristic euclideanHeuristic) {
      euclideanHeuristic.initialize(goalVertices, request);
    } else if (heuristic != null) {
      throw new IllegalArgumentException("Unknown heuristic type: " + heuristic);
    }
    return heuristic;
  }
}
