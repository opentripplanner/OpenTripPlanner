package org.opentripplanner.framework.collection.snapshot.impl;

import org.opentripplanner.framework.collection.snapshot.ImmutableSnapMap;
import org.opentripplanner.framework.collection.snapshot.MutableSnapMap;

/**
 * The mutable version of this class is implemented as a subclass with additional methods.
 * The type system thus enforces mutable / immutable behavior at compile time.
 * The mutable version applies a copy-on-write strategy when any modifications are made.
 *
 * The immutable form can be created only as a snapshot from a mutable form.
 * In that sense, the mutable form is like a builder.
 *
 * All costs of insertion, rehashing, protective copying etc. are to be incurred in the mutable
 * version of this class. Creating a read-only snapshot should be a near-zero-cost operation.
 * Creating a mutable copy should also be near-zero-cost until any changes actually happen.
 *
 * Sizes could be constrained to be 2*k for integer k, to optimize modulo operations. The table only
 * needs to grow, and is expected to grow slowly between snapshots. It doesn't require any special
 * logic for shrinking back down after removals.
 *
 * The hash table uses open addressing and linear probing. Keeping the array sizes at or above 2x
 * the capacity should yield an average of 2 probes per lookup. The separate key and value arrays
 * may be slightly worse for cache locality, but are more readable/maintainable than a single packed
 * Object array. And it seems possible that they're actually better for locality when probing.
 *
 * It does not support null keys.
 *
 * Non-goals:
 * This class will not implement the Map interface. It is not intended as a drop-in replacement for
 * Map. No concessions are made for multithreaded write access. A mutable instance may only be
 * written from one thread at a time. Snapshot creation is considered a write operation, and must be
 * performed in a synchronized fashion with any other write operations (put and remove).
 */
public class ImmutableHashSnapMap<K, V> implements ImmutableSnapMap<K, V> {

  protected static final int DEFAULT_INITIAL_CAPACITY = 32;
  protected static final int EXPAND_FACTOR = 2;

  protected int size;
  protected int capacity;
  protected K[] keys;
  protected V[] vals;
  protected int hashMask;

  public ImmutableHashSnapMap() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  // CAPACITY MUST BE A POWER OF TWO
  public ImmutableHashSnapMap(int initialCapacity) {
    setCapacityAndCreateArrays(initialCapacity);
  }

  /** Along with creating new empty arrays, (re)set size to zero. */
  protected void setCapacityAndCreateArrays(int newCapacity) {
    capacity = newCapacity;
    final int arrayLength = capacity * EXPAND_FACTOR;
    keys = (K[]) new Object[arrayLength];
    vals = (V[]) new Object[arrayLength];
    size = 0;
    hashMask = arrayLength - 1; // All ones in least significant bits.
  }

  /** Protected shallow copy constructor, for internal use by this class and subclasses. */
  protected ImmutableHashSnapMap(ImmutableHashSnapMap other) {
    size = other.size;
    capacity = other.capacity;
    keys = (K[]) other.keys;
    vals = (V[]) other.vals;
    hashMask = other.hashMask;
  }

  public V get(K key) {
    return vals[indexOf(key)];
  }

  public int size() {
    return size;
  }

  public MutableSnapMap<K, V> mutate() {
    return new MutableLayeredSnapMap<>(this);
    // return new MutableHashSnapMap(this);
  }

  // This method was a hot spot taking 55% of run time when using a modulo operator.
  // Bit masking is much faster.
  protected int indexOf(K key) {
    // Unsigned right shift will ensure hash is positive, as will bitwise-and with lower bits.
    int index = key.hashCode() & hashMask;
    // To terminate, this loop requires there to be at least one null entry in the keys array.
    while (true) {
      if (keys[index] == null || keys[index].equals(key)) {
        return index;
      }
      // Neither the key nor an empty slot was found, try the next slot.
      index += 1;
      if (index == keys.length) {
        index = 0;
      }
    }
  }

  /// Bulk Load Methods

  // public ImmutableHashSnapMap fromKeyValuePairs ()

}
