package org.opentripplanner.utils.collection.trie;

import java.util.AbstractMap;

/**
 * A single key/value entry stored in a trie slot. {@code next} is non-null only in the rare case
 * of a true 32-bit hash collision between two different keys - every other slot holds a
 * single-element chain.
 */
final class TrieLeaf<K, V> extends AbstractMap.SimpleImmutableEntry<K, V> {

  final TrieLeaf<K, V> next;

  TrieLeaf(K key, V value, TrieLeaf<K, V> next) {
    super(key, value);
    this.next = next;
  }

  V find(Object key) {
    for (TrieLeaf<K, V> l = this; l != null; l = l.next) {
      if (l.getKey().equals(key)) {
        return l.getValue();
      }
    }
    return null;
  }
}
