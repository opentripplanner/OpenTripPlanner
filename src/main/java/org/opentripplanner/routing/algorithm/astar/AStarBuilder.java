package org.opentripplanner.routing.algorithm.astar;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.routing.algorithm.astar.strategies.DurationSkipEdgeStrategy;
import org.opentripplanner.routing.algorithm.astar.strategies.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.astar.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.astar.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.astar.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.algorithm.astar.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.core.AStarRequest;
import org.opentripplanner.routing.core.AStarRequestMapper;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TemporaryVerticesContainer;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCalculator;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

public class AStarBuilder {

  private final RemainingWeightHeuristic heuristic;
  private final SkipEdgeStrategy skipEdgeStrategy;
  private TraverseVisitor traverseVisitor;
  private RouteRequest routeRequest;
  private boolean arriveBy;
  private Set<Vertex> fromVertices;
  private Set<Vertex> toVertices;
  private SearchTerminationStrategy terminationStrategy;
  private DominanceFunction dominanceFunction;
  private Duration timeout;
  private Edge originBackEdge;
  private Collection<State> initialStates;
  private IntersectionTraversalCalculator intersectionTraversalCalculator;
  private DataOverlayContext dataOverlayContext;
  private StreetRequest streetRequest = new StreetRequest();

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

  public AStarBuilder setRequest(RouteRequest request) {
    this.routeRequest = request;
    this.arriveBy = request.arriveBy();
    return this;
  }

  public AStarBuilder setArriveBy(boolean arriveBy) {
    this.arriveBy = arriveBy;
    return this;
  }

  public AStarBuilder setStreetRequest(StreetRequest streetRequest) {
    this.streetRequest = streetRequest;
    return this;
  }

  public AStarBuilder setFrom(Set<Vertex> fromVertices) {
    this.fromVertices = fromVertices;
    return this;
  }

  public AStarBuilder setFrom(Vertex fromVertex) {
    this.fromVertices = Collections.singleton(fromVertex);
    return this;
  }

  public AStarBuilder setTo(Set<Vertex> toVertices) {
    this.toVertices = toVertices;
    return this;
  }

  public AStarBuilder setTo(Vertex toVertex) {
    this.toVertices = Collections.singleton(toVertex);
    return this;
  }

  public AStarBuilder setVerticesContainer(TemporaryVerticesContainer container) {
    this.fromVertices = container.getFromVertices();
    this.toVertices = container.getToVertices();
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

  public AStarBuilder setIntersectionTraversalCalculator(
    IntersectionTraversalCalculator intersectionTraversalCalculator
  ) {
    this.intersectionTraversalCalculator = intersectionTraversalCalculator;
    return this;
  }

  public AStarBuilder setDataOverlayContext(DataOverlayContext dataOverlayContext) {
    this.dataOverlayContext = dataOverlayContext;
    return this;
  }

  public AStarBuilder setOriginBackEdge(Edge originBackEdge) {
    this.originBackEdge = originBackEdge;
    return this;
  }

  public AStarBuilder setInitialStates(Collection<State> initialStates) {
    this.initialStates = initialStates;
    return this;
  }

  public ShortestPathTree getShortestPathTree() {
    return build().getShortestPathTree();
  }

  public List<GraphPath> getPathsToTarget() {
    return build().getPathsToTarget();
  }

  private AStar build() {
    final Set<Vertex> origin = arriveBy ? toVertices : fromVertices;
    final Set<Vertex> destination = arriveBy ? fromVertices : toVertices;

    Collection<State> initialStates;

    if (this.initialStates != null) {
      initialStates = this.initialStates;
    } else {
      AStarRequest aStarRequest = AStarRequestMapper
        .map(routeRequest)
        .withMode(streetRequest.mode())
        .withArriveBy(arriveBy)
        .build();

      initialStates = State.getInitialStates(origin, aStarRequest);

      if (originBackEdge != null) {
        for (var state : initialStates) {
          state.backEdge = originBackEdge;
        }
      }
    }

    if (intersectionTraversalCalculator == null) {
      final StreetPreferences streetPreferences = routeRequest.preferences().street();
      intersectionTraversalCalculator =
        IntersectionTraversalCalculator.create(
          streetPreferences.intersectionTraversalModel(),
          streetPreferences.drivingDirection()
        );
    }

    for (var state : initialStates) {
      state.getRequest().setIntersectionTraversalCalculator(intersectionTraversalCalculator);
      state.getRequest().setDataOverlayContext(dataOverlayContext);
    }

    heuristic.initialize(
      streetRequest.mode(),
      origin,
      destination,
      arriveBy,
      routeRequest.preferences()
    );

    return new AStar(
      heuristic,
      skipEdgeStrategy,
      traverseVisitor,
      arriveBy,
      origin,
      destination,
      terminationStrategy,
      Optional.ofNullable(dominanceFunction).orElseGet(DominanceFunction.Pareto::new),
      timeout,
      initialStates
    );
  }
}
