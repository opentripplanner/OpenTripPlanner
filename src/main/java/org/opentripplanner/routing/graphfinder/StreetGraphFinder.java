package org.opentripplanner.routing.graphfinder;

import static java.lang.Integer.min;

import java.util.Comparator;
import java.util.List;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.algorithm.astar.TraverseVisitor;
import org.opentripplanner.routing.algorithm.astar.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.algorithm.astar.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.DominanceFunction;

/**
 * A GraphFinder which uses the street network to traverse the graph in order to find the nearest
 * stops and/or places from the origin.
 */
public class StreetGraphFinder implements GraphFinder {

  private final Graph graph;

  public StreetGraphFinder(Graph graph) {
    this.graph = graph;
  }

  @Override
  public List<NearbyStop> findClosestStops(double lat, double lon, double radiusMeters) {
      StopFinderTraverseVisitor visitor = new StopFinderTraverseVisitor(radiusMeters);
      findClosestUsingStreets(lat, lon, visitor, visitor.getSkipEdgeStrategy());
      return visitor.stopsFound;
  }

  @Override
  public List<PlaceAtDistance> findClosestPlaces(
      double lat, double lon, double radiusMeters, int maxResults, List<TransitMode> filterByModes,
      List<PlaceType> filterByPlaceTypes, List<FeedScopedId> filterByStops,
      List<FeedScopedId> filterByRoutes, List<String> filterByBikeRentalStations,
      List<String> filterByBikeParks, List<String> filterByCarParks, RoutingService routingService
  ) {
      PlaceFinderTraverseVisitor visitor = new PlaceFinderTraverseVisitor(
          routingService,
          filterByModes,
          filterByPlaceTypes,
          filterByStops,
          filterByRoutes,
          filterByBikeRentalStations,
          maxResults,
          radiusMeters
      );
      SkipEdgeStrategy terminationStrategy = visitor.getSkipEdgeStrategy();
      findClosestUsingStreets(lat, lon, visitor, terminationStrategy);
      List<PlaceAtDistance> results = visitor.placesFound;
      results.sort(Comparator.comparingDouble(pad -> pad.distance));
      return results.subList(0, min(results.size(), maxResults));
  }

  private void findClosestUsingStreets(
      double lat, double lon, TraverseVisitor visitor, SkipEdgeStrategy skipEdgeStrategy
  ) {
    // Make a normal OTP routing request so we can traverse edges and use GenericAStar
    // TODO make a function that builds normal routing requests from profile requests
    try (RoutingRequest rr = new RoutingRequest(TraverseMode.WALK)) {
      rr.from = new GenericLocation(null, null, lat, lon);
      rr.oneToMany = true;
      rr.setRoutingContext(graph);
      rr.walkSpeed = 1;
      rr.dominanceFunction = new DominanceFunction.LeastWalk();
      rr.rctx.remainingWeightHeuristic = new TrivialRemainingWeightHeuristic();
      // RR dateTime defaults to currentTime.
      // If elapsed time is not capped, searches are very slow.
      AStar astar = new AStar();
      rr.setNumItineraries(1);
      astar.setTraverseVisitor(visitor);
      astar.setSkipEdgeStrategy(skipEdgeStrategy);
      astar.getShortestPathTree(rr);
      // Destroy the routing context, to clean up the temporary edges & vertices
      rr.rctx.destroy();
    }
  }
}
