package org.opentripplanner.transit.framework;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.utils.collection.trie.ImmutableTrieMap;

/**
 * Read access to a set of transit entities, looked up by their id.
 * <p>
 * Snapshots and copies share their content with the source, so {@link EntityMap#freeze()} and
 * {@link #copyOf()} take constant time and memory, independent of the size of the map. Each later
 * {@link EntityMap#add} or {@link EntityMap#remove} on a mutable copy costs {@code O(log n)} time
 * and memory, so the additional memory footprint of a copy grows with the number of changes made
 * after it was taken, not with the size of the map.
 * <p>
 * The collections, iterators and streams returned are unmodifiable, and later changes to an
 * {@link EntityMap} are not visible in them.
 */
public sealed class ImmutableEntityMap<E extends TransitEntity>
  implements Iterable<E>
  permits EntityMap
{

  private static final ImmutableEntityMap<?> EMPTY = new ImmutableEntityMap<>(
    ImmutableTrieMap.empty()
  );

  /** Only {@link EntityMap} changes this field. */
  ImmutableTrieMap<FeedScopedId, E> map;

  ImmutableEntityMap(ImmutableTrieMap<FeedScopedId, E> map) {
    this.map = map;
  }

  @SuppressWarnings("unchecked")
  public static <E extends TransitEntity> ImmutableEntityMap<E> empty() {
    return (ImmutableEntityMap<E>) EMPTY;
  }

  /** Return the entity with the given id, or {@code null} if there is none. */
  @Nullable
  public E get(FeedScopedId id) {
    return map.get(id);
  }

  public boolean containsKey(FeedScopedId id) {
    return map.containsKey(id);
  }

  public int size() {
    return map.size();
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public Set<FeedScopedId> keys() {
    return map.keySet();
  }

  public Collection<E> values() {
    return map.values();
  }

  @Override
  public Iterator<E> iterator() {
    return values().iterator();
  }

  public Stream<E> stream() {
    return values().stream();
  }

  /**
   * Return a mutable copy of this map. Changes to the copy are not visible in this map, and the
   * other way around. Takes constant time and memory, independent of the size of the map.
   */
  public EntityMap<E> copyOf() {
    return new EntityMap<>(map);
  }

  @Override
  public String toString() {
    return map.toString();
  }
}
