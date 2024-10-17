package org.opentripplanner.transit.model.framework;

import java.util.Collection;
import java.util.Map;

/**
 * The purpose of this class is to provide a map from id to the corresponding entity. It is simply
 * an index of entities.
 *
 * @param <E> the entity type
 */
public interface ImmutableEntityById<E extends TransitEntity> {
  /** Delegates to {@link Map#values()} */
  Collection<E> values();

  /**
   * @param id the id whose associated value is to be returned
   * @return the value to which the specified key is mapped, or {@code null} if this map contains no
   * mapping for the key
   */
  E get(FeedScopedId id);

  /**
   * Returns the number of key-value mappings in this map.
   */
  int size();

  /**
   * Returns {@code true} if there are no entries in the map.
   */
  boolean isEmpty();

  /**
   * Return {@code true} if there is an entity in the map with the given id, if not return
   * {@code false}.
   */
  boolean containsKey(FeedScopedId id);

  /**
   * Return a copy of the internal map. Changes in the source are not reflected in the destination
   * (returned Map), and visa versa.
   * <p>
   * The returned map is immutable.
   */
  Map<FeedScopedId, E> asImmutableMap();
}
