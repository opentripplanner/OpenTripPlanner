package org.opentripplanner.framework.collection.snapshot;

public interface MutableSnapMultimap<K, V> extends ImmutableSnapMultimap<K, V> {
  void put(K key, V value);

  // Do we even need removal, given how complicated it is to implement in the layered cases?
  void remove(K key);

  ImmutableSnapMultimap snapshot();
}
