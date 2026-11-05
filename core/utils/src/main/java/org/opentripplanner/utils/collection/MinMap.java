package org.opentripplanner.utils.collection;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Decorate a HashMap that to track the smallest value for each key.
 */
public final class MinMap<K, V> {

  private final Map<K, V> map = new HashMap<>();
  private final Comparator<V> comparator;

  public MinMap(Comparator<V> comparator) {
    this.comparator = comparator;
  }

  /**
   * Create a MinMap with natural order for values.
   */
  public static <K, V extends Comparable<? super V>> MinMap<K, V> ofNaturalOrder() {
    return new MinMap<>(Comparator.<V>naturalOrder());
  }

  /**
   * Put the given key-value pair in the map if the map does not yet contain the key, or if the
   * value is less than the existing value for the same key.
   *
   * @see Map#put(Object, Object)
   * @return whether the key-value pair was inserted in the map.
   */
  public boolean putMin(K key, V value) {
    V oldValue = map.get(key);
    if (oldValue == null || comparator.compare(value, oldValue) < 0) {
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

  public Collection<Map.Entry<K, V>> entries() {
    return map.entrySet();
  }
}
