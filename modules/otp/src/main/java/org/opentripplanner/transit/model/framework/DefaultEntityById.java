package org.opentripplanner.transit.model.framework;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The purpose of this class is to provide a map from id to the corresponding entity. It is simply
 * an index of entities.
 *
 * @param <E> the entity type
 */
public class DefaultEntityById<E extends TransitEntity> implements EntityById<E> {

  private final Map<FeedScopedId, E> map = new HashMap<>();

  @Override
  public void add(E entity) {
    map.put(entity.getId(), entity);
  }

  @Override
  public void addAll(Collection<E> entities) {
    entities.forEach(this::add);
  }

  /** Delegates to {@link Map#values()} */
  @Override
  public Collection<E> values() {
    return map.values();
  }

  /**
   * @param id the id whose associated value is to be returned
   * @return the value to which the specified key is mapped, or {@code null} if this map contains no
   * mapping for the key
   */
  @Override
  public E get(FeedScopedId id) {
    return map.get(id);
  }

  /**
   * Returns the number of key-value mappings in this map.
   */
  @Override
  public int size() {
    return map.size();
  }

  /**
   * Returns {@code true} if there are no entries in the map.
   */
  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(FeedScopedId id) {
    return map.containsKey(id);
  }

  /**
   * Return a copy of the internal map. Changes in the source are not reflected in the destination
   * (returned Map), and visa versa.
   * <p>
   * The returned map is immutable.
   */
  @Override
  public Map<FeedScopedId, E> asImmutableMap() {
    return Map.copyOf(map);
  }

  @Override
  public int removeIf(Predicate<E> test) {
    return removeIf(test, null);
  }

  @Override
  public int removeIf(Predicate<E> test, Consumer<E> callback) {
    Collection<E> oldSet = map.values();
    Collection<E> newSet = oldSet.stream().filter(Predicate.not(test)).collect(Collectors.toList());

    int size = map.size();
    if (newSet.size() == size) {
      return 0;
    }
    if (callback != null) {
      var removed = new HashSet<>(oldSet);
      removed.removeAll(Set.copyOf(newSet));
      removed.forEach(callback);
    }
    map.clear();
    addAll(newSet);
    return size - map.size();
  }

  @Override
  public E computeIfAbsent(
    FeedScopedId id,
    Function<? super FeedScopedId, ? extends E> mappingFunction
  ) {
    return map.computeIfAbsent(id, mappingFunction);
  }

  @Override
  public String toString() {
    return map.toString();
  }
}
