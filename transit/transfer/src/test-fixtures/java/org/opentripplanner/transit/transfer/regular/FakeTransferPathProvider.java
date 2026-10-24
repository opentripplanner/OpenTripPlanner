package org.opentripplanner.transit.transfer.regular;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.api.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.TransferPath;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferSearch;

/**
 * Test double: candidate paths are pre-registered per {@code fromStop}; cost scales linearly
 * with the preferences' {@code reluctance()}. The discovery search only returns the paths within
 * the max duration it is given, re-costing a path never drops it - like the real provider.
 */
public class FakeTransferPathProvider
  implements TransferPathProvider<FakeTransferPathProvider.FakePath, WalkPreferences>
{

  private final Map<FeedScopedId, List<FakePath>> byStop = new HashMap<>();

  public FakePath addNearby(
    FeedScopedId from,
    FeedScopedId to,
    int baseCost,
    int baseDurationSeconds
  ) {
    var path = new FakePath(from, to, baseCost, baseDurationSeconds);
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
        return byStop
          .getOrDefault(fromStop, List.of())
          .stream()
          .filter(p -> p.baseDurationSeconds() <= maxDurations.maxDuration().toSeconds())
          .<TransferPath<FakePath>>mapMulti((p, consumer) ->
            computePathCriteria(p).ifPresent(criteria ->
              consumer.accept(new TransferPath<>(p.to(), p, criteria))
            )
          )
          .toList();
      }

      @Override
      public Optional<PathCriteria> computePathCriteria(FakePath path) {
        int cost = (int) Math.round(path.baseCost() * request.reluctance().value());
        return Optional.of(new PathCriteria(cost, path.baseDurationSeconds()));
      }
    };
  }

  public record FakePath(
    FeedScopedId from,
    FeedScopedId to,
    int baseCost,
    int baseDurationSeconds
  ) {}
}
