package org.opentripplanner.transit.transfer.regular;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.spi.NearbyPath;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * Test double: candidate paths are pre-registered per {@code (profileId, fromStop)}; cost scales
 * linearly with {@link FakePrefs#costMultiplier()}, filtered by {@link FakePrefs#maxDurationSeconds()}.
 */
public class FakeTransferPathProvider implements TransferPathProvider<FakeTransferPathProvider.FakePath, FakeTransferPathProvider.FakePrefs> {

  private final Map<RaptorTransferProfile, Map<FeedScopedId, List<FakePath>>> byProfileIdAndStop = new EnumMap<>(
    RaptorTransferProfile.class
  );

  public FakePath addNearby(
    RaptorTransferProfile profileId,
    FeedScopedId from,
    FeedScopedId to,
    int baseCost,
    int baseDurationSeconds
  ) {
    var path = new FakePath(from, to, baseCost, baseDurationSeconds);
    byProfileIdAndStop
      .computeIfAbsent(profileId, p -> new HashMap<>())
      .computeIfAbsent(from, f -> new ArrayList<>())
      .add(path);
    return path;
  }

  @Override
  public Collection<NearbyPath<FakePath>> findNearbyStops(
    FeedScopedId sourceStop,
    RaptorTransferProfile profileId,
    FakePrefs prefs
  ) {
    return byProfileIdAndStop
      .getOrDefault(profileId, Map.of())
      .getOrDefault(sourceStop, List.of())
      .stream()
      .map(p -> new NearbyPath<>(p.to(), p))
      .toList();
  }

  @Override
  public Optional<PathCriteria> computePathCriteria(
    FakePath path,
    RaptorTransferProfile profileId,
    FakePrefs prefs
  ) {
    if (path.baseDurationSeconds() > prefs.maxDurationSeconds()) {
      return Optional.empty();
    }
    int cost = (int) Math.round(path.baseCost() * prefs.costMultiplier());
    return Optional.of(new PathCriteria(cost, path.baseDurationSeconds()));
  }

  public record FakePath(FeedScopedId from, FeedScopedId to, int baseCost, int baseDurationSeconds) {}

  public record FakePrefs(double costMultiplier, int maxDurationSeconds) {}
}
