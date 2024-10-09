package org.opentripplanner.routing.graphfinder;

import static java.lang.Integer.min;

import java.util.Comparator;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitService;

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
  public List<NearbyStop> findClosestStops(Coordinate coordinate, double radiusMeters) {
    StopFinderTraverseVisitor visitor = new StopFinderTraverseVisitor(radiusMeters);
    findClosestUsingStreets(
      coordinate.getY(),
      coordinate.getX(),
      visitor,
      visitor.getSkipEdgeStrategy()
    );
    return visitor.stopsFound();
  }

  @Override
  public List<PlaceAtDistance> findClosestPlaces(
    double lat,
    double lon,
    double radiusMeters,
    int maxResults,
    List<TransitMode> filterByModes,
    List<PlaceType> filterByPlaceTypes,
    List<FeedScopedId> filterByStops,
    List<FeedScopedId> filterByStations,
    List<FeedScopedId> filterByRoutes,
    List<String> filterByBikeRentalStations,
    List<String> filterByNetwork,
    TransitService transitService
  ) {
    PlaceFinderTraverseVisitor visitor = new PlaceFinderTraverseVisitor(
      transitService,
      filterByModes,
      filterByPlaceTypes,
      filterByStops,
      filterByStations,
      filterByRoutes,
      filterByBikeRentalStations,
      filterByNetwork,
      maxResults,
      radiusMeters
    );
    SkipEdgeStrategy<State, Edge> terminationStrategy = visitor.getSkipEdgeStrategy();
    findClosestUsingStreets(lat, lon, visitor, terminationStrategy);
    List<PlaceAtDistance> results = visitor.placesFound;
    results.sort(Comparator.comparingDouble(PlaceAtDistance::distance));
    return results.subList(0, min(results.size(), maxResults));
  }

  private void findClosestUsingStreets(
    double lat,
    double lon,
    TraverseVisitor<State, Edge> visitor,
    SkipEdgeStrategy<State, Edge> skipEdgeStrategy
  ) {
    // Make a normal OTP routing request so we can traverse edges and use GenericAStar
    // TODO make a function that builds normal routing requests from profile requests
    RouteRequest rr = new RouteRequest();
    rr.setFrom(new GenericLocation(null, null, lat, lon));
    rr.withPreferences(pref -> pref.withWalk(it -> it.withSpeed(1)));
    rr.setNumItineraries(1);
    // RR dateTime defaults to currentTime.
    // If elapsed time is not capped, searches are very slow.
    try (
      var temporaryVertices = new TemporaryVerticesContainer(
        graph,
        rr.from(),
        rr.to(),
        StreetMode.WALK,
        StreetMode.WALK
      )
    ) {
      StreetSearchBuilder
        .of()
        .setSkipEdgeStrategy(skipEdgeStrategy)
        .setTraverseVisitor(visitor)
        .setDominanceFunction(new DominanceFunctions.LeastWalk())
        .setRequest(rr)
        .setVerticesContainer(temporaryVertices)
        .getShortestPathTree();
    }
  }
}
