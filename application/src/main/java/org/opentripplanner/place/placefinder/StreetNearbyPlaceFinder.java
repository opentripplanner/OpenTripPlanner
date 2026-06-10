package org.opentripplanner.place.placefinder;

import static java.lang.Integer.min;

import java.util.Comparator;
import java.util.List;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.place.NearbyPlaceFinder;
import org.opentripplanner.place.api.PlaceAtDistance;
import org.opentripplanner.place.api.PlaceType;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.LinkingContextRequest;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.service.TransitService;

/**
 * A nearby place finder which uses the street network to traverse the graph in order to find the
 * nearest places from the origin.
 */
public class StreetNearbyPlaceFinder implements NearbyPlaceFinder {

  private final LinkingContextFactory linkingContextFactory;

  public StreetNearbyPlaceFinder(LinkingContextFactory linkingContextFactory) {
    this.linkingContextFactory = linkingContextFactory;
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
      var request = StreetSearchRequest.of()
        .withWalk(it -> it.withSpeed(1))
        .build();
      StreetSearchBuilder.of()
        .withPreStartHook(OTPRequestTimeoutException::checkForTimeout)
        .withSkipEdgeStrategy(skipEdgeStrategy)
        .withTraverseVisitor(visitor)
        .withDominanceFunction(new DominanceFunctions.ShortestDistance())
        .withRequest(request)
        .withFrom(linkerContext.findVertices(from))
        .run();
    }
  }
}
