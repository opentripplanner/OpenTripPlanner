package org.opentripplanner.netex.index.api;

import java.time.LocalDateTime;
import java.util.Collection;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

/**
 * A hierarchical read-only view on a multimap indexing a collections of {@link
 * org.rutebanken.netex.model.EntityInVersionStructure} values by their {@code id}. This is used to
 * lookup the correct version of the element for a given key.
 *
 * @param <V> the value type
 */
public interface ReadOnlyHierarchicalVersionMapById<V> {
  /**
   * Return the element with the latest version with the given {@code id}. Returns {@code null} if
   * not element is found.
   */
  V lookupLastVersionById(String id);

  /**
   * Find the entity based on given {@code ref.id} and {@code ref.version} number. If the {@code
   * ref.version} number is empty, then the given {@code timestamp} is used to find the entity valid
   * at that time. If no entities are valid at the given {@code timestamp}, then the first to become
   * valid is chosen. If there is a tie with respect to the validity periods, then the version
   * number for the entities are used.
   * <p>
   * {@code null} if not element is found.
   */
  V lookup(VersionOfObjectRefStructure ref, LocalDateTime timestamp);

  /**
   * List one entity for each key. To select an entity we first look at the validation period, then
   * the version number. An entity is valid, if it has a validity period with a time equals to or
   * after the given {@code timestamp}. No validation periods is treated as an open ended period;
   * hence always valid.
   */
  Collection<V> localListCurrentVersionEntities(LocalDateTime timestamp);

  /**
   * Return {@code true} if the given {@code value.version} is larger or equals to all the maximum
   * version of all elements in the collection.
   * <p/>
   * Note! This method do not check all values in the hierarchy, only the elements in the first
   * collection found.
   */
  boolean isNewerOrSameVersionComparedWithExistingValues(V value);

  /**
   * @return a collection of all keys in the local map, all values added to one of the parents are
   * excluded from the collection.
   * @deprecated This interface should have business methods to retrieve the correct entities based
   * on desired version and validity version. NOT leaving this to the mapper. Fixing this is part
   * of:
   * TODO TOP2 https://github.com/opentripplanner/OpenTripPlanner/issues/2781
   */
  @Deprecated
  Collection<String> localKeys();

  /**
   * @return an empty collection if no element are found.
   * @deprecated This interface should have business methods to retrieve the correct entities based
   * on desired version and validity version. NOT leaving this to the mapper. Fixing this is part
   * of:
   * TODO TOP2 https://github.com/opentripplanner/OpenTripPlanner/issues/2781
   * <p>
   * Lookup element, if not found delegate up to the parent.
   * NB! elements of this class and its parents are NOT merged, the closest win.
   */
  @Deprecated
  Collection<V> lookup(String key);
}
