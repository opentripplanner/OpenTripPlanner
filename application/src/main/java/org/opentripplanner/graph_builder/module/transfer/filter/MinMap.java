package org.opentripplanner.graph_builder.module.transfer.filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Decorate a HashMap that to track the smallest value for each key.
 */
class MinMap<K, V extends Comparable<V>> {

  private final Map<K, V> map = new HashMap<>();

  /**
   * Put the given key-value pair in the map if the map does not yet contain the key, or if the
   * value is less than the existing value for the same key.
   *
   * @see Map#put(Object, Object)
   * @return whether the key-value pair was inserted in the map.
   */
  boolean putMin(K key, V value) {
    V oldValue = map.get(key);
    if (oldValue == null || value.compareTo(oldValue) < 0) {
      map.put(key, value);
      return true;
    }
    return false;
  }

  /**
   * @see Map#get(Object)
   */
  @Nullable
  public V get(K key) {
    return map.get(key);
  }

  /**
   * @see Map#values()
   */
  public Collection<V> values() {
    return map.values();
  }
}
