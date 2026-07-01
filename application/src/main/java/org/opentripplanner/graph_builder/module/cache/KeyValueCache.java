package org.opentripplanner.graph_builder.module.cache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A generic key-value cache that supports storing and retrieving serialized data.
 * This class maintains an internal cache and tracks new entries separately.
 *
 * @param <K> the type of the keys, which must be serializable
 * @param <V> the type of the values, which must be serializable
 */
public class KeyValueCache<K extends Serializable, V extends Serializable> implements Serializable {

  private final CacheTask task;
  private final Map<K, V> cache;
  private final Map<K, V> newEntries;

  /**
   * @param cache the loaded cache map, or {@code null} if the cache is empty.
   */
  public KeyValueCache(CacheTask task, @Nullable Map<K, V> cache) {
    this.task = task;
    this.cache = cache == null ? new HashMap<>() : cache;
    this.newEntries = new HashMap<>();
  }

  public void put(K key, V value) {
    newEntries.put(key, value);
  }

  public V get(K key) {
    V v = cache.get(key);
    newEntries.put(key, v);
    return v;
  }

  Map<K, V> newEntries() {
    return newEntries;
  }

  CacheTask task() {
    return task;
  }
}
