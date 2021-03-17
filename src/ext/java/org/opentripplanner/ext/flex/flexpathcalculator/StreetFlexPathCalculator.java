package org.opentripplanner.ext.flex.flexpathcalculator;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.algorithm.astar.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.SplitterVertex;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * StreetFlexPathCalculator calculates the driving times and distances based on the street network
 * using the AStar algorithm.
 *
 * Note that it caches the whole ShortestPathTree the first time it encounters a new fromVertex.
 * Subsequents requests from the same fromVertex can fetch the path to the toVertex from the
 * existing ShortestPathTree. This one-to-many approach is need to make the performance acceptable.
 */
public class StreetFlexPathCalculator implements FlexPathCalculator {

  // This is a limit to the max length of a flex trip
  private static final int SEARCH_RADIUS_METERS = 20_000;

  private Graph graph;
  private Map<Vertex, ShortestPathTree> cache = new HashMap<>();

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

    GraphPath path = null;
    for (Vertex v : getAllSplitterVerticesOutgoing(tov)) {
      path = shortestPathTree.getPath(v, false);
      if (path != null) break;
    }
    if (path == null) {
      return null;
    }

    int distance = (int) path.getDistanceMeters();
    int duration = path.getDuration();
    LineString geometry = path.getGeometry();

    return new FlexPath(distance, duration, geometry);
  }

  private ShortestPathTree routeToMany(Vertex fromv) {
    Set<Vertex> fromVertices = getAllSplitterVerticesIncoming(fromv);
    RoutingRequest routingRequest = new RoutingRequest(TraverseMode.CAR);
    routingRequest.setRoutingContext(graph, fromVertices, null);
    int driveTime = (int) (SEARCH_RADIUS_METERS / new RoutingRequest().carSpeed);
    routingRequest.worstTime = routingRequest.dateTime + driveTime;
    routingRequest.disableRemainingWeightHeuristic = true;
    routingRequest.rctx.remainingWeightHeuristic = new TrivialRemainingWeightHeuristic();
    routingRequest.dominanceFunction = new DominanceFunction.EarliestArrival();
    routingRequest.oneToMany = true;
    AStar search = new AStar();
    ShortestPathTree spt = search.getShortestPathTree(routingRequest);
    routingRequest.cleanup();
    return spt;
  }

  /**
   * In the case of routing from one SplitterVertex it is important to have all of them. Otherwise
   * the StreetEdge backtracking check will fail and we will only be able to travel in one direction
   * from the split StreetEdge.
   */
  private Set<Vertex> getAllSplitterVerticesIncoming(Vertex vertex) {
    if (vertex instanceof SplitterVertex) {
      TemporaryFreeEdge temporaryFreeEdge = (TemporaryFreeEdge) vertex
          .getIncoming()
          .iterator()
          .next();
      TemporaryStreetLocation temporaryStreetLocation =
          (TemporaryStreetLocation) temporaryFreeEdge.getFromVertex();
      return temporaryStreetLocation
          .getOutgoing()
          .stream()
          .map(Edge::getToVertex)
          .collect(Collectors.toSet());
    }
    else {
      return Set.of(vertex);
    }
  }

  private Set<Vertex> getAllSplitterVerticesOutgoing(Vertex vertex) {
    if (vertex instanceof SplitterVertex) {
      TemporaryFreeEdge temporaryFreeEdge = (TemporaryFreeEdge) vertex
          .getOutgoing()
          .iterator()
          .next();
      TemporaryStreetLocation temporaryStreetLocation =
          (TemporaryStreetLocation) temporaryFreeEdge.getToVertex();
      return temporaryStreetLocation
          .getIncoming()
          .stream()
          .map(Edge::getToVertex)
          .collect(Collectors.toSet());
    }
    else {
      return Set.of(vertex);
    }
  }
}
