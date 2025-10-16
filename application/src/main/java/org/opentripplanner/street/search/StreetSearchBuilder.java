package org.opentripplanner.street.search;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.opentripplanner.astar.AStarBuilder;
import org.opentripplanner.astar.spi.DominanceFunction;
import org.opentripplanner.astar.spi.RemainingWeightHeuristic;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ExtensionRequestContext;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.intersection_model.IntersectionTraversalCalculator;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestMapper;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;

public class StreetSearchBuilder extends AStarBuilder<State, Edge, Vertex, StreetSearchBuilder> {

  private RouteRequest routeRequest;
  private StreetRequest streetRequest = StreetRequest.DEFAULT;
  private IntersectionTraversalCalculator intersectionTraversalCalculator;
  private List<ExtensionRequestContext> extensionRequestContexts = List.of();

  public static StreetSearchBuilder of() {
    return new StreetSearchBuilder();
  }

  private StreetSearchBuilder() {
    super();
    setBuilder(this);
  }

  public StreetSearchBuilder withRequest(RouteRequest request) {
    this.routeRequest = request;
    withArriveBy(request.arriveBy());
    return this;
  }

  public StreetSearchBuilder withStreetRequest(StreetRequest streetRequest) {
    this.streetRequest = streetRequest;
    return this;
  }

  public StreetSearchBuilder withVerticesContainer(TemporaryVerticesContainer container) {
    withFrom(container.getFromVertices());
    withTo(container.getToVertices());
    return this;
  }

  public StreetSearchBuilder withIntersectionTraversalCalculator(
    IntersectionTraversalCalculator intersectionTraversalCalculator
  ) {
    this.intersectionTraversalCalculator = intersectionTraversalCalculator;
    return this;
  }

  public StreetSearchBuilder withExtensionRequestContexts(
    Collection<ExtensionRequestContext> extensionRequestContexts
  ) {
    this.extensionRequestContexts = List.copyOf(extensionRequestContexts);
    return this;
  }

  @Override
  protected Duration streetRoutingTimeout() {
    return routeRequest.preferences().street().routingTimeout();
  }

  @Override
  protected Collection<State> createInitialStates(Set<Vertex> originVertices) {
    StreetSearchRequest streetSearchRequest = StreetSearchRequestMapper.map(routeRequest)
      .withMode(streetRequest.mode())
      .withArriveBy(arriveBy())
      .build();

    return State.getInitialStates(originVertices, streetSearchRequest);
  }

  @Override
  protected void prepareInitialStates(Collection<State> initialStates) {
    if (intersectionTraversalCalculator == null) {
      final StreetPreferences streetPreferences = routeRequest.preferences().street();
      intersectionTraversalCalculator = IntersectionTraversalCalculator.create(
        streetPreferences.intersectionTraversalModel(),
        streetPreferences.drivingDirection()
      );
    }

    for (var state : initialStates) {
      state.getRequest().setIntersectionTraversalCalculator(intersectionTraversalCalculator);
      state.getRequest().setExtensionRequestContexts(extensionRequestContexts);
    }
  }

  @Override
  protected void initializeHeuristic(
    RemainingWeightHeuristic<State> heuristic,
    Set<Vertex> ignored,
    Set<Vertex> destination,
    boolean arriveBy
  ) {
    if (heuristic.equals(RemainingWeightHeuristic.TRIVIAL)) {
      // No initialization needed
    } else if (heuristic instanceof EuclideanRemainingWeightHeuristic euclideanHeuristic) {
      euclideanHeuristic.initialize(
        streetRequest.mode(),
        destination,
        arriveBy,
        routeRequest.preferences()
      );
    } else {
      throw new IllegalArgumentException("Unknown heuristic type: " + heuristic);
    }
  }

  @Override
  protected DominanceFunction<State> createDefaultDominanceFunction() {
    return new DominanceFunctions.Pareto();
  }
}
