package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.transit.transfer.regular.TransferGenerator;
import org.opentripplanner.transit.transfer.regular.spi.CostTolerance;
import org.opentripplanner.transit.transfer.regular.spi.NearbyPath;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.RegularTransferParameters;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * v1: only {@link #generateTransfersForAllStops()} is implemented (GraphBuilder use-case).
 * Updater use-cases ({@link #updateTransfersForStop}, {@link #updateTransfersForStops}) are not
 * supported yet.
 *
 * @param <P> the transfer path/template type
 * @param <U> the user preferences type
 */
public class DefaultTransferGenerator<P, U> implements TransferGenerator<P> {

  private final StopIndex stopIndex;
  private final Collection<FeedScopedId> stopsWithTrips;
  private final TransferPathProvider<P, U> pathProvider;
  private final List<RegularTransferParameters<U>> orderedProfiles;
  private final CostTolerance deduplicateTolerance;
  private final RegularTransferRepository<P> repository;

  public DefaultTransferGenerator(
    StopIndex stopIndex,
    Collection<FeedScopedId> stopsWithTrips,
    TransferPathProvider<P, U> pathProvider,
    Collection<RegularTransferParameters<U>> profiles,
    CostTolerance deduplicateTolerance,
    RegularTransferRepository<P> repository
  ) {
    this.stopIndex = stopIndex;
    this.stopsWithTrips = stopsWithTrips;
    this.pathProvider = pathProvider;
    this.orderedProfiles = RegularTransferProfileOrdering.order(profiles);
    this.deduplicateTolerance = deduplicateTolerance;
    this.repository = repository;
  }

  @Override
  public void generateTransfersForAllStops() {
    for (var profile : orderedProfiles) {
      for (FeedScopedId stop : stopsWithTrips) {
        generateForStop(profile, stop);
      }
    }
  }

  @Override
  public void updateTransfersForStop(FeedScopedId stopId) {
    throw new UnsupportedOperationException(
      "Updating transfers for a single stop is not supported yet - v1 only supports " +
      "generateTransfersForAllStops() at graph build time."
    );
  }

  @Override
  public void updateTransfersForStops(List<FeedScopedId> stops, Predicate<P> filterPaths) {
    throw new UnsupportedOperationException(
      "Updating transfers for a set of stops is not supported yet - v1 only supports " +
      "generateTransfersForAllStops() at graph build time."
    );
  }

  /**
   * Run the profile's own discovery search and keep every candidate the provider still considers
   * usable under this profile's own {@code (profileId, preferences)} - not just the single
   * cheapest. If the profile has a {@code base}, each surviving candidate is independently
   * checked for deduplication against the base's already-stored path to the same target stop
   * (the profile still runs its own search regardless - {@code base} only affects what gets
   * stored).
   */
  private void generateForStop(RegularTransferParameters<U> profile, FeedScopedId fromStopId) {
    int fromStop = stopIndex.toStopIndex(fromStopId);
    Map<Integer, NearbyPath<P>> bestCandidatePerTargetStop = new HashMap<>();
    Map<Integer, PathCriteria> bestCriteriaPerTargetStop = new HashMap<>();

    for (var candidate : pathProvider.findNearbyStops(
      fromStopId,
      profile.profileId(),
      profile.preferences()
    )) {
      Optional<PathCriteria> criteria = pathProvider.computePathCriteria(
        candidate.path(),
        profile.profileId(),
        profile.preferences()
      );
      if (criteria.isEmpty()) {
        continue;
      }
      int toStop = stopIndex.toStopIndex(candidate.targetStop());
      var existing = bestCriteriaPerTargetStop.get(toStop);
      if (existing == null || criteria.get().c1() < existing.c1()) {
        bestCandidatePerTargetStop.put(toStop, candidate);
        bestCriteriaPerTargetStop.put(toStop, criteria.get());
      }
    }

    for (var entry : bestCandidatePerTargetStop.entrySet()) {
      int toStop = entry.getKey();
      P ownPath = entry.getValue().path();
      PathCriteria ownCriteria = bestCriteriaPerTargetStop.get(toStop);
      P chosenPath = profile.base() == null
        ? ownPath
        : resolveDeduplicatedPath(profile, fromStop, toStop, ownPath, ownCriteria);
      repository.setPath(profile.profileId(), fromStop, toStop, chosenPath);
    }
  }

  /**
   * @return the base's path if it is cost-equivalent to {@code ownPath} under this profile's own
   * preferences (see {@link CostTolerance}), otherwise {@code ownPath} unchanged.
   */
  private P resolveDeduplicatedPath(
    RegularTransferParameters<U> profile,
    int fromStop,
    int toStop,
    P ownPath,
    PathCriteria ownCriteria
  ) {
    P basePath = repository.findPath(profile.base(), fromStop, toStop);
    if (basePath == null) {
      return ownPath;
    }
    Optional<PathCriteria> baseCriteriaUnderOwnPrefs = pathProvider.computePathCriteria(
      basePath,
      profile.profileId(),
      profile.preferences()
    );
    if (baseCriteriaUnderOwnPrefs.isEmpty()) {
      return ownPath;
    }
    int baseCost = baseCriteriaUnderOwnPrefs.get().c1();
    int delta = Math.abs(ownCriteria.c1() - baseCost);
    return delta <= deduplicateTolerance.toleranceFor(baseCost) ? basePath : ownPath;
  }
}
