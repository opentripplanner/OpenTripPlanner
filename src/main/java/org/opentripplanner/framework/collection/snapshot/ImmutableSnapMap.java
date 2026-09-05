package org.opentripplanner.framework.collection.snapshot;

/**
 * An unmodifiable mapping from keys to values, with one value per key.
 * Reading is thread safe because contents are guaranteed to be unchanging.
 */
public interface ImmutableSnapMap<K, V> {
  /** @return the value currently associated with the given key, or null if no mapping exists. */
  V get(K key);

  /** @return the number of mappings in this map instance. */
  int size();

  /**
   * Efficiently create a modifiable copy of this map. Changes to that copy will NOT affect this
   * source instance. Low-cost operation using deferred copy-on-write to reuse existing objects,
   * minimizing allocations and copies.
   * @return a mutable copy-on-write view of this map.
   */
  MutableSnapMap mutate();
}
