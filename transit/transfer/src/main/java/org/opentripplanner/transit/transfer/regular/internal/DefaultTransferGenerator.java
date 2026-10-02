package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.TransferGenerator;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfiles;
import org.opentripplanner.transit.transfer.regular.spi.TransferPath;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graph-build-time implementation of {@link TransferGenerator} - see that interface's doc. There
 * is no {@code TransferRealtimeUpdater} implementation yet.
 *
 * @param <P> the transfer path/template type
 * @param <U> the user preferences type
 */
public class DefaultTransferGenerator<P, U> implements TransferGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultTransferGenerator.class);
  private final StopIndex stopIndex;
  private final Collection<FeedScopedId> stopsWithTrips;
  private final TransferPathProvider<P, U> pathProvider;
  private final TransferProfiles<U> orderedProfiles;
  private final RegularTransferBuildRepository<P> buildRepository;

  /**
   * @param progressCallback invoked once per (profile, stop) generated - lets the caller report
   *                         progress without this class taking on a logging dependency itself.
   */
  public DefaultTransferGenerator(
    StopIndex stopIndex,
    Collection<FeedScopedId> stopsWithTrips,
    TransferPathProvider<P, U> pathProvider,
    TransferProfiles<U> profiles,
    RegularTransferBuildRepository<P> buildRepository
  ) {
    this.stopIndex = stopIndex;
    this.stopsWithTrips = stopsWithTrips;
    this.pathProvider = pathProvider;
    this.orderedProfiles = profiles;
    this.buildRepository = buildRepository;
  }

  @Override
  public void generateTransfersForAllStops() {
    var progress = ProgressTracker.track("Generate regular transfers", 1000, stopsWithTrips.size());
    stopsWithTrips.parallelStream().forEach(stop -> {
      for (var profile : orderedProfiles) {
        generateForStop(profile, stop);
      }
      progress.step(m -> LOG.info(m));
    });
    LOG.info(progress.completeMessage());
  }

  /**
   * Run the profile's own discovery search and keep only the cheapest candidate per target stop.
   * <p>
   * Only the cheapest(generalized-cost) candidate per target stop is kept. We assume that the diffrence between the
   * best time and generalized-cost is small. The {@link TransferPathProvider} may return multiple candidates for the
   * same target stop.
   */
  private void generateForStop(TransferProfile<U> profile, FeedScopedId fromStopId) {
    int fromStop = stopIndex.toStopIndex(fromStopId);
    Map<Integer, TransferPath<P>> bestCandidatePerTargetStop = new HashMap<>();

    var nearbyStops = pathProvider.findNearbyStops(
      fromStopId,
      profile.profileType(),
      profile.userPreferences()
    );
    for (var candidate : nearbyStops) {
      int toStop = stopIndex.toStopIndex(candidate.toStop());
      var existing = bestCandidatePerTargetStop.get(toStop);
      if (existing == null || candidate.criteria().c1() < existing.criteria().c1()) {
        bestCandidatePerTargetStop.put(toStop, candidate);
      }
    }

    Map<Integer, P> pathsByToStop = new HashMap<>();
    bestCandidatePerTargetStop.forEach((toStop, candidate) ->
      pathsByToStop.put(toStop, candidate.path())
    );
    // Stops are generated in parallel, the build repository is not thread-safe
    synchronized (this) {
      buildRepository.setPaths(profile.profileType(), fromStop, pathsByToStop);
    }
  }
}
