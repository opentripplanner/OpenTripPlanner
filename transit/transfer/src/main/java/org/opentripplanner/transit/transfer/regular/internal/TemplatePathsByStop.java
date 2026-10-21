package org.opentripplanner.transit.transfer.regular.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The {@code P} path templates of one user profile, keyed by {@code (fromStop, toStop)}.
 * <p>
 * Part of the immutable base of {@link TemplatePathsByUserProfile}: written only at graph build,
 * and never modified once the build repository is sealed. The {@code toStop} maps handed out must
 * not be modified.
 * <p>
 * This class is not thread-safe while it is written to.
 *
 * @param <P> the transfer path/template type
 */
final class TemplatePathsByStop<P> implements Serializable {

  private final Map<Integer, Map<Integer, P>> paths = new HashMap<>();

  /**
   * Store the paths from {@code fromStop}, keyed by {@code toStop}; paths to other stops are kept.
   * The given map is copied, not retained.
   */
  void setPaths(int fromStop, Map<Integer, P> pathsByToStop) {
    if (pathsByToStop.isEmpty()) {
      return;
    }
    // Never modify a stored toStop map, it may already have been handed out - replace it.
    var byToStop = new HashMap<>(paths.getOrDefault(fromStop, Map.of()));
    byToStop.putAll(pathsByToStop);
    paths.put(fromStop, byToStop);
  }

  /** The paths from {@code fromStop}, by {@code toStop}. Empty if the stop is unknown. */
  Map<Integer, P> pathsFrom(int fromStop) {
    return paths.getOrDefault(fromStop, Map.of());
  }

  /** Call {@code consumer} with the paths of every {@code fromStop}, by {@code toStop}. */
  void forEachFromStop(BiConsumer<Integer, Map<Integer, P>> consumer) {
    paths.forEach(consumer);
  }

  boolean hasTransfersFrom(int fromStop) {
    return !pathsFrom(fromStop).isEmpty();
  }

  /** The number of stored paths. */
  int size() {
    return paths.values().stream().mapToInt(Map::size).sum();
  }
}
