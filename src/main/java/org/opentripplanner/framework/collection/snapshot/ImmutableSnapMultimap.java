package org.opentripplanner.framework.collection.snapshot;

import java.util.Collection;

/**
 * This is similar to an ImmutableSnapMap but allows multiple mappings for the same key. Calling
 * get with a particular key returns an unordered collection of values, rather than a single value.
 */
public interface ImmutableSnapMultimap<K, V> {
  // TODO Collections.unmodifiableList(Arrays.asList()); and ensure arrays are private and CoW.
  Collection<V> get(K key);

  /** @return the number of mappings in this map (the number of values, not the number of keys). */
  int size();

  /** @see ImmutableSnapMap#mutate() */
  MutableSnapMultimap mutate();
}
