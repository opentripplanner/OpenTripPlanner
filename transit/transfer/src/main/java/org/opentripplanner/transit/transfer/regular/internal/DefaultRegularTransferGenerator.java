package org.opentripplanner.transit.transfer.regular.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.data.stop.RaptorStopIndex;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferGenerator;
import org.opentripplanner.transit.transfer.regular.RegularTransferProfileStatistics;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.spi.TransferPath;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;
import org.opentripplanner.transit.transfer.regular.spi.TransferSearch;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graph-build-time implementation of {@link RegularTransferGenerator} - see that interface's doc.
 * There is no {@code RegularTransferRealtimeUpdater} implementation yet.
 *
 * @param <P> the street path type
 * @param <R> the request type
 */
public class DefaultRegularTransferGenerator<P, R> implements RegularTransferGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultRegularTransferGenerator.class);
  private final RaptorStopIndex stopIndex;
  private final TransferProfiles<P, R> profiles;
  private final RegularTransferBuildRepository<P> buildRepository;
  private final Set<Integer> stopsUsedInTransfers = ConcurrentHashMap.newKeySet();

  /** The number of paths of a profile (by id) that are the paths of its parent. */
  private final Map<String, LongAdder> sharedPaths = new ConcurrentHashMap<>();

  /** The stops that are the start or the end of a path of a profile (by id). */
  private final Map<String, Set<Integer>> stopsUsedByProfile = new ConcurrentHashMap<>();

  public DefaultRegularTransferGenerator(
    RaptorStopIndex stopIndex,
    TransferParameters<R> parameters,
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper,
    RegularTransferBuildRepository<P> buildRepository
  ) {
    this(stopIndex, TransferProfiles.of(parameters, pathProvider, mapper), buildRepository);
  }

  DefaultRegularTransferGenerator(
    RaptorStopIndex stopIndex,
    TransferProfiles<P, R> profiles,
    RegularTransferBuildRepository<P> buildRepository
  ) {
    this.stopIndex = stopIndex;
    this.profiles = profiles;
    this.buildRepository = buildRepository;
  }

  @Override
  public void generateTransfersForAllStops() {
    var stops = listAllStops();
    var progress = ProgressTracker.track("Generate regular transfers", 1000, stops.size());
    stops.parallelStream().forEach(stop -> {
      // The paths found for this stop, the parents are generated before their children
      var pathsOfThisStop = new HashMap<TransferProfileState<P, R>, Map<Integer, P>>();
      for (var profile : profiles) {
        pathsOfThisStop.put(profile, generateForStop(profile, stop, pathsOfThisStop));
      }
      progress.step(m -> LOG.info(m));
    });
    LOG.info(progress.completeMessage());
  }

  @Override
  public Set<FeedScopedId> listAllStopsUsedInTransfers() {
    return stopsUsedInTransfers.stream().map(stopIndex::toStopId).collect(Collectors.toSet());
  }

  /** All stops in the stop index. The path provider decides which of them have transfers. */
  private List<FeedScopedId> listAllStops() {
    var stops = new ArrayList<FeedScopedId>();
    for (int i = 0; i < stopIndex.size(); ++i) {
      var stopId = stopIndex.toStopId(i);
      if (stopId != null) {
        stops.add(stopId);
      }
    }
    return stops;
  }

  @Override
  public List<RegularTransferProfileStatistics> profileStatistics() {
    var result = new ArrayList<RegularTransferProfileStatistics>();
    for (var profile : profiles) {
      var stops = stopsUsedByProfile
        .getOrDefault(profile.id(), Set.of())
        .stream()
        .map(stopIndex::toStopId)
        .collect(Collectors.toSet());
      result.add(
        new RegularTransferProfileStatistics(
          profile.id(),
          profile.userPreferences().toString(),
          profile.reuseLimit(),
          profile.parent() == null ? null : profile.parent().id(),
          buildRepository.calculateNumberOfTransferPaths(profile.userPreferences()),
          sharedPaths.getOrDefault(profile.id(), new LongAdder()).sum(),
          stops
        )
      );
    }
    return result;
  }

  /**
   * Run the profile's own discovery search and keep only the cheapest candidate per target stop.
   * <p>
   * Only the cheapest (generalized-cost) candidate per target stop is kept. We assume that the
   * difference between the best time and generalized-cost is small. The {@link TransferSearch} may
   * return multiple candidates for the same target stop.
   * <p>
   * If the profile has a parent, the path of the parent is used instead of the path found for a
   * target stop if it is equivalent: re-costed with the preferences of this profile, it costs
   * about the same, see {@link TransferParameters#DEFAULT_REUSE_LIMIT}. The path is then stored once, for both
   * profiles.
   *
   * @param pathsOfThisStop the paths found for this stop so far, by profile and target stop
   * @return the paths of the profile from the stop, by target stop
   */
  private Map<Integer, P> generateForStop(
    TransferProfileState<P, R> profile,
    FeedScopedId fromStopId,
    Map<TransferProfileState<P, R>, Map<Integer, P>> pathsOfThisStop
  ) {
    int fromStop = requireStopIndex(fromStopId);
    Map<Integer, TransferPath<P>> bestCandidatePerTargetStop = new HashMap<>();

    var nearbyStops = profile.search().findNearbyStops(fromStopId, profile.maxDurations());
    for (var candidate : nearbyStops) {
      int toStop = stopIndex.toStopIndex(candidate.toStop());
      if (toStop == RaptorConstants.NOT_FOUND) {
        // The nearby stops may include flex stops, they can not be a target of a regular transfer
        continue;
      }
      var existing = bestCandidatePerTargetStop.get(toStop);
      if (existing == null || candidate.criteria().c1() < existing.criteria().c1()) {
        bestCandidatePerTargetStop.put(toStop, candidate);
      }
    }

    var parentPaths =
      profile.parent() == null
        ? Map.<Integer, P>of()
        : pathsOfThisStop.getOrDefault(profile.parent(), Map.of());
    Map<Integer, P> pathsByToStop = new HashMap<>();
    bestCandidatePerTargetStop.forEach((toStop, candidate) -> {
      var parentPath = parentPaths.get(toStop);
      if (parentPath != null && isEquivalent(profile, candidate, parentPath)) {
        pathsByToStop.put(toStop, parentPath);
        sharedPaths.computeIfAbsent(profile.id(), id -> new LongAdder()).increment();
      } else {
        pathsByToStop.put(toStop, candidate.path());
      }
    });
    if (!pathsByToStop.isEmpty()) {
      stopsUsedInTransfers.add(fromStop);
      stopsUsedInTransfers.addAll(pathsByToStop.keySet());
      var stopsOfProfile = stopsUsedByProfile.computeIfAbsent(profile.id(), id ->
        ConcurrentHashMap.newKeySet()
      );
      stopsOfProfile.add(fromStop);
      stopsOfProfile.addAll(pathsByToStop.keySet());
    }
    // Stops are generated in parallel, the build repository is not thread-safe
    synchronized (this) {
      buildRepository.setPaths(profile.userPreferences(), fromStop, pathsByToStop);
    }
    return pathsByToStop;
  }

  /**
   * The path of the parent is equivalent to the path found by the profile if its cost for the
   * profile, is close to the cost of the path the profile found.
   */
  private boolean isEquivalent(
    TransferProfileState<P, R> profile,
    TransferPath<P> own,
    P parentPath
  ) {
    return profile
      .search()
      .computePathCriteria(parentPath)
      .map(criteria -> isWithinLimit(profile, criteria.c1(), own.criteria().c1()))
      .orElse(false);
  }

  /**
   * Is the cost of the path of the parent within the tolerance of the cost of the path of the
   * profile? The tolerance is the reuse limit of the profile of the cost of the path of the parent.
   * Both costs are in Raptor cost units (centi-seconds).
   */
  private boolean isWithinLimit(TransferProfileState<P, R> profile, int parentPathC1, int ownC1) {
    var tolerance = profile
      .reuseLimit()
      .calculate(Cost.costOfCentiSeconds(parentPathC1))
      .toCentiSeconds();
    return Math.abs(parentPathC1 - ownC1) <= tolerance;
  }

  private int requireStopIndex(FeedScopedId stopId) {
    int index = stopIndex.toStopIndex(stopId);
    if (index == RaptorConstants.NOT_FOUND) {
      throw new IllegalStateException("The stop is not in the stop index: " + stopId);
    }
    return index;
  }
}
