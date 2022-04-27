package org.opentripplanner.netex.index.hierarchy;

import java.util.Collection;
import java.util.Map;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMapById;
import org.rutebanken.netex.model.EntityStructure;

/**
 * A hierarchical map of {@link EntityStructure} values indexed by their id. Use the one argument
 * {@link #add(EntityStructure)} method to add elements to the map.
 *
 * @param <V> the value type
 */
public class HierarchicalMapById<V extends EntityStructure>
  extends HierarchicalMap<String, V>
  implements ReadOnlyHierarchicalMapById<V> {

  /** Create a root for the hierarchy */
  public HierarchicalMapById() {}

  /** Create a child of the given {@code parent}. */
  public HierarchicalMapById(HierarchicalMap<String, V> parent) {
    super(parent);
  }

  /** Return a reference to the parent. */
  @Override
  public HierarchicalMapById<V> parent() {
    return (HierarchicalMapById<V>) super.parent();
  }

  /**
   * Use the {@link #add(EntityStructure)} method!
   *
   * @throws IllegalArgumentException This method throws an exception to prevent adding elements
   *                                  with a key different than the element id.
   */
  @Override
  public void add(String key, V value) {
    throw new IllegalArgumentException("Use the add method with just one argument instead.");
  }

  /**
   * Use the {@link #addAll(Collection)} method!
   *
   * @throws IllegalArgumentException This method throws an exception to prevent adding elements
   *                                  with a key different than the element id.
   */
  @Override
  public void addAll(Map<String, V> other) {
    throw new IllegalArgumentException("Use the add method with just one argument instead.");
  }

  @Override
  public Collection<V> localValues() {
    return super.localValues();
  }

  /**
   * Add an entity and use its {@code id} as key to index it.
   */
  public void add(V entity) {
    super.add(entity.getId(), entity);
  }

  /**
   * Add all entities to the local map
   */
  public void addAll(Collection<V> other) {
    for (V e : other) {
      add(e);
    }
  }
}
