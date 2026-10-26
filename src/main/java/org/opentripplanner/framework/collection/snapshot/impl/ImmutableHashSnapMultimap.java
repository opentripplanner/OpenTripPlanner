package org.opentripplanner.framework.collection.snapshot.impl;

import java.util.Collections;
import java.util.List;
import org.opentripplanner.framework.collection.snapshot.ImmutableSnapMultimap;
import org.opentripplanner.framework.collection.snapshot.MutableSnapMap;
import org.opentripplanner.framework.collection.snapshot.MutableSnapMultimap;

/**
 * Making this extend ImmutableHashSnapMap<K, List<V>> almost works, except that the mutate methods
 * clash and we don't want to return a mutable view of the internal list objects. So instead do it
 * by composition.
 */
public class ImmutableHashSnapMultimap<K, V> implements ImmutableSnapMultimap<K, V> {

  // Currently this is the interface rather than a concrete type, so we can drop in layered
  // implementations as needed. The mutable type is specified here because subclasses need the
  // mutable methods and it's an implementation detail. Immutable implementations should never call
  // the mutating methods of this field.
  protected MutableSnapMap<K, List<V>> map;

  // The size of the multimap (number of mappings) is different from that of the map
  // (number of keys with mappings).
  protected int size;

  public ImmutableHashSnapMultimap() {
    map = MutableHashSnapMap.withDefaultCapacity();
  }

  /**
   * Protected shallow copy constructor, for internal use by this class and subclasses.
   * WARNING: calling this constructor leaks the map, and requires caller to protectively copy
   * anything it contains on any later writes.
   */
  protected ImmutableHashSnapMultimap(ImmutableHashSnapMultimap source) {
    map = source.map;
    size = source.size;
  }

  // WARNING: CAPACITY MUST BE A POWER OF TWO
  // TODO add checks
  public ImmutableHashSnapMultimap(int initialCapacity) {
    map = MutableHashSnapMap.withInitialCapacity(initialCapacity);
  }

  public List<V> get(K key) {
    List<V> ret = map.get(key);
    if (ret == null) {
      return Collections.EMPTY_LIST;
    } else {
      return Collections.unmodifiableList(ret);
    }
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public MutableSnapMultimap<K, V> mutate() {
    return new MutableHashSnapMultimap<>(this);
  }
}
