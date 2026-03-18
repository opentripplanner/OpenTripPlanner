package org.opentripplanner.astar.model;

import java.util.function.Consumer;

/**
 * A minimal open-addressing identity hash map with segmented interleaved key/value storage.
 * <p>
 * Splits the hash table into fixed-size segments so no single {@code Object[]} allocation exceeds
 * ~256KB. Each segment is an independent interleaved key/value array where {@code segment[2*i]} is
 * the key and {@code segment[2*i+1]} is the value. Linear probing wraps within a segment.
 * <p>
 * Uses identity semantics ({@code ==}) for key comparison and
 * {@link System#identityHashCode(Object)} for hashing. Power-of-2 capacity with 2/3 load factor.
 * <p>
 * No remove operation — entries are never removed from ShortestPathTree. Does not implement
 * {@link java.util.Map}.
 */
class SegmentedIdentityMap<K, V> {

  private static final int MAX_CAPACITY = 1 << 29;
  private static final int SEGMENT_SLOT_BITS = 14;
  private static final int MAX_SEGMENT_SLOTS = 1 << SEGMENT_SLOT_BITS;

  private Object[][] segments;
  private int slotMask;
  private int slotBits;
  private int segmentMask;
  private int size;
  private int threshold;

  SegmentedIdentityMap(int expectedSize) {
    initSegments(tableSizeFor(expectedSize));
  }

  @SuppressWarnings("unchecked")
  V get(K key) {
    int hash = System.identityHashCode(key);
    int slot = hash & slotMask;
    Object[] seg = segments[(hash >>> slotBits) & segmentMask];
    while (true) {
      int i = slot * 2;
      Object k = seg[i];
      if (k == null) {
        return null;
      }
      if (k == key) {
        return (V) seg[i + 1];
      }
      slot = (slot + 1) & slotMask;
    }
  }

  @SuppressWarnings("unchecked")
  V put(K key, V value) {
    int hash = System.identityHashCode(key);
    int slot = hash & slotMask;
    Object[] seg = segments[(hash >>> slotBits) & segmentMask];
    while (true) {
      int i = slot * 2;
      Object k = seg[i];
      if (k == null) {
        seg[i] = key;
        seg[i + 1] = value;
        if (++size > threshold) {
          resize();
        }
        return null;
      }
      if (k == key) {
        V old = (V) seg[i + 1];
        seg[i + 1] = value;
        return old;
      }
      slot = (slot + 1) & slotMask;
    }
  }

  int size() {
    return size;
  }

  @SuppressWarnings("unchecked")
  void forEachKey(Consumer<K> action) {
    for (Object[] seg : segments) {
      for (int i = 0; i < seg.length; i += 2) {
        Object k = seg[i];
        if (k != null) {
          action.accept((K) k);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  void forEachValue(Consumer<V> action) {
    for (Object[] seg : segments) {
      for (int i = 0; i < seg.length; i += 2) {
        if (seg[i] != null) {
          action.accept((V) seg[i + 1]);
        }
      }
    }
  }

  private void resize() {
    Object[][] oldSegments = segments;
    int oldTotalSlots = (slotMask + 1) * (segmentMask + 1);
    if (oldTotalSlots >= MAX_CAPACITY) {
      throw new IllegalStateException("SegmentedIdentityMap capacity exhausted");
    }
    initSegments(oldTotalSlots * 2);
    for (Object[] oldSeg : oldSegments) {
      for (int j = 0; j < oldSeg.length; j += 2) {
        Object k = oldSeg[j];
        if (k != null) {
          int hash = System.identityHashCode(k);
          int slot = hash & slotMask;
          Object[] seg = segments[(hash >>> slotBits) & segmentMask];
          while (seg[slot * 2] != null) {
            slot = (slot + 1) & slotMask;
          }
          seg[slot * 2] = k;
          seg[slot * 2 + 1] = oldSeg[j + 1];
        }
      }
    }
  }

  private void initSegments(int totalSlots) {
    int slotsPerSegment = Math.min(totalSlots, MAX_SEGMENT_SLOTS);
    int segmentCount = totalSlots / slotsPerSegment;
    this.slotBits = Integer.numberOfTrailingZeros(slotsPerSegment);
    this.slotMask = slotsPerSegment - 1;
    this.segmentMask = segmentCount - 1;
    this.threshold = (totalSlots * 2) / 3;
    this.segments = new Object[segmentCount][];
    for (int i = 0; i < segmentCount; i++) {
      segments[i] = new Object[slotsPerSegment * 2];
    }
  }

  /**
   * Returns the smallest power of 2 that can hold {@code expectedSize} entries at 2/3 load factor.
   */
  private static int tableSizeFor(int expectedSize) {
    int minCapacity = expectedSize + expectedSize / 2 + 1;
    int capacity = Integer.highestOneBit(minCapacity);
    if (capacity < minCapacity) {
      capacity *= 2;
    }
    return Math.max(capacity, 4);
  }
}
