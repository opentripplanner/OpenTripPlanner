package org.opentripplanner.routing.graphfinder;

import static java.lang.Integer.min;

import java.util.Comparator;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.LinkingContextRequest;
import org.opentripplanner.routing.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.streetadapter.StreetSearchBuilder;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.service.TransitService;

/**
 * A GraphFinder which uses the street network to traverse the graph in order to find the nearest
 * stops and/or places from the origin.
 */
public class StreetGraphFinder implements GraphFinder {

  private final LinkingContextFactory linkingContextFactory;
  private final StopResolver stopResolver;

  public StreetGraphFinder(LinkingContextFactory linkingContextFactory, StopResolver stopResolver) {
    this.linkingContextFactory = linkingContextFactory;
    this.stopResolver = stopResolver;
  }

  @Override
  public List<NearbyStop> findClosestStops(Coordinate coordinate, double radiusMeters) {
    StopFinderTraverseVisitor visitor = new StopFinderTraverseVisitor(stopResolver, radiusMeters);
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
    // RR dateTime defaults to currentTime.
    // If elapsed time is not capped, searches are very slow.
    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var from = GenericLocation.fromCoordinate(lat, lon);
      var linkingRequest = LinkingContextRequest.of()
        .withFrom(from)
        .withDirectMode(StreetMode.WALK)
        .build();
      var linkerContext = linkingContextFactory.create(temporaryVerticesContainer, linkingRequest);
      // Make a normal OTP routing request so we can traverse edges and use GenericAStar
      // TODO make a function that builds normal routing requests from profile requests
      // TODO: This is incorrect, the configured defaults are not used.
      var request = RouteRequest.of()
        .withPreferences(pref -> pref.withWalk(it -> it.withSpeed(1)))
        .withNumItineraries(1)
        .buildDefault();
      StreetSearchBuilder.of()
        .withPreStartHook(OTPRequestTimeoutException::checkForTimeout)
        .withSkipEdgeStrategy(skipEdgeStrategy)
        .withTraverseVisitor(visitor)
        .withDominanceFunction(new DominanceFunctions.LeastWalk())
        .withRequest(request)
        .withFrom(linkerContext.findVertices(from))
        .getShortestPathTree();
    }
  }
}
