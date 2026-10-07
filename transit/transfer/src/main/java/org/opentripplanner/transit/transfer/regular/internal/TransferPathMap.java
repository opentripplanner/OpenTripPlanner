package org.opentripplanner.transit.transfer.regular.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot.StoredPath;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * The {@code P} path templates keyed by {@code (profileType, fromStop, toStop)}, shared by the
 * build repository, the runtime repository and its snapshots.
 * <p>
 * <b>Copy-on-write.</b> The map can be large (all stops x nearby stops), so it is never
 * deep-copied. The {@code toStop} maps (the leaves) are never modified once stored - a write
 * replaces the leaf with a modified copy instead. That makes it safe to share them between maps,
 * and {@link #copy()} only copies the two upper levels, the profile and {@code fromStop} maps.
 * <p>
 * Plain {@code HashMap}, not {@code EnumMap}: Kryo can't deserialize an empty {@code EnumMap} (it
 * can't infer the enum key type from zero entries) - a real bug this design hit, since the empty
 * instance (no raptor-data GraphBuilderModule ran) must still round-trip through graph
 * serialization.
 * <p>
 * This class is not thread-safe.
 *
 * @param <P> the transfer path/template type
 */
final class TransferPathMap<P> implements Serializable {

  private final Map<TransferProfileType, Map<Integer, Map<Integer, P>>> pathsByProfile;

  TransferPathMap() {
    this(new HashMap<>());
  }

  private TransferPathMap(Map<TransferProfileType, Map<Integer, Map<Integer, P>>> pathsByProfile) {
    this.pathsByProfile = pathsByProfile;
  }

  /**
   * Copy the profile and {@code fromStop} maps. The {@code toStop} maps are never modified, so
   * they are shared, not copied. A write to the copy never changes this map, and the other way
   * around.
   */
  TransferPathMap<P> copy() {
    var copy = new HashMap<TransferProfileType, Map<Integer, Map<Integer, P>>>();
    pathsByProfile.forEach((profileType, byFromStop) ->
      copy.put(profileType, new HashMap<>(byFromStop))
    );
    return new TransferPathMap<>(copy);
  }

  /**
   * Store the paths from {@code fromStop}, keyed by {@code toStop}; paths to other stops are kept.
   * The given map is copied, not retained.
   */
  void setPaths(TransferProfileType profileType, int fromStop, Map<Integer, P> pathsByToStop) {
    if (pathsByToStop.isEmpty()) {
      return;
    }
    var byFromStop = pathsByProfile.computeIfAbsent(profileType, p -> new HashMap<>());
    // Never modify a stored toStop map, it may be shared with another map - replace it.
    var byToStop = new HashMap<>(byFromStop.getOrDefault(fromStop, Map.of()));
    byToStop.putAll(pathsByToStop);
    byFromStop.put(fromStop, byToStop);
  }

  @Nullable
  P findPath(TransferProfileType profileType, int fromStop, int toStop) {
    var byFromStop = pathsByProfile.get(profileType);
    if (byFromStop == null) {
      return null;
    }
    var byToStop = byFromStop.get(fromStop);
    return byToStop == null ? null : byToStop.get(toStop);
  }

  List<StoredPath<P>> pathsFor(TransferProfileType profileType) {
    var byFromStop = pathsByProfile.get(profileType);
    if (byFromStop == null) {
      return List.of();
    }
    List<StoredPath<P>> result = new ArrayList<>();
    for (var fromEntry : byFromStop.entrySet()) {
      for (var toEntry : fromEntry.getValue().entrySet()) {
        result.add(new StoredPath<>(fromEntry.getKey(), toEntry.getKey(), toEntry.getValue()));
      }
    }
    return Collections.unmodifiableList(result);
  }

  boolean hasTransfersFrom(int fromStop) {
    return pathsByProfile
      .values()
      .stream()
      .anyMatch(byFromStop -> !byFromStop.getOrDefault(fromStop, Map.of()).isEmpty());
  }

  /** The number of stored paths for a profile. */
  int size(TransferProfileType profileType) {
    return pathsByProfile
      .getOrDefault(profileType, Map.of())
      .values()
      .stream()
      .mapToInt(Map::size)
      .sum();
  }

  /** The number of stored paths, in all profiles. */
  int size() {
    return pathsByProfile
      .values()
      .stream()
      .flatMap(byFromStop -> byFromStop.values().stream())
      .mapToInt(Map::size)
      .sum();
  }
}
