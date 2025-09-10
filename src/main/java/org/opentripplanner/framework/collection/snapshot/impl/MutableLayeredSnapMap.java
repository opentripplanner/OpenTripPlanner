package org.opentripplanner.framework.collection.snapshot.impl;

import org.opentripplanner.framework.collection.snapshot.ImmutableSnapMap;
import org.opentripplanner.framework.collection.snapshot.MutableSnapMap;

public class MutableLayeredSnapMap<K, V>
  extends ImmutableLayeredSnapMap<K, V>
  implements MutableSnapMap<K, V> {

  // TODO Can we just null the value for this purpose?
  private static final Object REMOVED = new Object();

  protected MutableLayeredSnapMap(
    MutableHashSnapMap<K, Object> mutable,
    ImmutableHashSnapMap<K, V> immutable,
    int nRemoved
  ) {
    super(mutable, immutable, nRemoved);
  }

  protected MutableLayeredSnapMap(ImmutableHashSnapMap<K, V> immutableHashSnapMap) {
    this(new MutableHashSnapMap<>(), immutableHashSnapMap, 0);
  }

  @Override
  public void put(K key, V value) {
    mutable.put(key, value);
  }

  @Override
  public void remove(K key) {
    // Two ways to do this: a special REMOVED object, or rehash all data into a new table on removal.
    if (get(key) != null) {
      mutable.put(key, REMOVED);
      nRemoved += 1;
    }
  }

  @Override
  public ImmutableSnapMap snapshot() {
    // Need to signal that a protective copy is needed. FIXME may be wrong.
    // We should probably just make a protective copy immediately, every time mutate() is called.
    // Maybe separate deferred and non-deferred copy classes.
    mutable.snapshot();
    return new ImmutableLayeredSnapMap(mutable, immutable, nRemoved);
  }
}
