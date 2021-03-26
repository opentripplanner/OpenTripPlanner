package org.opentripplanner.ext.flex.flexpathcalculator;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.algorithm.astar.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * StreetFlexPathCalculator calculates the driving times and distances based on the street network
 * using the AStar algorithm.
 *
 * Note that it caches the whole ShortestPathTree the first time it encounters a new fromVertex.
 * Subsequents requests from the same fromVertex can fetch the path to the toVertex from the
 * existing ShortestPathTree. This one-to-many approach is needed to make the performance acceptable.
 */
public class StreetFlexPathCalculator implements FlexPathCalculator {

  private static final long MAX_FLEX_TRIP_DURATION_SECONDS = Duration.ofMinutes(45).toSeconds();

  private final Graph graph;
  private final Map<Vertex, ShortestPathTree> cache = new HashMap<>();

  public StreetFlexPathCalculator(Graph graph) {
    this.graph = graph;
  }

  @Override
  public FlexPath calculateFlexPath(Vertex fromv, Vertex tov, int fromStopIndex, int toStopIndex) {

    ShortestPathTree shortestPathTree;
    if (cache.containsKey(fromv)) {
      shortestPathTree = cache.get(fromv);
    } else {
      shortestPathTree = routeToMany(fromv);
      cache.put(fromv, shortestPathTree);
    }

    GraphPath path = shortestPathTree.getPath(tov, false);
    if (path == null) { return null; }

    int distance = (int) path.getDistanceMeters();
    int duration = path.getDuration();
    LineString geometry = path.getGeometry();

    return new FlexPath(distance, duration, geometry);
  }

  private ShortestPathTree routeToMany(Vertex fromv) {
    RoutingRequest routingRequest = new RoutingRequest(TraverseMode.CAR);
    routingRequest.setRoutingContext(graph, fromv, null);
    routingRequest.worstTime = routingRequest.dateTime + MAX_FLEX_TRIP_DURATION_SECONDS;
    routingRequest.disableRemainingWeightHeuristic = true;
    routingRequest.rctx.remainingWeightHeuristic = new TrivialRemainingWeightHeuristic();
    routingRequest.dominanceFunction = new DominanceFunction.EarliestArrival();
    routingRequest.oneToMany = true;
    AStar search = new AStar();
    ShortestPathTree spt = search.getShortestPathTree(routingRequest);
    routingRequest.cleanup();
    return spt;
  }
}
