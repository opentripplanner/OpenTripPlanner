package org.opentripplanner.transit.transfer.regular.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.data.stop.RaptorStopIndex;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.TransferGenerator;
import org.opentripplanner.transit.transfer.regular.api.TransferProfilesParameters;
import org.opentripplanner.transit.transfer.regular.spi.TransferPath;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;
import org.opentripplanner.transit.transfer.regular.spi.TransferSearch;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graph-build-time implementation of {@link TransferGenerator} - see that interface's doc. There
 * is no {@code TransferRealtimeUpdater} implementation yet.
 *
 * @param <P> the street path type
 * @param <R> the request type
 */
public class DefaultTransferGenerator<P, R> implements TransferGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultTransferGenerator.class);
  private final RaptorStopIndex stopIndex;
  private final TransferProfiles<P, R> profiles;
  private final RegularTransferBuildRepository<P> buildRepository;
  private final Set<Integer> stopsUsedInTransfers = ConcurrentHashMap.newKeySet();

  public DefaultTransferGenerator(
    RaptorStopIndex stopIndex,
    TransferProfilesParameters<R> parameters,
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper,
    RegularTransferBuildRepository<P> buildRepository
  ) {
    this(stopIndex, TransferProfiles.of(parameters, pathProvider, mapper), buildRepository);
  }

  DefaultTransferGenerator(
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
      for (var profile : profiles) {
        generateForStop(profile, stop);
      }
      progress.step(m -> LOG.info(m));
    });
    LOG.info(progress.completeMessage());
    logSummary(stops.size());
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

  private void logSummary(int numberOfStops) {
    int sum = 0;
    for (var profile : profiles) {
      int size = buildRepository.calculateNumberOfTransferPaths(profile.userPreferences());
      LOG.info("Created {} regular transfers for profile {}.", size, profile.userPreferences());
      sum += size;
    }
    LOG.info(
      "Done generating regular transfers. paths: {}, profiles: {} stops: {}",
      sum,
      profiles.size(),
      numberOfStops
    );
  }

  /**
   * Run the profile's own discovery search and keep only the cheapest candidate per target stop.
   * <p>
   * Only the cheapest (generalized-cost) candidate per target stop is kept. We assume that the
   * difference between the best time and generalized-cost is small. The {@link TransferSearch} may
   * return multiple candidates for the same target stop.
   */
  private void generateForStop(TransferProfile<P, R> profile, FeedScopedId fromStopId) {
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
    bestCandidatePerTargetStop.forEach((toStop, candidate) ->
      pathsByToStop.put(toStop, candidate.path())
    );
    if (!pathsByToStop.isEmpty()) {
      stopsUsedInTransfers.add(fromStop);
      stopsUsedInTransfers.addAll(pathsByToStop.keySet());
    }
    // Stops are generated in parallel, the build repository is not thread-safe
    synchronized (this) {
      buildRepository.setPaths(profile.userPreferences(), fromStop, pathsByToStop);
    }
  }

  private int requireStopIndex(FeedScopedId stopId) {
    int index = stopIndex.toStopIndex(stopId);
    if (index == RaptorConstants.NOT_FOUND) {
      throw new IllegalStateException("The stop is not in the stop index: " + stopId);
    }
    return index;
  }
}
