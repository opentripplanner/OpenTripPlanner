package org.opentripplanner.raptor.data.transfers.regular.streetadapter;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.place.nearbystopfinder.StraightLineNearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.StreetNearbyStopFinder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.cost.CostLimit;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.EdgeTraverser;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.streetadapter.StreetSearchRequestMapper;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.TransferPath;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * The application-side, street-search-backed implementation of {@link TransferPathProvider} that
 * raptor-data's regular-transfer generator and factory are built against - the one place this
 * pipeline crosses out of the raptor SPI into the street/application modules.
 * <p>
 * {@code P = NearbyStop}. Traveler preferences are the concrete {@link AbstractUserPreferences} -
 * {@link RegularTransferPreferencesMapper#toRouteRequest} reconstructs the minimal {@code
 * RouteRequest}/{@code StreetSearchRequest} a street search needs. Only {@code WALK} is supported
 * today, so the street mode is always {@link StreetMode#WALK}. Re-traversal logic (a fresh {@link
 * StateEditor} + {@link EdgeTraverser#traverseEdges}, or a distance/speed estimate when there are
 * no edges) mirrors {@code PathTransfer.asRaptorTransfer}, the equivalent step in today's transfer
 * pipeline.
 * <p>
 * TODO TX - There is no configurable per-profile duration limit yet - the design for it
 *           (restricted-vs-unrestricted rules, per-target-stop mode restrictions, ...) needs more
 *           thought; {@link #SEARCH_RADIUS} is a fixed placeholder until that's designed.
 */
public class StreetTransferPathProvider
  implements TransferPathProvider<NearbyStop, AbstractUserPreferences<?>>
{

  private static final int NO_STOP_COUNT_LIMIT = 0;
  private static final Duration SEARCH_RADIUS = Duration.ofMinutes(30);

  private final Graph graph;
  private final NearbyStopFinder nearbyStopFinder;

  public StreetTransferPathProvider(Graph graph, NearbyStopFinder nearbyStopFinder) {
    this.graph = graph;
    this.nearbyStopFinder = nearbyStopFinder;
  }

  /**
   * Shared by the graph-build (generator) and request-time (factory) call sites, so both build
   * this collaborator the same way: streets if the graph has them, straight-line distance
   * otherwise (mirrors {@code FlexTransferGenerator.createNearbyStopFinder}).
   */
  public static NearbyStopFinder createNearbyStopFinder(
    Graph graph,
    TransitRepository transitRepository
  ) {
    if (!graph.hasStreets) {
      var transitService = new DefaultTransitService(transitRepository);
      return new StraightLineNearbyStopFinder(transitService::findRegularStopsByBoundingBox);
    }
    return StreetNearbyStopFinder.of(null).build();
  }

  @Override
  public Collection<TransferPath<NearbyStop>> findNearbyStops(
    FeedScopedId fromStop,
    TransferProfileType profileType,
    AbstractUserPreferences<?> preferences
  ) {
    TransitStopVertex vertex = graph.getStopVertex(fromStop);
    if (vertex == null) {
      return List.of();
    }
    RouteRequest routeRequest = RegularTransferPreferencesMapper.toRouteRequest(preferences);
    var nearbyStops = nearbyStopFinder.findNearbyStops(
      vertex,
      routeRequest,
      StreetMode.WALK,
      false,
      SEARCH_RADIUS,
      NO_STOP_COUNT_LIMIT
    );
    return nearbyStops
      .stream()
      .filter(nearbyStop -> !nearbyStop.stopId.equals(fromStop))
      .map(StreetTransferPathProvider::stripState)
      .<TransferPath<NearbyStop>>mapMulti((path, consumer) ->
        computePathCriteria(path, profileType, preferences).ifPresent(criteria ->
          consumer.accept(new TransferPath<>(path.stopId, path, criteria))
        )
      )
      .toList();
  }

  /**
   * {@link NearbyStop#state} is a live street-search {@link org.opentripplanner.street.search.state.State}
   * - not {@link java.io.Serializable}, and not needed by anything downstream ({@link
   * #computePathCriteria} and itinerary mapping both re-derive it from {@code edges}/{@code
   * distance}). Strip it before a path is handed to the generator/repository, since stored paths
   * get serialized into the graph.
   */
  private static NearbyStop stripState(NearbyStop nearbyStop) {
    return new NearbyStop(nearbyStop.stopId, nearbyStop.distance, nearbyStop.edges, null);
  }

  @Override
  public Optional<PathCriteria> computePathCriteria(
    NearbyStop path,
    TransferProfileType profileType,
    AbstractUserPreferences<?> preferences
  ) {
    RouteRequest routeRequest = RegularTransferPreferencesMapper.toRouteRequest(preferences);
    StreetSearchRequest request = streetSearchRequest(routeRequest);

    if (path.edges == null || path.edges.isEmpty()) {
      double durationSeconds = path.distance / request.walk().speed();
      int c1 = CostLimit.toRaptorCostWholeSeconds(durationSeconds * request.walk().reluctance());
      return Optional.of(new PathCriteria(c1, (int) Math.ceil(durationSeconds)));
    }

    StateEditor stateEditor = new StateEditor(path.edges.get(0).getFromVertex(), request);
    stateEditor.setTimeSeconds(0);
    return EdgeTraverser.traverseEdges(stateEditor.makeState(), path.edges).map(state ->
      new PathCriteria(
        CostLimit.toRaptorCostWholeSeconds(state.getWeight()),
        (int) state.getElapsedTimeSeconds()
      )
    );
  }

  private static StreetSearchRequest streetSearchRequest(RouteRequest preferences) {
    return StreetSearchRequestMapper.map(preferences)
      .withFromEnvelope(null)
      .withToEnvelope(null)
      .withArriveBy(false)
      .withStartTime(Instant.EPOCH)
      .withMode(StreetMode.WALK)
      .build();
  }
}
