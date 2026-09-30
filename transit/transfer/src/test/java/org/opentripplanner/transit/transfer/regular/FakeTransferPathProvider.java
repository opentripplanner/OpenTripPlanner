package org.opentripplanner.transit.transfer.regular;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.TransferPath;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * Test double: candidate paths are pre-registered per {@code (profileType, fromStop)}; cost
 * scales linearly with the preferences' {@code reluctance()}, filtered by a fixed
 * {@link #withMaxDurationSeconds max duration} (a stand-in for a profile's own config-derived
 * duration limit, which the real {@code TransferPathProvider} implementation is responsible for
 * resolving internally per {@link TransferProfileType}).
 */
public class FakeTransferPathProvider
  implements TransferPathProvider<FakeTransferPathProvider.FakePath, AbstractUserPreferences<?>>
{

  private final Map<TransferProfileType, Map<FeedScopedId, List<FakePath>>> byProfileTypeAndStop =
    new EnumMap<>(TransferProfileType.class);
  private int maxDurationSeconds = Integer.MAX_VALUE;

  public FakeTransferPathProvider withMaxDurationSeconds(int maxDurationSeconds) {
    this.maxDurationSeconds = maxDurationSeconds;
    return this;
  }

  public FakePath addNearby(
    TransferProfileType profileType,
    FeedScopedId from,
    FeedScopedId to,
    int baseCost,
    int baseDurationSeconds
  ) {
    var path = new FakePath(from, to, baseCost, baseDurationSeconds);
    byProfileTypeAndStop
      .computeIfAbsent(profileType, p -> new HashMap<>())
      .computeIfAbsent(from, f -> new ArrayList<>())
      .add(path);
    return path;
  }

  @Override
  public Collection<TransferPath<FakePath>> findNearbyStops(
    FeedScopedId fromStop,
    TransferProfileType profileType,
    AbstractUserPreferences<?> preferences
  ) {
    return byProfileTypeAndStop
      .getOrDefault(profileType, Map.of())
      .getOrDefault(fromStop, List.of())
      .stream()
      .<TransferPath<FakePath>>mapMulti((p, consumer) ->
        computePathCriteria(p, profileType, preferences).ifPresent(criteria ->
          consumer.accept(new TransferPath<>(p.to(), p, criteria))
        )
      )
      .toList();
  }

  @Override
  public Optional<PathCriteria> computePathCriteria(
    FakePath path,
    TransferProfileType profileType,
    AbstractUserPreferences<?> preferences
  ) {
    if (path.baseDurationSeconds() > maxDurationSeconds) {
      return Optional.empty();
    }
    int cost = (int) Math.round(path.baseCost() * preferences.reluctance().value());
    return Optional.of(new PathCriteria(cost, path.baseDurationSeconds()));
  }

  public record FakePath(
    FeedScopedId from,
    FeedScopedId to,
    int baseCost,
    int baseDurationSeconds
  ) {}
}
