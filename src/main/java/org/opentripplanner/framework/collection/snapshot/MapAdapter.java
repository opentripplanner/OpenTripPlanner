package org.opentripplanner.framework.collection.snapshot;

import java.util.HashMap;
import java.util.Map;

// For comparing performance on typical workloads against the standard JDK HashMap in TESTING ONLY.
public class MapAdapter<K, V> implements MutableSnapMap<K, V> {

  private Map<K, V> map;

  public MapAdapter(int size) {
    map = new HashMap(size);
  }

  public MapAdapter() {
    map = new HashMap();
  }

  public MapAdapter(Map<K, V> map) {
    this.map = map;
  }

  @Override
  public void put(K key, V value) {
    map.put(key, value);
  }

  @Override
  public void remove(K key) {
    map.remove(key);
  }

  @Override
  public ImmutableSnapMap snapshot() {
    return new MapAdapter(Map.copyOf(map));
  }

  @Override
  public V get(K key) {
    return map.get(key);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public MutableSnapMap mutate() {
    return new MapAdapter(new HashMap(map));
  }
}
