package org.opentripplanner.transit.transfer.regular.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.opentripplanner.transit.transfer.regular.AlternativePath;

/**
 * The {@code P} path templates of one user profile, keyed by {@code (fromStop, toStop)}.
 * <p>
 * Part of the immutable base of {@link TemplatePathsByUserProfile}: written only at graph build,
 * and never modified once the build repository is sealed. The {@code toStop} maps handed out must
 * not be modified.
 * <p>
 * This class is not thread-safe while it is written to.
 *
 * @param <P> the street path type
 */
final class TemplatePathsByStop<P> implements Serializable {

  private final Map<Integer, Map<Integer, P>> paths = new HashMap<>();

  /**
   * The paths of the transfers that depend on ids, keyed by {@code (fromStop, toStop)}.
   * Only a small share of the transfers.
   */
  private final Map<Integer, Map<Integer, List<AlternativePath<P>>>> alternativePaths =
    new HashMap<>();

  /**
   * Index of {@link #alternativePaths}: the stops with a path that depends on a facility, by
   * facility id. Built when first used, after graph build, and never modified after that. It is
   * not serialized.
   */
  private transient volatile Map<String, Set<Integer>> fromStopsByFacilityId;

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

  /**
   * Store the alternative paths from {@code fromStop}, keyed by {@code toStop}, see
   * {@link org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository#setAlternativePaths}.
   */
  void setAlternativePaths(int fromStop, Map<Integer, List<AlternativePath<P>>> pathsByToStop) {
    if (pathsByToStop.isEmpty()) {
      return;
    }
    var byToStop = new HashMap<>(alternativePaths.getOrDefault(fromStop, Map.of()));
    pathsByToStop.forEach((toStop, list) -> byToStop.put(toStop, List.copyOf(list)));
    alternativePaths.put(fromStop, byToStop);
  }

  /**
   * The alternative paths of the transfer, the best path first. Empty if the transfer does not
   * depend on ids, or if they were not generated.
   */
  List<AlternativePath<P>> alternativePaths(int fromStop, int toStop) {
    return alternativePaths.getOrDefault(fromStop, Map.of()).getOrDefault(toStop, List.of());
  }

  /**
   * The stops that have a transfer with an alternative path that depends on one of the ids. The
   * index is built the first time it is needed.
   */
  Set<Integer> fromStopsDependingOn(Set<String> facilityIds) {
    var index = fromStopsByFacilityId;
    if (index == null) {
      index = buildIndex();
      fromStopsByFacilityId = index;
    }
    var result = new HashSet<Integer>();
    for (var id : facilityIds) {
      result.addAll(index.getOrDefault(id, Set.of()));
    }
    return result;
  }

  private Map<String, Set<Integer>> buildIndex() {
    var index = new HashMap<String, Set<Integer>>();
    alternativePaths.forEach((fromStop, byToStop) ->
      byToStop
        .values()
        .forEach(alternatives ->
          alternatives.forEach(alternative ->
            alternative
              .closableFacilityIds()
              .forEach(id -> index.computeIfAbsent(id, k -> new HashSet<>()).add(fromStop))
          )
        )
    );
    return index;
  }

  /** The paths from {@code fromStop}, by {@code toStop}. Empty if the stop is unknown. */
  Map<Integer, P> pathsFrom(int fromStop) {
    return paths.getOrDefault(fromStop, Map.of());
  }

  /** Call {@code consumer} with the paths of every {@code fromStop}, by {@code toStop}. */
  void forEachFromStop(BiConsumer<Integer, Map<Integer, P>> consumer) {
    paths.forEach(consumer);
  }

  /** The number of stored paths. */
  int size() {
    return paths.values().stream().mapToInt(Map::size).sum();
  }
}
