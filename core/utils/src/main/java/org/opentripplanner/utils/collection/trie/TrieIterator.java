package org.opentripplanner.utils.collection.trie;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Iterates over the entries of a trie, depth first. The stack has a fixed size, because the trie
 * is at most {@link TrieNode#MAX_DEPTH} levels deep.
 */
final class TrieIterator<K, V> implements Iterator<Map.Entry<K, V>> {

  private final Object[][] slotsStack = new Object[TrieNode.MAX_DEPTH][];
  private final int[] indexStack = new int[TrieNode.MAX_DEPTH];
  private int depth = 0;
  private TrieLeaf<K, V> next;

  TrieIterator(TrieNode<K, V> root) {
    slotsStack[0] = root.slots;
    next = findNextChain();
  }

  @Override
  public boolean hasNext() {
    return next != null;
  }

  @Override
  public Map.Entry<K, V> next() {
    if (next == null) {
      throw new NoSuchElementException();
    }
    var current = next;
    next = current.next != null ? current.next : findNextChain();
    return current;
  }

  @SuppressWarnings("unchecked")
  private TrieLeaf<K, V> findNextChain() {
    while (depth >= 0) {
      Object[] slots = slotsStack[depth];
      if (indexStack[depth] == slots.length) {
        --depth;
        continue;
      }
      Object slot = slots[indexStack[depth]++];
      if (slot instanceof TrieNode<?, ?> child) {
        ++depth;
        slotsStack[depth] = child.slots;
        indexStack[depth] = 0;
      } else {
        return (TrieLeaf<K, V>) slot;
      }
    }
    return null;
  }
}
