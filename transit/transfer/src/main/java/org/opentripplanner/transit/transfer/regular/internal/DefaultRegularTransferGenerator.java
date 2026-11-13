package org.opentripplanner.transit.transfer.regular.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
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

  /** The number of paths of a profile (by id) that are the paths of an earlier profile. */
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
      // The paths of this stop of the profiles generated so far, in the order of the profiles
      var pathsOfEarlierProfiles = new ArrayList<Map<Integer, P>>();
      for (var profile : profiles) {
        pathsOfEarlierProfiles.add(generateForStop(profile, stop, pathsOfEarlierProfiles));
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
   * A path of an earlier profile is used instead of the path found for a target stop if it is
   * equivalent: re-costed with the preferences of this profile, it costs about the same, see
   * {@link TransferParameters#DEFAULT_REUSE_LIMIT}. The path is then stored once, for both
   * profiles.
   * If several are equivalent, the one that costs least is used.
   *
   * @param pathsOfEarlierProfiles the paths from this stop of the profiles that are generated
   *                               before this profile, by target stop
   * @return the paths of the profile from the stop, by target stop
   */
  private Map<Integer, P> generateForStop(
    TransferProfileState<P, R> profile,
    FeedScopedId fromStopId,
    List<Map<Integer, P>> pathsOfEarlierProfiles
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

    Map<Integer, P> pathsByToStop = new HashMap<>();
    bestCandidatePerTargetStop.forEach((toStop, candidate) -> {
      var reusedPath = findEquivalentPath(
        profile,
        fromStopId,
        toStop,
        candidate,
        pathsOfEarlierProfiles
      );
      if (reusedPath != null) {
        pathsByToStop.put(toStop, reusedPath);
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
   * The cheapest, for the profile, of the paths of the earlier profiles to the target stop that is
   * equivalent to the path the profile found itself, or {@code null} if there is none. A path
   * that several profiles share is only checked once. A path of an earlier profile may not be
   * possible with the preferences of this profile, for example a path of another type, and it may
   * be longer than the max duration of this profile.
   */
  @Nullable
  private P findEquivalentPath(
    TransferProfileState<P, R> profile,
    FeedScopedId fromStopId,
    int toStop,
    TransferPath<P> own,
    List<Map<Integer, P>> pathsOfEarlierProfiles
  ) {
    var checked = Collections.newSetFromMap(new IdentityHashMap<P, Boolean>());
    P best = null;
    int bestC1 = Integer.MAX_VALUE;
    for (var paths : pathsOfEarlierProfiles) {
      var path = paths.get(toStop);
      if (path == null || !checked.add(path)) {
        continue;
      }
      var criteria = profile.search().computePathCriteria(path);
      if (criteria.isPresent()) {
        int c1 = criteria.get().c1();
        if (
          c1 < bestC1 &&
          isWithinLimit(profile, c1, own.criteria().c1()) &&
          profile.search().accepts(fromStopId, own.toStop(), criteria.get(), profile.maxDurations())
        ) {
          best = path;
          bestC1 = c1;
        }
      }
    }
    return best;
  }

  /**
   * Is the cost of the path of an earlier profile within the tolerance of the cost of the path the
   * profile found? The tolerance is the reuse limit of the profile of the cost of the path of the
   * earlier profile. Both costs are in Raptor cost units (centi-seconds).
   */
  private boolean isWithinLimit(TransferProfileState<P, R> profile, int earlierPathC1, int ownC1) {
    var tolerance = profile
      .reuseLimit()
      .calculate(Cost.costOfCentiSeconds(earlierPathC1))
      .toCentiSeconds();
    return Math.abs(earlierPathC1 - ownC1) <= tolerance;
  }

  private int requireStopIndex(FeedScopedId stopId) {
    int index = stopIndex.toStopIndex(stopId);
    if (index == RaptorConstants.NOT_FOUND) {
      throw new IllegalStateException("The stop is not in the stop index: " + stopId);
    }
    return index;
  }
}
