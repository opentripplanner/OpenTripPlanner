package org.opentripplanner.transfer.streetadapter;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.place.nearbystopfinder.PatternConsideringNearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.PatternsByStopLookup;
import org.opentripplanner.place.nearbystopfinder.StraightLineNearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.StreetNearbyStopFinder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.transfer.regular.spi.AllowedModesLookup;
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
  private final Predicate<FeedScopedId> isTransferTarget;
  private final AllowedModesLookup allowedModes;

  /**
   * Find transfers from all stops, no stop is allowed for any mode. This is used to re-cost
   * transfers, not to search for them.
   */
  public StreetTransferPathProvider(Graph graph, NearbyStopFinder nearbyStopFinder) {
    this(graph, nearbyStopFinder, stopId -> true, stopId -> true, AllowedModesLookup.NONE);
  }

  /**
   * @param isTransferSource only the stops accepted by this predicate have transfers from them, no
   *                         paths are found for the others.
   * @param isTransferTarget only the stops accepted by this predicate have transfers to them, the
   *                         paths to the others are dropped.
   * @param allowedModes     the modes the stops are allowed for, to select the max duration of a
   *                         transfer
   */
  public StreetTransferPathProvider(
    Graph graph,
    NearbyStopFinder nearbyStopFinder,
    Predicate<FeedScopedId> isTransferSource,
    Predicate<FeedScopedId> isTransferTarget,
    AllowedModesLookup allowedModes
  ) {
    this.graph = graph;
    this.nearbyStopFinder = nearbyStopFinder;
    this.isTransferSource = isTransferSource;
    this.isTransferTarget = isTransferTarget;
    this.allowedModes = allowedModes;
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

  /**
   * Create the nearby-stop finder for {@code graph}, like
   * {@link #createNearbyStopFinder(Graph, TransitRepository)}, but if the feature
   * {@link OTPFeature#ConsiderPatternsForDirectTransfers} is on, only the stops that are useful for
   * a transfer are found: for each trip pattern only the closest stop. This limits the number of
   * transfers a lot, so the transfers can be stored and the graph built.
   *
   * @param flexIndex the flex trips to consider, or {@code null} to ignore flex trips
   */
  public static NearbyStopFinder createNearbyStopFinder(
    Graph graph,
    TransitRepository transitRepository,
    PatternsByStopLookup patternsByStop,
    @Nullable FlexIndex flexIndex
  ) {
    var finder = createNearbyStopFinder(graph, transitRepository);
    if (!OTPFeature.ConsiderPatternsForDirectTransfers.isOn()) {
      return finder;
    }
    return new PatternConsideringNearbyStopFinder(
      transitRepository.getSiteRepository(),
      patternsByStop,
      flexIndex,
      finder
    );
  }

  @Override
  public TransferSearch<NearbyStop> searchFor(RouteRequest request) {
    return new DefaultTransferSearch(
      graph,
      nearbyStopFinder,
      isTransferSource,
      isTransferTarget,
      allowedModes,
      request
    );
  }
}
