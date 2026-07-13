package org.opentripplanner.framework.transaction.moduletest.candyshop.base;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public abstract class AbstractRepository<E extends Entity> {

  private final Map<Integer, E> entitiesById;

  public AbstractRepository(Map<Integer, E> entitiesById) {
    this.entitiesById = entitiesById;
  }

  public E save(E entity) {
    return this.entitiesById.put(entity.id(), entity);
  }

  public Collection<Integer> listIds() {
    return Collections.unmodifiableSet(entitiesById.keySet());
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + "(" + System.identityHashCode(this) + ")";
  }

  protected Map<Integer, E> copyOfEntitiesById() {
    return Map.copyOf(entitiesById);
  }
}
