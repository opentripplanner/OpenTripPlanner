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
public final class ImmutableTrieMap<K, V> extends AbstractMap<K, V> {

  private static final ImmutableTrieMap<?, ?> EMPTY = new ImmutableTrieMap<>(TrieNode.empty(), 0);

  private final TrieNode<K, V> root;
  private final int size;
  private Set<Map.Entry<K, V>> entrySet;

  private ImmutableTrieMap(TrieNode<K, V> root, int size) {
    this.root = root;
    this.size = size;
  }

  @SuppressWarnings("unchecked")
  public static <K, V> ImmutableTrieMap<K, V> empty() {
    return (ImmutableTrieMap<K, V>) EMPTY;
  }

  /**
   * Return a map with the same mappings as the given map. If the given map is an
   * {@link ImmutableTrieMap}, it is returned as is.
   * <p>
   * Takes time linear in the size of the map. Adding the same mappings one by one with
   * {@link #with(Object, Object)} copies part of the map for every mapping, which is much slower
   * for a large map.
   *
   * @throws IllegalArgumentException if two keys of the given map are equal, which is possible for
   *         a map that does not compare keys with {@code equals}, such as
   *         {@link java.util.IdentityHashMap}
   */
  @SuppressWarnings("unchecked")
  public static <K, V> ImmutableTrieMap<K, V> of(Map<? extends K, ? extends V> map) {
    if (map instanceof ImmutableTrieMap<?, ?> trieMap) {
      return (ImmutableTrieMap<K, V>) trieMap;
    }
    if (map.isEmpty()) {
      return empty();
    }
    TrieNode<K, V> root = TrieNode.empty();
    var addedLeaf = new TrieNode.AddedLeaf();
    for (var e : map.entrySet()) {
      K key = Objects.requireNonNull(e.getKey());
      V value = Objects.requireNonNull(e.getValue());
      addedLeaf.val = false;
      root = TrieNode.insertInPlace(root, key, TrieNode.hash(key), value, addedLeaf);
      if (!addedLeaf.val) {
        throw new IllegalArgumentException("Duplicate key: " + key);
      }
    }
    return new ImmutableTrieMap<>(root, map.size());
  }

  /**
   * Return a map with the given mapping added, replacing any existing mapping for the key. If the
   * key already maps to the same value instance, this map is returned.
   */
  public ImmutableTrieMap<K, V> with(K key, V value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    var addedLeaf = new TrieNode.AddedLeaf();
    var newRoot = TrieNode.insert(root, key, TrieNode.hash(key), 0, value, addedLeaf);
    return newRoot == root
      ? this
      : new ImmutableTrieMap<>(newRoot, addedLeaf.val ? size + 1 : size);
  }

  /**
   * Return a map without the mapping for the given key. If the key is not in this map, this map
   * is returned.
   */
  public ImmutableTrieMap<K, V> without(Object key) {
    Objects.requireNonNull(key);
    var newRoot = TrieNode.remove(root, key, TrieNode.hash(key), 0);
    return newRoot == root ? this : new ImmutableTrieMap<>(newRoot, size - 1);
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
