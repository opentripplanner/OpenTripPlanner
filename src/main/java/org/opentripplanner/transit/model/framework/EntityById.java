package org.opentripplanner.transit.model.framework;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The purpose of this class is to provide a map from id to the corresponding entity. It is simply
 * an index of entities.
 *
 * @param <E> the entity type
 */
public interface EntityById<E extends TransitEntity> extends ImmutableEntityById<E> {
  void add(E entity);

  void addAll(Collection<E> entities);

  int removeIf(Predicate<E> test);

  int removeIf(Predicate<E> test, Consumer<E> callback);

  E computeIfAbsent(FeedScopedId id, Function<? super FeedScopedId, ? extends E> mappingFunction);
}
