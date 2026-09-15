package org.opentripplanner.graph_builder.module.islandpruning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal multimap from a key to a small set of distinct values, purpose-built for
 * {@link IslandPruningModule}'s street graph adjacency ({@code neighborsForVertex}). Only the
 * operations that class actually needs are implemented: {@link #put}, {@link #containsKey} and
 * {@link #get}.
 * <p>
 * Values for a key are deduplicated and stored in an {@link ArrayList} for performance.
 */
class ArrayMultimap<K, V> {

  @SuppressWarnings("rawtypes")
  private static final List EMPTY = List.of();

  private final Map<K, List<V>> map = new HashMap<>();

  /** Associates {@code value} with {@code key}, if not already present. */
  void put(K key, V value) {
    var list = map.computeIfAbsent(key, k -> new ArrayList<>(4));
    if (!list.contains(value)) {
      list.add(value);
    }
  }

  boolean containsKey(K key) {
    return map.containsKey(key);
  }

  /** Returns the values associated with {@code key}, or an empty set if there are none. */
  @SuppressWarnings("unchecked")
  Iterable<V> get(K key) {
    return map.getOrDefault(key, EMPTY);
  }
}
