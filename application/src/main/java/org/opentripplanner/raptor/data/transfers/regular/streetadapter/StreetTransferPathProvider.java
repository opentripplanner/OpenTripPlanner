package org.opentripplanner.raptor.data.transfers.regular.streetadapter;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.module.transfer.api.MaxDurationRule;
import org.opentripplanner.graph_builder.module.transfer.api.TransferProfileConfig;
import org.opentripplanner.graph_builder.module.transfer.api.TransferProfilesConfig;
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
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.transfer.regular.RaptorTransferProfile;
import org.opentripplanner.transit.transfer.regular.spi.NearbyPath;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * The application-side, street-search-backed implementation of {@link TransferPathProvider} that
 * raptor-data's regular-transfer generator and factory are built against - the one place this
 * pipeline crosses out of the raptor SPI into the street/application modules.
 * <p>
 * {@code P = NearbyStop}, {@code U = RouteRequest}. Re-traversal logic (a fresh
 * {@link StateEditor} + {@link EdgeTraverser#traverseEdges}, or a distance/speed estimate when
 * there are no edges) mirrors {@code PathTransfer.asRaptorTransfer}, the equivalent step in
 * today's transfer pipeline.
 */
public class StreetTransferPathProvider implements TransferPathProvider<NearbyStop, RouteRequest> {

  private static final int NO_STOP_COUNT_LIMIT = 0;

  private final Graph graph;
  private final NearbyStopFinder nearbyStopFinder;
  private final Map<RaptorTransferProfile, List<MaxDurationRule>> maxDurationsByProfileId;
  private final Map<StreetMode, Set<StopLocation>> stopsAllowingMode;
  private final SiteRepository siteRepository;

  public StreetTransferPathProvider(
    Graph graph,
    NearbyStopFinder nearbyStopFinder,
    TransferProfilesConfig config,
    TransitRepository transitRepository
  ) {
    this.graph = graph;
    this.nearbyStopFinder = nearbyStopFinder;
    this.maxDurationsByProfileId = config
      .profiles()
      .stream()
      .collect(
        Collectors.toMap(TransferProfileConfig::profileId, TransferProfileConfig::maxDurations)
      );
    this.stopsAllowingMode = Map.of(
      StreetMode.CAR,
      transitRepository.getStopLocationsUsedForCarsAllowedTrips(),
      StreetMode.BIKE,
      transitRepository.getStopLocationsUsedForBikesAllowedTrips()
    );
    this.siteRepository = transitRepository.getSiteRepository();
  }

  /**
   * Shared by the graph-build (generator) and request-time (factory) call sites, so both build
   * this collaborator the same way: streets if the graph has them, straight-line distance
   * otherwise (mirrors {@code DirectTransferGenerator.createNearbyStopFinder}).
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
  public Collection<NearbyPath<NearbyStop>> findNearbyStops(
    FeedScopedId fromStop,
    RaptorTransferProfile profileId,
    RouteRequest preferences
  ) {
    TransitStopVertex vertex = graph.getStopVertex(fromStop);
    if (vertex == null) {
      return List.of();
    }
    var nearbyStops = nearbyStopFinder.findNearbyStops(
      vertex,
      preferences,
      streetMode(profileId),
      false,
      maxSearchRadius(profileId),
      NO_STOP_COUNT_LIMIT
    );
    return nearbyStops
      .stream()
      .filter(nearbyStop -> !nearbyStop.stopId.equals(fromStop))
      .map(nearbyStop -> new NearbyPath<>(nearbyStop.stopId, stripState(nearbyStop)))
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
    RaptorTransferProfile profileId,
    RouteRequest preferences
  ) {
    Optional<Duration> limit = resolveMaxDuration(profileId, path.stopId);
    if (limit.isEmpty()) {
      return Optional.empty();
    }
    StreetSearchRequest request = streetSearchRequest(profileId, preferences);
    long limitSeconds = limit.get().toSeconds();

    if (path.edges == null || path.edges.isEmpty()) {
      double durationSeconds = path.distance / request.walk().speed();
      if (durationSeconds > limitSeconds) {
        return Optional.empty();
      }
      int c1 = CostLimit.toRaptorCostWholeSeconds(durationSeconds * request.walk().reluctance());
      return Optional.of(new PathCriteria(c1, (int) Math.ceil(durationSeconds)));
    }

    StateEditor stateEditor = new StateEditor(path.edges.get(0).getFromVertex(), request);
    stateEditor.setTimeSeconds(0);
    return EdgeTraverser.traverseEdges(stateEditor.makeState(), path.edges)
      .filter(state -> state.getElapsedTimeSeconds() <= limitSeconds)
      .map(state ->
        new PathCriteria(
          CostLimit.toRaptorCostWholeSeconds(state.getWeight()),
          (int) state.getElapsedTimeSeconds()
        )
      );
  }

  /**
   * The most specific matching rule wins: a restricted rule (declared earlier in config) whose
   * {@code allowedModes} covers the to-stop beats the profile's unrestricted rule, if any. A
   * profile with only restricted rules (e.g. CAR, matching today's
   * {@code carsAllowedStopMaxDuration}) has no answer - and so no transfer - for a stop none of
   * those rules cover.
   */
  private Optional<Duration> resolveMaxDuration(
    RaptorTransferProfile profileId,
    FeedScopedId toStopId
  ) {
    List<MaxDurationRule> rules = maxDurationsByProfileId.getOrDefault(profileId, List.of());
    MaxDurationRule matchedRestricted = null;
    MaxDurationRule unrestrictedFallback = null;
    StopLocation toStop = null;

    for (var rule : rules) {
      if (rule.allowedModes() == null) {
        if (unrestrictedFallback == null) {
          unrestrictedFallback = rule;
        }
        continue;
      }
      if (matchedRestricted != null) {
        continue;
      }
      if (toStop == null) {
        toStop = siteRepository.getStopLocation(toStopId);
      }
      if (stopAllowsAnyOf(toStop, rule.allowedModes())) {
        matchedRestricted = rule;
      }
    }

    var winner = matchedRestricted != null ? matchedRestricted : unrestrictedFallback;
    return Optional.ofNullable(winner).map(MaxDurationRule::maxDuration);
  }

  private boolean stopAllowsAnyOf(StopLocation stop, Set<StreetMode> modes) {
    for (StreetMode allowedMode : modes) {
      if (stopsAllowingMode.getOrDefault(allowedMode, Set.of()).contains(stop)) {
        return true;
      }
    }
    return false;
  }

  /** Widest of this profile's configured duration rules, bounding the underlying street search. */
  private Duration maxSearchRadius(RaptorTransferProfile profileId) {
    return maxDurationsByProfileId
      .getOrDefault(profileId, List.of())
      .stream()
      .map(MaxDurationRule::maxDuration)
      .max(Duration::compareTo)
      .orElseThrow(() ->
        new IllegalArgumentException("No maxDurations configured for transfer profile " + profileId)
      );
  }

  private StreetSearchRequest streetSearchRequest(
    RaptorTransferProfile profileId,
    RouteRequest preferences
  ) {
    return StreetSearchRequestMapper.map(preferences)
      .withFromEnvelope(null)
      .withToEnvelope(null)
      .withArriveBy(false)
      .withStartTime(Instant.EPOCH)
      .withMode(streetMode(profileId))
      .build();
  }

  /**
   * WHEELCHAIR maps to {@link StreetMode#WALK} - wheelchair isn't a street mode of its own, it's
   * WALK with wheelchair accessibility enabled on the {@code RouteRequest} passed in as
   * preferences (the caller/config's responsibility to set up per profile).
   * <p>
   * SCOOTER throws: there is no transfer-capable ({@code Feature.TRANSFER}) {@link StreetMode}
   * for it today - only {@code SCOOTER_RENTAL}, which is access/egress-only. Not supported until
   * such a mode exists.
   */
  private static StreetMode streetMode(RaptorTransferProfile profileId) {
    return switch (profileId) {
      case WALK, WHEELCHAIR -> StreetMode.WALK;
      case BICYCLE -> StreetMode.BIKE;
      case CAR -> StreetMode.CAR;
      case SCOOTER -> throw new IllegalArgumentException(
        "SCOOTER transfers are not supported yet - there is no transfer-capable StreetMode for it."
      );
    };
  }
}
