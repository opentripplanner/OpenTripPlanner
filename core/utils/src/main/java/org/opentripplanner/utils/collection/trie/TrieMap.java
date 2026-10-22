package org.opentripplanner.utils.collection.trie;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * An immutable map for large data sets that change a little at a time, where many versions of the
 * map must be alive at once.
 * <p>
 * The map is persistent: {@link #with(Object, Object)} and {@link #without(Object)} return a new
 * version and leave this one unchanged. A new version shares all unchanged content with the
 * previous one, so a change costs {@code O(log n)} time and memory, not {@code O(n)} like a copy.
 * The {@link Map} mutation methods throw {@link UnsupportedOperationException}.
 * <p>
 * Keys and values can not be {@code null}. Iteration order is unspecified.
 */
public final class TrieMap<K, V> extends AbstractMap<K, V> {

  private static final TrieMap<?, ?> EMPTY = new TrieMap<>(TrieNode.empty(), 0);

  private final TrieNode<K, V> root;
  private final int size;
  private Set<Map.Entry<K, V>> entrySet;

  private TrieMap(TrieNode<K, V> root, int size) {
    this.root = root;
    this.size = size;
  }

  @SuppressWarnings("unchecked")
  public static <K, V> TrieMap<K, V> empty() {
    return (TrieMap<K, V>) EMPTY;
  }

  /**
   * Return a map with the given mapping added, replacing any existing mapping for the key. If the
   * key already maps to the same value instance, this map is returned.
   */
  public TrieMap<K, V> with(K key, V value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    var change = new TrieNode.Change();
    var newRoot = TrieNode.insert(root, key, TrieNode.hash(key), 0, value, change);
    return newRoot == root ? this : new TrieMap<>(newRoot, change.added ? size + 1 : size);
  }

  /**
   * Return a map without the mapping for the given key. If the key is not in this map, this map
   * is returned.
   */
  public TrieMap<K, V> without(Object key) {
    Objects.requireNonNull(key);
    var newRoot = TrieNode.remove(root, key, TrieNode.hash(key), 0);
    return newRoot == root ? this : new TrieMap<>(newRoot, size - 1);
  }

  @Override
  public V get(Object key) {
    Objects.requireNonNull(key);
    return TrieNode.get(root, key, TrieNode.hash(key));
  }

  @Override
  public boolean containsKey(Object key) {
    return get(key) != null;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    TrieNode.forEach(root, action);
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    if (entrySet == null) {
      entrySet = new AbstractSet<>() {
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
          return new TrieIterator<>(root);
        }

        @Override
        public int size() {
          return size;
        }
      };
    }
    return entrySet;
  }
}
