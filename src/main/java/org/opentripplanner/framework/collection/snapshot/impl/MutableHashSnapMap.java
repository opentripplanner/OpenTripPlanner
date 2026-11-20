package org.opentripplanner.framework.collection.snapshot.impl;

import org.opentripplanner.framework.collection.snapshot.MutableSnapMap;

public class MutableHashSnapMap<K, V>
  extends ImmutableHashSnapMap<K, V>
  implements MutableSnapMap<K, V> {

  private boolean needsProtectiveCopy = false;

  /// Private and protected constructors. Called from public factory methods.

  protected MutableHashSnapMap() {
    super();
  }

  private MutableHashSnapMap(int initialCapacity) {
    super(initialCapacity);
  }

  /** Create a mutable instance (builder) from either a mutable or immutable instance. */
  protected MutableHashSnapMap(ImmutableHashSnapMap source) {
    super(source);
    needsProtectiveCopy = true; // Arrays alias those in some other snapshot, so can't be modified.
  }

  /// Internal implementation methods

  // We may want to perform the protective copy on construction to avoid repeatedly incurring the
  // cost of this function call. Should be profiled.
  private void protectiveCopy() {
    if (needsProtectiveCopy) {
      keys = keys.clone();
      vals = vals.clone();
      needsProtectiveCopy = false;
    }
  }

  private static final int DURATION_THRESHOLD_MSEC = 10;

  private void expandAndRehash() {
    long start = System.currentTimeMillis();
    K[] oldKeys = keys;
    V[] oldVals = vals;
    setCapacityAndCreateArrays(capacity * 2);
    for (int i = 0; i < oldKeys.length; i++) {
      if (oldKeys[i] != null) {
        put(oldKeys[i], oldVals[i]);
      }
    }
    long duration = System.currentTimeMillis() - start;
    if (duration >= DURATION_THRESHOLD_MSEC) {
      System.out.println("Expanded capacity to " + capacity);
      System.out.println("Rehashed in " + duration + " milliseconds");
    }
  }

  /// Public interface: mutable extensions to immutable superclass

  public void put(K key, V val) {
    protectiveCopy();
    int index = indexOf(key);
    if (vals[index] == null) {
      // Inserting a mapping, rather than replacing an existing one.
      if (size >= capacity) {
        expandAndRehash();
        index = indexOf(key);
      }
      // Increment size after rehash, because rehash sets the size to the number of mappings.
      size += 1;
    }
    keys[index] = key;
    vals[index] = val;
  }

  public void remove(K key) {
    final int index = indexOf(key);
    if (keys[index] != null) {
      protectiveCopy();
      keys[index] = null;
      vals[index] = null;
      size -= 1;
    }
  }

  public ImmutableHashSnapMap snapshot() {
    this.needsProtectiveCopy = true;
    return new ImmutableHashSnapMap(this);
  }

  /// Public factory methods.

  public static MutableHashSnapMap withDefaultCapacity() {
    return new MutableHashSnapMap();
  }

  public static MutableHashSnapMap withInitialCapacity(int initialCapacity) {
    return new MutableHashSnapMap(initialCapacity);
  }
}
