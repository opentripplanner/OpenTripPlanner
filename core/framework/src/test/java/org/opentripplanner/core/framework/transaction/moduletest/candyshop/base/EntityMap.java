package org.opentripplanner.framework.transaction.moduletest.candyshop.base;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class EntityMap<E extends Entity> {

  private final Map<Integer, E> entitiesById;

  private EntityMap(Map<Integer, E> entitiesById) {
    this.entitiesById = new HashMap<>(entitiesById);
  }

  public static <E extends Entity> EntityMap<E> of() {
    return new EntityMap<>(new HashMap<>());
  }

  public EntityMap<E> copyOf() {
    return new EntityMap<>(new HashMap<>(entitiesById));
  }

  public E save(E entity) {
    return this.entitiesById.put(entity.id(), entity);
  }

  public E get(Integer id) {
    return this.entitiesById.get(id);
  }

  public Collection<Integer> listIds() {
    return Collections.unmodifiableSet(entitiesById.keySet());
  }
}
