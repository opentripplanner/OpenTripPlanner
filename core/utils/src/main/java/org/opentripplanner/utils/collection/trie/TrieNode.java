package org.opentripplanner.utils.collection.trie;

import java.util.function.BiConsumer;

/**
 * A node of a 32-way, bitmap-compressed hash array mapped trie (HAMT/CHAMP-style). {@code bitmap}
 * marks which of the 32 possible 5-bit slots at this level are occupied; {@code slots} holds only
 * the occupied ones, in bit order, so a node with a single child costs a 1-element array instead
 * of a 32-element one.
 * <p>
 * Each occupied slot holds either a child {@link TrieNode} (push one level deeper, consuming the
 * next 5 hash bits) or a {@link TrieLeaf} chain (one entry, except in the rare case of a genuine
 * 32-bit hash collision).
 * <p>
 * Many versions of a map share nodes and leaves, so a node, its {@code slots} array and a leaf
 * must never change after they are created. A change instead copies the nodes on the path from the
 * root to the changed slot. Each copy gets a new {@code slots} array where only the changed slot
 * differs; every other slot points to the same child as in the original. The new root reaches the
 * new content, the old root still reaches the old content, and both share every subtree off the
 * path. The mutation methods return the new node, or the original node if nothing changed.
 */
final class TrieNode<K, V> {

  private static final int BITS_PER_LEVEL = 5;
  private static final int LEVEL_MASK = 0x1F;

  /** 32 hash bits consumed 5 at a time gives at most 7 levels. */
  static final int MAX_DEPTH = 7;

  private static final TrieNode<?, ?> EMPTY = new TrieNode<>(0, new Object[0]);

  final int bitmap;
  final Object[] slots;

  private TrieNode(int bitmap, Object[] slots) {
    this.bitmap = bitmap;
    this.slots = slots;
  }

  /** Tells the caller of {@link #insert} whether a new key was added or a value replaced. */
  static final class Change {

    boolean added;
  }

  @SuppressWarnings("unchecked")
  static <K, V> TrieNode<K, V> empty() {
    return (TrieNode<K, V>) EMPTY;
  }

  /**
   * MurmurHash3 finalizer applied to {@link Object#hashCode()}: spreads entropy across all 32
   * bits so each 5-bit chunk is well mixed.
   */
  static int hash(Object key) {
    int h = key.hashCode();
    h ^= h >>> 16;
    h *= 0x85ebca6b;
    h ^= h >>> 13;
    h *= 0xc2b2ae35;
    h ^= h >>> 16;
    return h;
  }

  @SuppressWarnings("unchecked")
  static <K, V> V get(TrieNode<K, V> node, Object key, int hash) {
    int shift = 0;
    while (true) {
      int bit = bit(hash, shift);
      if ((node.bitmap & bit) == 0) {
        return null;
      }
      Object slot = node.slots[node.index(bit)];
      if (slot instanceof TrieNode<?, ?>) {
        node = (TrieNode<K, V>) slot;
        shift += BITS_PER_LEVEL;
      } else {
        return ((TrieLeaf<K, V>) slot).find(key);
      }
    }
  }

  /** Returns the new node. Sets {@code change.added} if {@code key} was not present before. */
  @SuppressWarnings("unchecked")
  static <K, V> TrieNode<K, V> insert(
    TrieNode<K, V> node,
    K key,
    int hash,
    int shift,
    V value,
    Change change
  ) {
    int bit = bit(hash, shift);
    int pos = node.index(bit);

    if ((node.bitmap & bit) == 0) {
      change.added = true;
      return new TrieNode<>(
        node.bitmap | bit,
        arrayInsert(node.slots, pos, new TrieLeaf<>(key, value, null))
      );
    }

    Object existing = node.slots[pos];
    Object newSlot;
    if (existing instanceof TrieNode<?, ?>) {
      var child = (TrieNode<K, V>) existing;
      var newChild = insert(child, key, hash, shift + BITS_PER_LEVEL, value, change);
      if (newChild == child) {
        return node;
      }
      newSlot = newChild;
    } else {
      TrieLeaf<K, V> leaf = (TrieLeaf<K, V>) existing;
      V oldValue = leaf.find(key);
      if (oldValue == value) {
        return node;
      } else if (oldValue != null) {
        newSlot = replaceInChain(leaf, key, value);
      } else if (shift + BITS_PER_LEVEL >= Integer.SIZE) {
        // All 32 hash bits are exhausted: this is a genuine hash collision, chain it.
        change.added = true;
        newSlot = new TrieLeaf<>(key, value, leaf);
      } else {
        // A different key landed in the same slot: split it into a child node one level deeper.
        // `leaf` is guaranteed to be a single entry here - multi-entry chains only occur once all
        // hash bits are exhausted (see the branch above).
        int nextShift = shift + BITS_PER_LEVEL;
        TrieNode<K, V> sub = new TrieNode<>(bit(hash(leaf.getKey()), nextShift), new Object[] {
          leaf,
        });
        newSlot = insert(sub, key, hash, nextShift, value, change);
      }
    }
    Object[] newSlots = node.slots.clone();
    newSlots[pos] = newSlot;
    return new TrieNode<>(node.bitmap, newSlots);
  }

