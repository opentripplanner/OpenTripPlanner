package org.opentripplanner.transfer.streetadapter;

import java.util.function.Predicate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.place.nearbystopfinder.StraightLineNearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.StreetNearbyStopFinder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferSearch;

/**
 * A {@link TransferPathProvider} backed by a street search, with {@code P = NearbyStop}.
 * <p>
 * The request is a {@link RouteRequest} with the user preferences applied, see
 * {@link RegularTransferPreferencesMapper}. {@link #searchFor} creates the
 * {@link DefaultTransferSearch} of a request, with the street search, once per request.
 */
public class StreetTransferPathProvider implements TransferPathProvider<NearbyStop, RouteRequest> {

  private final Graph graph;
  private final NearbyStopFinder nearbyStopFinder;
  private final Predicate<FeedScopedId> isTransferSource;

  /** Find transfers from all stops. */
  public StreetTransferPathProvider(Graph graph, NearbyStopFinder nearbyStopFinder) {
    this(graph, nearbyStopFinder, stopId -> true);
  }

  /**
   * @param isTransferSource only the stops accepted by this predicate have transfers from them, no
   *                         paths are found for the others.
   */
  public StreetTransferPathProvider(
    Graph graph,
    NearbyStopFinder nearbyStopFinder,
    Predicate<FeedScopedId> isTransferSource
  ) {
    this.graph = graph;
    this.nearbyStopFinder = nearbyStopFinder;
    this.isTransferSource = isTransferSource;
  }

  /**
   * Create the nearby-stop finder for {@code graph}: it searches the streets if the graph has
   * them, and uses the straight-line distance otherwise, like
   * {@code FlexTransferGenerator.createNearbyStopFinder}.
   */
  public static NearbyStopFinder createNearbyStopFinder(
    Graph graph,
    TransitRepository transitRepository
  ) {
    if (!graph.hasStreets) {
      return new StraightLineNearbyStopFinder(
        transitRepository.getSiteRepository()::findRegularStops
      );
    }
    return StreetNearbyStopFinder.of(null).build();
  }

  @Override
  public TransferSearch<NearbyStop> searchFor(RouteRequest request) {
    return new DefaultTransferSearch(graph, nearbyStopFinder, isTransferSource, request);
  }
}
