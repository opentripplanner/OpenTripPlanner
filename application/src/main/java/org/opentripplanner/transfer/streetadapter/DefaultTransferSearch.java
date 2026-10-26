package org.opentripplanner.transfer.streetadapter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.cost.CostLimit;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.EdgeTraverser;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.streetadapter.StreetSearchRequestMapper;
import org.opentripplanner.transit.transfer.regular.api.maxduration.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.TransferPath;
import org.opentripplanner.transit.transfer.regular.spi.TransferSearch;

/**
 * The {@link TransferSearch} of one request, a street search with {@code P = NearbyStop}: the
 * {@link RouteRequest} the nearby-stop search is configured with, and the
 * {@link StreetSearchRequest} the edges of a path are traversed with when it is re-costed. Both are
 * created once, and are immutable. The street mode is the transfer mode of the request.
 * <p>
 * Re-costing a path traverses its edges with a fresh {@link StateEditor} and
 * {@link EdgeTraverser#traverseEdges}, or estimates its duration from distance and speed when it
 * has no edges, like {@code PathTransfer.asRaptorTransfer}.
 * <p>
 * The max duration of the profile type bounds the street search that discovers the transfers, see
 * {@link TransferSearch#findNearbyStops}. Re-costing a path is not limited.
 */
final class DefaultTransferSearch implements TransferSearch<NearbyStop> {

  private static final int NO_STOP_COUNT_LIMIT = 0;

  private final Graph graph;
  private final NearbyStopFinder nearbyStopFinder;
  private final Predicate<FeedScopedId> isTransferSource;
  private final RouteRequest routeRequest;
  private final StreetSearchRequest streetSearchRequest;

  /**
   * @param isTransferSource only the stops accepted by this predicate have transfers from them, no
   *                         paths are found for the others.
   */
  DefaultTransferSearch(
    Graph graph,
    NearbyStopFinder nearbyStopFinder,
    Predicate<FeedScopedId> isTransferSource,
    RouteRequest routeRequest
  ) {
    this.graph = graph;
    this.nearbyStopFinder = nearbyStopFinder;
    this.isTransferSource = isTransferSource;
    this.routeRequest = routeRequest;
    this.streetSearchRequest = streetSearchRequest(routeRequest);
  }

  @Override
  public Collection<TransferPath<NearbyStop>> findNearbyStops(
    FeedScopedId fromStop,
    MaxDurationsPreference maxDurations
  ) {
    if (!isTransferSource.test(fromStop)) {
      return List.of();
    }
    TransitStopVertex vertex = graph.getStopVertex(fromStop);
    if (vertex == null) {
      return List.of();
    }
    var nearbyStops = nearbyStopFinder.findNearbyStops(
      vertex,
      routeRequest,
      mode(routeRequest),
      false,
      maxDurations.maxDuration(),
      NO_STOP_COUNT_LIMIT
    );
    return nearbyStops
      .stream()
      .filter(nearbyStop -> !nearbyStop.stopId().equals(fromStop))
      .map(DefaultTransferSearch::stripState)
      .<TransferPath<NearbyStop>>mapMulti((path, consumer) ->
        computePathCriteria(path).ifPresent(criteria ->
          consumer.accept(new TransferPath<>(path.stopId(), path, criteria))
        )
      )
      .toList();
  }

  /** The speed of the transfer mode, used for a path without edges. */
  private double speed() {
    return switch (mode(routeRequest)) {
      case BIKE -> streetSearchRequest.bike().speed();
      default -> streetSearchRequest.walk().speed();
    };
  }

  private double reluctance() {
    return switch (mode(routeRequest)) {
      case BIKE -> streetSearchRequest.bike().reluctance();
      default -> streetSearchRequest.walk().reluctance();
    };
  }

  @Override
  public Optional<PathCriteria> computePathCriteria(NearbyStop path) {
    if (path.edges() == null || path.edges().isEmpty()) {
      double durationSeconds = path.distance() / speed();
      int c1 = CostLimit.toRaptorCostWholeSeconds(durationSeconds * reluctance());
      return Optional.of(new PathCriteria(c1, (int) Math.ceil(durationSeconds)));
    }

    var stateEditor = new StateEditor(path.edges().get(0).getFromVertex(), streetSearchRequest);
    stateEditor.setTimeSeconds(0);
    return EdgeTraverser.traverseEdges(stateEditor.makeState(), path.edges()).map(state ->
      new PathCriteria(
        CostLimit.toRaptorCostWholeSeconds(state.getWeight()),
        (int) state.getElapsedTimeSeconds()
      )
    );
  }

  /**
   * Drop the live street-search {@link org.opentripplanner.street.search.state.State} of a path,
   * {@link NearbyStop#state}: it is not {@link java.io.Serializable}, and a path can be re-costed
   * from its {@code edges} and {@code distance} without it.
   */
  private static NearbyStop stripState(NearbyStop nearbyStop) {
    return new NearbyStop(nearbyStop.stopId(), nearbyStop.distance(), nearbyStop.edges(), null);
  }

  private static StreetMode mode(RouteRequest request) {
    return request.journey().transfer().mode();
  }

  private static StreetSearchRequest streetSearchRequest(RouteRequest preferences) {
    return StreetSearchRequestMapper.map(preferences)
      .withFromEnvelope(null)
      .withToEnvelope(null)
      .withArriveBy(false)
      .withStartTime(Instant.EPOCH)
      .withMode(mode(preferences))
      .build();
  }
}
