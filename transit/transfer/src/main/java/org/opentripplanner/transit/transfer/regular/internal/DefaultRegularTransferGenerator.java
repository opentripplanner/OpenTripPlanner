package org.opentripplanner.transit.transfer.regular.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
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
import org.opentripplanner.transit.transfer.regular.AlternativePath;
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
  private final boolean pathAlternatives;
  private final Set<Integer> stopsUsedInTransfers = ConcurrentHashMap.newKeySet();

  /** The number of paths of a profile (by id) that are the paths of an earlier profile. */
  private final Map<String, LongAdder> sharedPaths = new ConcurrentHashMap<>();

  /** The stops that are the start or the end of a path of a profile (by id). */
  private final Map<String, Set<Integer>> stopsUsedByProfile = new ConcurrentHashMap<>();

  /** The number of searches that avoid closable facility ids, in addition to the discovery searches. */
  private final LongAdder alternativeSearches = new LongAdder();

  /** The number of paths stored for the transfers that depend on ids. */
  private final LongAdder alternativePaths = new LongAdder();

  public DefaultRegularTransferGenerator(
    RaptorStopIndex stopIndex,
    TransferParameters<R> parameters,
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper,
    RegularTransferBuildRepository<P> buildRepository
  ) {
    this(
      stopIndex,
      TransferProfiles.of(parameters, pathProvider, mapper),
      buildRepository,
      parameters.pathAlternatives()
    );
  }

  DefaultRegularTransferGenerator(
    RaptorStopIndex stopIndex,
    TransferProfiles<P, R> profiles,
    RegularTransferBuildRepository<P> buildRepository,
    boolean pathAlternatives
  ) {
    this.stopIndex = stopIndex;
    this.profiles = profiles;
    this.buildRepository = buildRepository;
    this.pathAlternatives = pathAlternatives;
  }

  @Override
  public void generateTransfersForAllStops() {
    var stops = listAllStops();
    var progress = ProgressTracker.track("Generate regular transfers", 1000, stops.size());
    stops.parallelStream().forEach(stop -> {
      // The paths of this stop of the profiles generated so far, in the order of the profiles
      var pathsOfEarlierProfiles = new ArrayList<Map<Integer, AlternativePath<P>>>();
      for (var profile : profiles) {
        pathsOfEarlierProfiles.add(generateForStop(profile, stop, pathsOfEarlierProfiles));
      }
      progress.step(m -> LOG.info(m));
    });
    LOG.info(progress.completeMessage());
    if (pathAlternatives) {
      LOG.info(
        "Transfer alternatives: {} extra searches, {} paths stored for the transfers with closable facility ids.",
        alternativeSearches.sum(),
        alternativePaths.sum()
      );
    }
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
  private Map<Integer, AlternativePath<P>> generateForStop(
    TransferProfileState<P, R> profile,
    FeedScopedId fromStopId,
    List<Map<Integer, AlternativePath<P>>> pathsOfEarlierProfiles
  ) {
    int fromStop = requireStopIndex(fromStopId);
    var bestCandidatePerTargetStop = findBestPerTargetStop(
      profile.search().findNearbyStops(fromStopId, profile.maxDurations())
    );

    Map<Integer, AlternativePath<P>> pathsByToStop = new HashMap<>();
    bestCandidatePerTargetStop.forEach((toStop, candidate) -> {
      var ownPath = new AlternativePath<>(candidate.path(), closableFacilityIdsOf(candidate));
      var reusedPath = findEquivalentPath(
        profile,
        fromStopId,
        toStop,
        candidate,
        ownPath.closableFacilityIds(),
        pathsOfEarlierProfiles
      );
      if (reusedPath != null) {
        pathsByToStop.put(toStop, reusedPath);
        sharedPaths.computeIfAbsent(profile.id(), id -> new LongAdder()).increment();
      } else {
        pathsByToStop.put(toStop, ownPath);
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
    var templates = new HashMap<Integer, P>();
    pathsByToStop.forEach((toStop, alternative) -> templates.put(toStop, alternative.path()));
    // Stops are generated in parallel, the build repository is not thread-safe
    synchronized (this) {
      buildRepository.setPaths(profile.userPreferences(), fromStop, templates);
    }
    if (pathAlternatives) {
      generateAlternativePaths(profile, fromStopId, fromStop, pathsByToStop);
    }
    return pathsByToStop;
  }

  /**
   * The cheapest candidate per target stop. The target stops that are not in the stop index are
   * dropped.
   */
  private Map<Integer, TransferPath<P>> findBestPerTargetStop(
    Collection<TransferPath<P>> candidates
  ) {
    Map<Integer, TransferPath<P>> bestPerTargetStop = new HashMap<>();
    for (var candidate : candidates) {
      int toStop = stopIndex.toStopIndex(candidate.toStop());
      if (toStop == RaptorConstants.NOT_FOUND) {
        // The nearby stops may include flex stops, they can not be a target of a regular transfer
        continue;
      }
      var existing = bestPerTargetStop.get(toStop);
      if (existing == null || candidate.criteria().c1() < existing.criteria().c1()) {
        bestPerTargetStop.put(toStop, candidate);
      }
    }
    return bestPerTargetStop;
  }

  /**
   * The closable facility ids of the path. They are ignored if the feature is off, so the paths are
   * generated and reused as if they had no closable facility ids.
   */
  private Set<String> closableFacilityIdsOf(TransferPath<P> candidate) {
    return pathAlternatives ? candidate.closableFacilityIds() : Set.of();
  }

  /**
   * Find the alternatives of the transfers from the stop that depend on ids and store all
   * paths of these transfers. For each non-empty subset {@code S} of the closable facility ids {@code E} of
   * a best path, the best path of the transfer that avoids
   * {@code S} is an alternative. There is one search for each distinct subset, it is shared by all
   * the transfers that need it. A transfer without a path that avoids {@code S} has no alternative
   * for {@code S}. The alternatives of a transfer are stored once even if several subsets give the
   * same path. The number of searches grows as 2^n for a path with n closable facility ids.
   * <p>
   * The best path is stored with the alternatives, so the closable facility ids of the best path are known.
   * The alternatives are not shared with other profiles, they are found with the search of this
   * profile.
   */
  private void generateAlternativePaths(
    TransferProfileState<P, R> profile,
    FeedScopedId fromStopId,
    int fromStop,
    Map<Integer, AlternativePath<P>> bestPaths
  ) {
    var pathsByToStop = new HashMap<Integer, Set<AlternativePath<P>>>();
    var toStopsByAvoidedIds = new HashMap<Set<String>, List<Integer>>();
    bestPaths.forEach((toStop, best) -> {
      var ids = best.closableFacilityIds();
      if (ids.isEmpty()) {
        return;
      }
      pathsByToStop.put(toStop, new LinkedHashSet<>(List.of(best)));
      for (var subset : nonEmptySubsets(ids)) {
        toStopsByAvoidedIds.computeIfAbsent(subset, k -> new ArrayList<>()).add(toStop);
      }
    });

    toStopsByAvoidedIds.forEach((avoidedIds, toStops) -> {
      alternativeSearches.increment();
      var found = findBestPerTargetStop(
        profile.search().findNearbyStops(fromStopId, profile.maxDurations(), avoidedIds)
      );
      for (int toStop : toStops) {
        var alternative = found.get(toStop);
        if (alternative != null) {
          pathsByToStop
            .get(toStop)
            .add(new AlternativePath<>(alternative.path(), alternative.closableFacilityIds()));
        }
      }
    });

    var stored = new HashMap<Integer, List<AlternativePath<P>>>();
    pathsByToStop.forEach((toStop, paths) -> {
      stored.put(toStop, List.copyOf(paths));
      alternativePaths.add(paths.size());
    });
    synchronized (this) {
      buildRepository.setAlternativePaths(profile.userPreferences(), fromStop, stored);
    }
  }

  /** All non-empty subsets of the ids, there are 2^n - 1 of them. */
  private static List<Set<String>> nonEmptySubsets(Set<String> ids) {
    var list = List.copyOf(ids);
    var subsets = new ArrayList<Set<String>>();
    for (int mask = 1; mask < 1 << list.size(); ++mask) {
      var subset = new HashSet<String>();
      for (int i = 0; i < list.size(); ++i) {
        if ((mask & (1 << i)) != 0) {
          subset.add(list.get(i));
        }
      }
      subsets.add(Set.copyOf(subset));
    }
    return subsets;
  }

  /**
   * The cheapest, for the profile, of the paths of the earlier profiles to the target stop that is
   * equivalent to the path the profile found itself, or {@code null} if there is none. A path
   * that several profiles share is only checked once. A path of an earlier profile may not be
   * possible with the preferences of this profile, for example a path of another type, and it may
   * be longer than the max duration of this profile. The path must also have the same closable facility
   * ids as the path of this profile, it is not equivalent if it depends on something else.
   */
  @Nullable
  private AlternativePath<P> findEquivalentPath(
    TransferProfileState<P, R> profile,
    FeedScopedId fromStopId,
    int toStop,
    TransferPath<P> own,
    Set<String> ownClosableFacilityIds,
    List<Map<Integer, AlternativePath<P>>> pathsOfEarlierProfiles
  ) {
    var checked = Collections.newSetFromMap(new IdentityHashMap<P, Boolean>());
    AlternativePath<P> best = null;
    int bestC1 = Integer.MAX_VALUE;
    for (var paths : pathsOfEarlierProfiles) {
      var path = paths.get(toStop);
      if (
        path == null ||
        !path.closableFacilityIds().equals(ownClosableFacilityIds) ||
        !checked.add(path.path())
      ) {
        continue;
      }
      var criteria = profile.search().computePathCriteria(path.path());
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
