package org.opentripplanner.transit.framework;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.utils.collection.trie.TrieMap;

/**
 * A mutable set of transit entities of one type, looked up by their id. The id of an entity is
 * always its key, so an entity can not be stored under another id.
 * <p>
 * This class is not thread-safe.
 */
public final class EntityMap<E extends TransitEntity> extends ImmutableEntityMap<E> {

  public EntityMap() {
    this(TrieMap.empty());
  }

  EntityMap(TrieMap<FeedScopedId, E> map) {
    super(map);
  }

  /** Add the entity, replacing any existing entity with the same id. */
  public void add(E entity) {
    map = map.with(entity.getId(), entity);
  }

  /** Remove the entity with the given id, if it exists. */
  public void remove(FeedScopedId id) {
    map = map.without(id);
  }

  /** Return a snapshot of the current content. */
  public ImmutableEntityMap<E> freeze() {
    return new ImmutableEntityMap<>(map);
  }
}
