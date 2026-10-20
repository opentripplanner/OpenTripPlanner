package org.opentripplanner.transit.transfer.regular;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.TransferPath;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * Test double: candidate paths are pre-registered per {@code fromStop}; cost scales linearly
 * with the preferences' {@code reluctance()}, filtered by a fixed
 * {@link #withMaxDurationSeconds max duration} (a stand-in for a profile's own config-derived
 * duration limit, which the real {@code TransferPathProvider} implementation is responsible for
 * resolving internally).
 */
public class FakeTransferPathProvider
  implements TransferPathProvider<FakeTransferPathProvider.FakePath, AbstractUserPreferences<?>>
{

  private final Map<FeedScopedId, List<FakePath>> byStop = new HashMap<>();
  private int maxDurationSeconds = Integer.MAX_VALUE;

  public FakeTransferPathProvider withMaxDurationSeconds(int maxDurationSeconds) {
    this.maxDurationSeconds = maxDurationSeconds;
    return this;
  }

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
  public Collection<TransferPath<FakePath>> findNearbyStops(
    FeedScopedId fromStop,
    AbstractUserPreferences<?> userPreferences
  ) {
    return byStop
      .getOrDefault(fromStop, List.of())
      .stream()
      .<TransferPath<FakePath>>mapMulti((p, consumer) ->
        computePathCriteria(p, userPreferences).ifPresent(criteria ->
          consumer.accept(new TransferPath<>(p.to(), p, criteria))
        )
      )
      .toList();
  }

  @Override
  public Optional<PathCriteria> computePathCriteria(
    FakePath path,
    AbstractUserPreferences<?> userPreferences
  ) {
    if (path.baseDurationSeconds() > maxDurationSeconds) {
      return Optional.empty();
    }
    int cost = (int) Math.round(path.baseCost() * userPreferences.reluctance().value());
    return Optional.of(new PathCriteria(cost, path.baseDurationSeconds()));
  }

  public record FakePath(
    FeedScopedId from,
    FeedScopedId to,
    int baseCost,
    int baseDurationSeconds
  ) {}
}
