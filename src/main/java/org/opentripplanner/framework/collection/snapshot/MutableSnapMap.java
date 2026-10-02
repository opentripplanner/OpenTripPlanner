package org.opentripplanner.framework.collection.snapshot;

/**
 * Extends the ImmutableSnapMap interface to make it writable.
 * For performance reasons, implementations of this interface are generally not thread safe and
 * perform no locking or synchronization. All reads, writes, and snapshot generation on a particular
 * instance should be performed from only a single thread at a time, with any necessary
 * synchronization provided by the caller.
 */
public interface MutableSnapMap<K, V> extends ImmutableSnapMap<K, V> {
  /** Add a new key-value mapping to this map. */
  void put(K key, V value);

  /** Remove the mapping for the given key, if any, from this map. */
  void remove(K key);

  /**
   * Efficiently create an unmodifiable copy of this map, whose contents are unchanging and will
   * not be affected by further changes to this instance. Implementations aim to reuse existing
   * objects, avoiding allocations and copies.
   * @return an unmodifiable and unchanging view of the mappings currently in this map.
   */
  ImmutableSnapMap snapshot();
}
