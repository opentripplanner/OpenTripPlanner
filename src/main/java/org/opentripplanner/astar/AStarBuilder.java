package org.opentripplanner.astar;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.astar.spi.AStarEdge;
import org.opentripplanner.astar.spi.AStarState;
import org.opentripplanner.astar.spi.AStarVertex;
import org.opentripplanner.astar.spi.RemainingWeightHeuristic;
import org.opentripplanner.astar.spi.SearchTerminationStrategy;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.routing.algorithm.astar.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.core.AStarRequest;
import org.opentripplanner.routing.core.AStarRequestMapper;
import org.opentripplanner.routing.core.TemporaryVerticesContainer;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCalculator;

public class AStarBuilder<
  State extends AStarState<State, Edge, Vertex>,
  Edge extends AStarEdge<State, Edge, Vertex>,
  Vertex extends AStarVertex<State, Edge, Vertex>
> {

  private RemainingWeightHeuristic<State, Vertex> heuristic = new TrivialRemainingWeightHeuristic<>();
  private SkipEdgeStrategy<State, Edge> skipEdgeStrategy;
  private TraverseVisitor<State, Edge> traverseVisitor;
  private RouteRequest routeRequest;
  private Set<Vertex> fromVertices;
  private Set<Vertex> toVertices;
  private SearchTerminationStrategy<State> terminationStrategy;
  private DominanceFunction<State> dominanceFunction;
  private Duration timeout;
  private Edge originBackEdge;
  private Collection<State> initialStates;
  private IntersectionTraversalCalculator intersectionTraversalCalculator;
  private DataOverlayContext dataOverlayContext;
  private StreetRequest streetRequest = new StreetRequest();

  AStarBuilder() {}

  public AStarBuilder<State, Edge, Vertex> setHeuristic(
    RemainingWeightHeuristic<State, Vertex> heuristic
  ) {
    this.heuristic = heuristic;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> setSkipEdgeStrategy(
    SkipEdgeStrategy<State, Edge> skipEdgeStrategy
  ) {
    this.skipEdgeStrategy = skipEdgeStrategy;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> setTraverseVisitor(
    TraverseVisitor<State, Edge> traverseVisitor
  ) {
    this.traverseVisitor = traverseVisitor;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> setRequest(RouteRequest request) {
    this.routeRequest = request;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> setStreetRequest(StreetRequest streetRequest) {
    this.streetRequest = streetRequest;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> setFrom(Set<Vertex> fromVertices) {
    this.fromVertices = fromVertices;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> setFrom(Vertex fromVertex) {
    this.fromVertices = Collections.singleton(fromVertex);
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> setTo(Set<Vertex> toVertices) {
    this.toVertices = toVertices;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> setTo(Vertex toVertex) {
    this.toVertices = Collections.singleton(toVertex);
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> setVerticesContainer(
    TemporaryVerticesContainer container
  ) {
    this.fromVertices = (Set<Vertex>) container.getFromVertices();
    this.toVertices = (Set<Vertex>) container.getToVertices();
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> setTerminationStrategy(
    SearchTerminationStrategy<State> terminationStrategy
  ) {
    this.terminationStrategy = terminationStrategy;
    return this;
  }

  /** The function that compares paths converging on the same vertex to decide which ones continue to be explored. */
  public AStarBuilder<State, Edge, Vertex> setDominanceFunction(
    DominanceFunction<State> dominanceFunction
  ) {
    this.dominanceFunction = dominanceFunction;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> setTimeout(Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> setIntersectionTraversalCalculator(
    IntersectionTraversalCalculator intersectionTraversalCalculator
  ) {
    this.intersectionTraversalCalculator = intersectionTraversalCalculator;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> setDataOverlayContext(
    DataOverlayContext dataOverlayContext
  ) {
    this.dataOverlayContext = dataOverlayContext;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> setOriginBackEdge(Edge originBackEdge) {
    this.originBackEdge = originBackEdge;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> setInitialStates(Collection<State> initialStates) {
    this.initialStates = initialStates;
    return this;
  }

  public ShortestPathTree<State, Edge, Vertex> getShortestPathTree() {
    return build().getShortestPathTree();
  }

  public List<GraphPath<State, Edge, Vertex>> getPathsToTarget() {
    return build().getPathsToTarget();
  }

  private AStar<State, Edge, Vertex> build() {
    final Set<Vertex> origin = routeRequest.arriveBy() ? toVertices : fromVertices;
    final Set<Vertex> destination = routeRequest.arriveBy() ? fromVertices : toVertices;

    Collection<State> initialStates;

    if (this.initialStates != null) {
      initialStates = this.initialStates;
    } else {
      AStarRequest aStarRequest = AStarRequestMapper
        .map(routeRequest)
        .withMode(streetRequest.mode())
        .build();

      initialStates =
        (Collection<State>) org.opentripplanner.routing.core.State.getInitialStates(
          (Set<org.opentripplanner.street.model.vertex.Vertex>) origin,
          aStarRequest
        );

      if (originBackEdge != null) {
        for (var state : initialStates) {
          state.initBackEdge(originBackEdge);
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

    heuristic.initialize(routeRequest, streetRequest.mode(), origin, destination);

    return new AStar<>(
      heuristic,
      skipEdgeStrategy,
      traverseVisitor,
      routeRequest.arriveBy(),
      origin,
      destination,
      terminationStrategy,
      Optional
        .ofNullable(dominanceFunction)
        .orElseGet(() -> (DominanceFunction<State>) new DominanceFunctions.Pareto()),
      timeout,
      initialStates
    );
  }
}
