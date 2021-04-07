package org.opentripplanner.ext.flex.flexpathcalculator;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.util.HashMap;
import java.util.Map;

/**
 * StreetFlexPathCalculator calculates the driving times and distances based on the street network
 * using the AStar algorithm.
 *
 * TODO: - Mast fast enough to be usable
 *       - Use a one-to-many search
 *       - Cache found times
 */
public class StreetFlexPathCalculator implements FlexPathCalculator {
  private Graph graph;
  private Map<T2<Vertex, Vertex>, FlexPath> cache = new HashMap<>();

  public StreetFlexPathCalculator(Graph graph) {
    this.graph = graph;
  }

  @Override
  public FlexPath calculateFlexPath(Vertex fromv, Vertex tov, int fromStopIndex, int toStopIndex) {
    T2<Vertex, Vertex> key = new T2<>(fromv, tov);
    FlexPath cacheValue = cache.get(key);
    if (cacheValue != null) { return cacheValue; }

    RoutingRequest routingRequest = new RoutingRequest(TraverseMode.CAR);
    routingRequest.setNumItineraries(1);

    routingRequest.setRoutingContext(graph, fromv, tov);
    routingRequest.dominanceFunction = new DominanceFunction.EarliestArrival();
    AStar search = new AStar();
    ShortestPathTree spt = search.getShortestPathTree(routingRequest);

    GraphPath path = spt.getPaths().get(0);

    if (path == null) { return null; }

    int distance = (int) path.edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
    int duration = path.getDuration();

    routingRequest.cleanup();

    FlexPath value = new FlexPath(distance, duration);
    cache.put(key, value);
    return value;
  }
}
