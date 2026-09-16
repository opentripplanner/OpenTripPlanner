package org.opentripplanner.transit.transfer.regular.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.RaptorTransferProfile;

/**
 * Holds every discovered {@code P} path template, keyed by {@code (profileId, fromStop, toStop)}.
 * Written once by {@link DefaultTransferGenerator#generateTransfersForAllStops()}; not mutated
 * afterward in v1. Doubles as the lookup service an itinerary mapper needs to recover a
 * transfer's real street path for leg geometry - see {@link #findPath}.
 * <p>
 * Plain {@code HashMap}, not {@code EnumMap}: Kryo can't deserialize an empty {@code EnumMap} (it
 * can't infer the enum key type from zero entries) - a real bug this design hit, since the empty
 * instance (no raptor-data GraphBuilderModule ran) must still round-trip through graph
 * serialization.
 * <p>
 * No total stop count is required upfront - this is a plain, size-agnostic map so the empty
 * instance can be created once as a Dagger singleton (mirroring {@code TransferRepository}) before
 * the transit model is indexed, then populated in place by the graph-builder module, and survive
 * graph serialization the same way {@code TransferRepository} does.
 *
 * @param <P> the transfer path/template type
 */
public class RegularTransferRepository<P> implements Serializable {

  private final Map<RaptorTransferProfile, Map<Integer, Map<Integer, P>>> pathsByProfile =
    new HashMap<>();

  void setPath(RaptorTransferProfile profileId, int fromStop, int toStop, P path) {
    pathsByProfile
      .computeIfAbsent(profileId, p -> new HashMap<>())
      .computeIfAbsent(fromStop, s -> new HashMap<>())
      .put(toStop, path);
  }

  /**
   * Recover the real street path template for a resolved transfer - used by itinerary mapping to
   * build a transfer leg's geometry/walk-steps, since the routing-time {@code RaptorTransfers}
   * lookup only carries {@code (stop, duration, c1)}, not the path itself.
   */
  @Nullable
  public P findPath(RaptorTransferProfile profileId, int fromStop, int toStop) {
    var byFromStop = pathsByProfile.get(profileId);
    if (byFromStop == null) {
      return null;
    }
    var byToStop = byFromStop.get(fromStop);
    return byToStop == null ? null : byToStop.get(toStop);
  }

  /** All candidates stored from {@code fromStop} for {@code profileId}, keyed by toStop. */
  Map<Integer, P> pathsFrom(RaptorTransferProfile profileId, int fromStop) {
    var byFromStop = pathsByProfile.get(profileId);
    if (byFromStop == null) {
      return Map.of();
    }
    return byFromStop.getOrDefault(fromStop, Map.of());
  }

  /** Every stored {@code (fromStop, toStop, path)} for a profile. Empty if it has none. */
  public List<StoredPath<P>> pathsFor(RaptorTransferProfile profileId) {
    var byFromStop = pathsByProfile.get(profileId);
    if (byFromStop == null) {
      return List.of();
    }
    List<StoredPath<P>> result = new ArrayList<>();
    for (var fromEntry : byFromStop.entrySet()) {
      for (var toEntry : fromEntry.getValue().entrySet()) {
        result.add(new StoredPath<>(fromEntry.getKey(), toEntry.getKey(), toEntry.getValue()));
      }
    }
    return result;
  }

  public record StoredPath<P>(int fromStop, int toStop, P path) {}
}
