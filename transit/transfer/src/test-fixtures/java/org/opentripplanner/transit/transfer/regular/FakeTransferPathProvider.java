package org.opentripplanner.transit.transfer.regular;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.api.maxduration.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.TransferPath;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferSearch;

/**
 * Test double: candidate paths are pre-registered per {@code fromStop}; cost scales linearly
 * with the preferences' {@code reluctance()}. The discovery search only returns the paths within
 * the max duration it is given, re-costing a path never drops it - like the real provider.
 * <p>
 * A path may have closable facility ids. A search that avoids an id does not find the paths with the id,
 * and the avoided ids of every search are recorded, see {@link #avoidedIdsOfSearches()}.
 */
public class FakeTransferPathProvider
  implements TransferPathProvider<FakeTransferPathProvider.FakePath, WalkPreferences>
{

  private final Map<FeedScopedId, List<FakePath>> byStop = new HashMap<>();
  private final Map<CostKey, Integer> costOverrides = new HashMap<>();
  private final Map<Double, Integer> maxDurationOverrides = new HashMap<>();
  private final List<Set<String>> avoidedIdsOfSearches = Collections.synchronizedList(
    new ArrayList<>()
  );

  /**
   * Set the cost of {@code path} under the given {@code reluctance}, instead of the cost scaled by
   * the reluctance. Used to make profiles find different best paths.
   */
  /** The max duration for the searches with the reluctance, the one of the type if not set. */
  public FakeTransferPathProvider withMaxDuration(double reluctance, int seconds) {
    maxDurationOverrides.put(reluctance, seconds);
    return this;
  }

  public FakeTransferPathProvider withCost(FakePath path, double reluctance, int cost) {
    costOverrides.put(new CostKey(path, reluctance), cost);
    return this;
  }

  /** The ids to avoid of each search, in the order the searches were made. */
  public List<Set<String>> avoidedIdsOfSearches() {
    return List.copyOf(avoidedIdsOfSearches);
  }

  public FakePath addNearby(
    FeedScopedId from,
    FeedScopedId to,
    int baseCost,
    int baseDurationSeconds
  ) {
    return addNearby(from, to, baseCost, baseDurationSeconds, Set.of());
  }

  public FakePath addNearby(
    FeedScopedId from,
    FeedScopedId to,
    int baseCost,
    int baseDurationSeconds,
    Set<String> closableFacilityIds
  ) {
    var path = new FakePath(from, to, baseCost, baseDurationSeconds, closableFacilityIds);
    byStop.computeIfAbsent(from, f -> new ArrayList<>()).add(path);
    return path;
  }

  @Override
  public TransferSearch<FakePath> searchFor(WalkPreferences request) {
    return new TransferSearch<>() {
      @Override
      public Collection<TransferPath<FakePath>> findNearbyStops(
        FeedScopedId fromStop,
        MaxDurationsPreference maxDurations
      ) {
        return findNearbyStops(fromStop, maxDurations, Set.of());
      }

      @Override
      public Collection<TransferPath<FakePath>> findNearbyStops(
        FeedScopedId fromStop,
        MaxDurationsPreference maxDurations,
        Set<String> closedFacilityIds
      ) {
        avoidedIdsOfSearches.add(closedFacilityIds);
        return byStop
          .getOrDefault(fromStop, List.of())
          .stream()
          .filter(p -> p.baseDurationSeconds() <= maxSeconds(request, maxDurations))
          .filter(p -> Collections.disjoint(p.closableFacilityIds(), closedFacilityIds))
          .<TransferPath<FakePath>>mapMulti((p, consumer) ->
            computePathCriteria(p).ifPresent(criteria ->
              consumer.accept(new TransferPath<>(p.to(), p, criteria, p.closableFacilityIds()))
            )
          )
          .toList();
      }

      @Override
      public Optional<PathCriteria> computePathCriteria(FakePath path) {
        var override = costOverrides.get(new CostKey(path, request.reluctance().value()));
        int cost =
          override != null
            ? override
            : (int) Math.round(path.baseCost() * request.reluctance().value());
        return Optional.of(new PathCriteria(cost, path.baseDurationSeconds()));
      }

      @Override
      public boolean accepts(
        FeedScopedId fromStop,
        FeedScopedId toStop,
        PathCriteria criteria,
        MaxDurationsPreference maxDurations
      ) {
        return criteria.durationInSeconds() <= maxSeconds(request, maxDurations);
      }
    };
  }

  private long maxSeconds(WalkPreferences request, MaxDurationsPreference maxDurations) {
    var override = maxDurationOverrides.get(request.reluctance().value());
    return override != null ? override : maxDurations.longestDuration().toSeconds();
  }

  private record CostKey(FakePath path, double reluctance) {}

  public record FakePath(
    FeedScopedId from,
    FeedScopedId to,
    int baseCost,
    int baseDurationSeconds,
    Set<String> closableFacilityIds
  ) {
    public FakePath(FeedScopedId from, FeedScopedId to, int baseCost, int baseDurationSeconds) {
      this(from, to, baseCost, baseDurationSeconds, Set.of());
    }
  }
}
