package org.opentripplanner.framework.collection.snapshot.impl;

import org.opentripplanner.framework.collection.snapshot.ImmutableSnapMap;
import org.opentripplanner.framework.collection.snapshot.MutableSnapMap;

public class ImmutableLayeredSnapMap<K, V> implements ImmutableSnapMap<K, V> {

  private static final Object REMOVED = new Object();

  // The types of these fields are concrete implementations rather than interfaces to prevent deep
  // nesting of layered instances. Using the concrete types also allows finer  manipulation of
  // behavior and internals (signaling when protective copies are necessary).

  protected final MutableHashSnapMap<K, Object> mutable;
  protected final ImmutableHashSnapMap<K, V> immutable;
  protected int nRemoved;

  protected ImmutableLayeredSnapMap(
    MutableHashSnapMap<K, Object> mutable,
    ImmutableHashSnapMap<K, V> immutable,
    int nRemoved
  ) {
    this.mutable = mutable;
    this.immutable = immutable;
    this.nRemoved = nRemoved;
  }

  @Override
  public V get(K key) {
    Object value = mutable.get(key);
    if (value == REMOVED) {
      return null;
    }
    if (value == null) {
      return immutable.get(key);
    }
    return (V) value;
  }

  @Override
  public int size() {
    // FIXME handle cases where removed is overwritten with new value,
    //  and where elements in one layer mask old ones.
    return mutable.size() + immutable.size() - nRemoved;
  }

  @Override
  public MutableSnapMap mutate() {
    return new MutableLayeredSnapMap(new MutableHashSnapMap(mutable), immutable, nRemoved);
  }
}
