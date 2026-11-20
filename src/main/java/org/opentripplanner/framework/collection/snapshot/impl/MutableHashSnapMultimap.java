package org.opentripplanner.framework.collection.snapshot.impl;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.opentripplanner.framework.collection.snapshot.MutableSnapMultimap;

/**
 * Should this use some kind of snapshot set for the values? No, this can be a list multimap that
 * accepts duplicate mappings.
 */
public class MutableHashSnapMultimap<K, V>
  extends ImmutableHashSnapMultimap<K, V>
  implements MutableSnapMultimap<K, V> {

  // Track which lists we created, to avoid redundant copy-on-write between snapshots. This is
  // intentionally using identity (reference) equality instead of Set's default semantic equality.
  // Guava Sets.newIdentityHashSet is just a facade for the approach here.
  private Set<List<V>> ownedLists = Collections.newSetFromMap(new IdentityHashMap<>());

  /// Private and protected constructors. Called from public factory methods.

  private MutableHashSnapMultimap() {
    super();
  }

  /** Capacity here means the number of keys, not number of mappings. */
  private MutableHashSnapMultimap(int initialCapacity) {
    super(initialCapacity);
  }

  /** Create a mutable instance (builder) from either a mutable or immutable source instance. */
  protected MutableHashSnapMultimap(ImmutableHashSnapMultimap source) {
    super(source); // FIXME need to call mutate() on delegate
  }

  /// Internal implementation methods

  /// Public interface: mutable multimap extensions to immutable superclass

  public void put(K key, V val) {
    // No need to make top-level protective copy, that's done at construction.
    List<V> list = map.get(key);
    if (list == null) {
      list = new ArrayList<>();
      map.put(key, list);
      ownedLists.add(list);
    } else if (!ownedLists.contains(list)) {
      list = new ArrayList<>(list);
      ownedLists.add(list);
    }
    list.add(val);
  }

  public void remove(K key) {
    throw new UnsupportedOperationException();
  }

  public ImmutableHashSnapMultimap snapshot() {
    this.ownedLists.clear();
    return new ImmutableHashSnapMultimap(this);
  }

  /// Public factory methods.

  public static MutableHashSnapMultimap withDefaultCapacity() {
    return new MutableHashSnapMultimap();
  }

  public static MutableHashSnapMultimap withInitialCapacity(int initialCapacity) {
    return new MutableHashSnapMultimap(initialCapacity);
  }
}