  /** Returns the new node, or the given node if {@code key} is not present. */
  @SuppressWarnings("unchecked")
  static <K, V> TrieNode<K, V> remove(TrieNode<K, V> node, Object key, int hash, int shift) {
    int bit = bit(hash, shift);
    if ((node.bitmap & bit) == 0) {
      return node;
    }
    int pos = node.index(bit);
    Object existing = node.slots[pos];

    Object newSlot;
    if (existing instanceof TrieNode<?, ?>) {
      var child = (TrieNode<K, V>) existing;
      var newChild = remove(child, key, hash, shift + BITS_PER_LEVEL);
      if (newChild == child) {
        return node;
      }
      if (newChild.bitmap == 0) {
        return new TrieNode<>(node.bitmap & ~bit, arrayRemove(node.slots, pos));
      }
      newSlot = newChild;
    } else {
      TrieLeaf<K, V> leaf = (TrieLeaf<K, V>) existing;
      if (leaf.find(key) == null) {
        return node;
      }
      TrieLeaf<K, V> newChain = removeFromChain(leaf, key);
      if (newChain == null) {
        return new TrieNode<>(node.bitmap & ~bit, arrayRemove(node.slots, pos));
      }
      newSlot = newChain;
    }
    Object[] newSlots = node.slots.clone();
    newSlots[pos] = newSlot;
    return new TrieNode<>(node.bitmap, newSlots);
  }

  @SuppressWarnings("unchecked")
  static <K, V> void forEach(TrieNode<K, V> node, BiConsumer<? super K, ? super V> action) {
    for (Object slot : node.slots) {
      if (slot instanceof TrieNode<?, ?>) {
        forEach((TrieNode<K, V>) slot, action);
      } else {
        for (TrieLeaf<K, V> l = (TrieLeaf<K, V>) slot; l != null; l = l.next) {
          action.accept(l.getKey(), l.getValue());
        }
      }
    }
  }

  private static int bit(int hash, int shift) {
    return 1 << ((hash >>> shift) & LEVEL_MASK);
  }

  /** The position in {@code slots} of the slot marked by {@code bit} in the bitmap. */
  private int index(int bit) {
    return Integer.bitCount(bitmap & (bit - 1));
  }

  private static <K, V> TrieLeaf<K, V> replaceInChain(TrieLeaf<K, V> leaf, K key, V value) {
    if (leaf.getKey().equals(key)) {
      return new TrieLeaf<>(key, value, leaf.next);
    }
    return new TrieLeaf<>(leaf.getKey(), leaf.getValue(), replaceInChain(leaf.next, key, value));
  }

  private static <K, V> TrieLeaf<K, V> removeFromChain(TrieLeaf<K, V> leaf, Object key) {
    if (leaf == null) {
      return null;
    }
    if (leaf.getKey().equals(key)) {
      return leaf.next;
    }
    return new TrieLeaf<>(leaf.getKey(), leaf.getValue(), removeFromChain(leaf.next, key));
  }

  private static Object[] arrayInsert(Object[] arr, int pos, Object val) {
    Object[] result = new Object[arr.length + 1];
    System.arraycopy(arr, 0, result, 0, pos);
    result[pos] = val;
    System.arraycopy(arr, pos, result, pos + 1, arr.length - pos);
    return result;
  }

  private static Object[] arrayRemove(Object[] arr, int pos) {
    Object[] result = new Object[arr.length - 1];
    System.arraycopy(arr, 0, result, 0, pos);
    System.arraycopy(arr, pos + 1, result, pos, arr.length - pos - 1);
    return result;
  }
}
