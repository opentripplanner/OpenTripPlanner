package org.opentripplanner.transit.transfer.regular.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot.StoredPath;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * The {@code P} path templates keyed by {@code (userPreferences, fromStop, toStop)}, shared by the
 * build repository, the runtime repository and its snapshots.
 * <p>
 * <b>Copy-on-write.</b> The map can be large (all stops x nearby stops), so it is never
 * deep-copied. The {@code toStop} maps (the leaves) are never modified once stored - a write
 * replaces the leaf with a modified copy instead. That makes it safe to share them between maps,
 * and {@link #copy()} only copies the two upper levels, the user preferences and {@code fromStop}
 * maps.
 * <p>
 * This class is not thread-safe.
 *
 * @param <P> the transfer path/template type
 */
final class TransferPathMap<P> implements Serializable {

  private final Map<
    AbstractUserPreferences<?>,
    Map<Integer, Map<Integer, P>>
  > pathsByUserPreferences;

  TransferPathMap() {
    this(new HashMap<>());
  }

  private TransferPathMap(
    Map<AbstractUserPreferences<?>, Map<Integer, Map<Integer, P>>> pathsByUserPreferences
  ) {
    this.pathsByUserPreferences = pathsByUserPreferences;
  }

  /**
   * Copy the user preferences and {@code fromStop} maps. The {@code toStop} maps are never
   * modified, so they are shared, not copied. A write to the copy never changes this map, and the
   * other way around.
   */
  TransferPathMap<P> copy() {
    var copy = new HashMap<AbstractUserPreferences<?>, Map<Integer, Map<Integer, P>>>();
    pathsByUserPreferences.forEach((userPreferences, byFromStop) ->
      copy.put(userPreferences, new HashMap<>(byFromStop))
    );
    return new TransferPathMap<>(copy);
  }

  /**
   * Store the paths from {@code fromStop}, keyed by {@code toStop}; paths to other stops are kept.
   * The given map is copied, not retained.
   */
  void setPaths(
    AbstractUserPreferences<?> userPreferences,
    int fromStop,
    Map<Integer, P> pathsByToStop
  ) {
    if (pathsByToStop.isEmpty()) {
      return;
    }
    var byFromStop = pathsByUserPreferences.computeIfAbsent(userPreferences, p -> new HashMap<>());
    // Never modify a stored toStop map, it may be shared with another map - replace it.
    var byToStop = new HashMap<>(byFromStop.getOrDefault(fromStop, Map.of()));
    byToStop.putAll(pathsByToStop);
    byFromStop.put(fromStop, byToStop);
  }

  @Nullable
  P findPath(AbstractUserPreferences<?> userPreferences, int fromStop, int toStop) {
    var byFromStop = closestPaths(userPreferences);
    if (byFromStop == null) {
      return null;
    }
    var byToStop = byFromStop.get(fromStop);
    return byToStop == null ? null : byToStop.get(toStop);
  }

  List<StoredPath<P>> pathsFor(AbstractUserPreferences<?> userPreferences) {
    var byFromStop = closestPaths(userPreferences);
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
    return pathsByUserPreferences
      .values()
      .stream()
      .anyMatch(byFromStop -> !byFromStop.getOrDefault(fromStop, Map.of()).isEmpty());
  }

  /** The number of paths stored under exactly the given user preferences. */
  int size(AbstractUserPreferences<?> userPreferences) {
    return pathsByUserPreferences
      .getOrDefault(userPreferences, Map.of())
      .values()
      .stream()
      .mapToInt(Map::size)
      .sum();
  }

  /** The number of stored paths, under all user preferences. */
  int size() {
    return pathsByUserPreferences
      .values()
      .stream()
      .flatMap(byFromStop -> byFromStop.values().stream())
      .mapToInt(Map::size)
      .sum();
  }

  /**
   * The paths stored under the user preferences closest to the given ones, {@code null} if no
   * paths are stored.
   */
  @Nullable
  private Map<Integer, Map<Integer, P>> closestPaths(AbstractUserPreferences<?> userPreferences) {
    var exactMatch = pathsByUserPreferences.get(userPreferences);
    if (exactMatch != null) {
      return exactMatch;
    }
    return pathsByUserPreferences
      .entrySet()
      .stream()
      .min(Comparator.comparingDouble(e -> userPreferences.normalizedDistance(e.getKey())))
      .map(Map.Entry::getValue)
      .orElse(null);
  }
}
