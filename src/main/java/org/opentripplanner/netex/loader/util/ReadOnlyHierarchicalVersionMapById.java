package org.opentripplanner.netex.loader.util;

/**
 * A hierarchical read-only view on a multimap indexing a collections of
 * {@link org.rutebanken.netex.model.EntityInVersionStructure} values by
 * their {@code id}. This is used to lookup the correct version of the
 * element for a given key.
 *
 * @param <V> the value type
 */
public interface ReadOnlyHierarchicalVersionMapById<V> {

    /**
     * Return the element with the latest version with the given {@code id}. Returns
     * {@code null} if not element is found.
     */
    V lookupLastVersionById(String id);

}

