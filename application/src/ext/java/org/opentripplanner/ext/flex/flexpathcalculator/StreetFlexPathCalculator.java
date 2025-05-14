package org.opentripplanner.ext.flex.flexpathcalculator;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;

/**
 * StreetFlexPathCalculator calculates the driving times and distances based on the street network
 * using the AStar algorithm.
 * <p>
 * Note that it caches the whole ShortestPathTree the first time it encounters a new fromVertex.
 * Subsequent requests from the same fromVertex can fetch the path to the toVertex from the existing
 * ShortestPathTree. This one-to-many approach is needed to make the performance acceptable.
 * <p>
 * Because we will have lots of searches with the same origin when doing access searches and a lot
 * of searches with the same destination when doing egress searches, the calculator needs to be
 * configured so that the caching is done with either the origin or destination vertex as the key.
 * The one-to-many search will then either be done in the forward or the reverse direction depending
 * on this configuration.
 */
public class StreetFlexPathCalculator implements FlexPathCalculator {

  private final Map<Vertex, ShortestPathTree<State, Edge, Vertex>> cache = new HashMap<>();
  private final boolean reverseDirection;
  private final Duration maxFlexTripDuration;

  public StreetFlexPathCalculator(boolean reverseDirection, Duration maxFlexTripDuration) {
    this.reverseDirection = reverseDirection;
    this.maxFlexTripDuration = maxFlexTripDuration;
  }

  @Override
  public FlexPath calculateFlexPath(
    Vertex fromv,
    Vertex tov,
    int boardStopPosition,
    int alightStopPosition
  ) {
    // These are the origin and destination vertices from the perspective of the one-to-many search,
    // which may be reversed
    Vertex originVertex = reverseDirection ? tov : fromv;
    Vertex destinationVertex = reverseDirection ? fromv : tov;

    ShortestPathTree<State, Edge, Vertex> shortestPathTree;
    if (cache.containsKey(originVertex)) {
      shortestPathTree = cache.get(originVertex);
    } else {
      shortestPathTree = routeToMany(originVertex);
      cache.put(originVertex, shortestPathTree);
    }

    GraphPath<State, Edge, Vertex> path = shortestPathTree.getPath(destinationVertex);
    if (path == null) {
      return null;
    }

    int distance = (int) path.edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
    int duration = path.getDuration();

    // computing the linestring from the graph path is a surprisingly expensive operation
    // so we delay it until it's actually needed. since most flex paths are never shown to the user
    // this improves performance quite a bit.
    return new FlexPath(distance, duration, () ->
      GeometryUtils.concatenateLineStrings(path.edges, Edge::getGeometry)
    );
  }

  private ShortestPathTree<State, Edge, Vertex> routeToMany(Vertex vertex) {
    // TODO: This is incorrect, the configured defaults are not used.
    var routingRequest = RouteRequest.of().withArriveBy(reverseDirection).buildDefault();

    return StreetSearchBuilder.of()
      .setSkipEdgeStrategy(new DurationSkipEdgeStrategy<>(maxFlexTripDuration))
      .setDominanceFunction(new DominanceFunctions.EarliestArrival())
      .setRequest(routingRequest)
      .setStreetRequest(new StreetRequest(StreetMode.CAR))
      .setFrom(reverseDirection ? null : vertex)
      .setTo(reverseDirection ? vertex : null)
      .getShortestPathTree();
  }
}
