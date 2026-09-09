package org.opentripplanner.graph_builder.module.islandpruning;

import java.util.HashMap;
import java.util.Map;

/**
 * A minimal multimap from a key to a small set of distinct values, purpose-built for
 * {@link IslandPruningModule}'s street graph adjacency ({@code neighborsForVertex}). Only the
 * operations that class actually needs are implemented: {@link #put}, {@link #containsKey} and
 * {@link #get}.
 * <p>
 * Values for a key are deduplicated and stored in an {@link ArraySet} rather than a hash-based
 * {@link java.util.Set} — see {@link ArraySet} for the benchmarked rationale (street graph
 * vertices typically have very few neighbors, so a linear scan beats hashing).
 */
class ArrayMultimap<K, V> {

  @SuppressWarnings("rawtypes")
  private static final ArraySet EMPTY = new ArraySet();

  private final Map<K, ArraySet<V>> map = new HashMap<>();

  /** Associates {@code value} with {@code key}, if not already present. */
  void put(K key, V value) {
    map.computeIfAbsent(key, k -> new ArraySet<>()).add(value);
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
