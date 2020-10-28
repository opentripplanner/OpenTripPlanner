package org.opentripplanner.netex.index.api;

import java.util.Collection;

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

    /**
     * Return {@code true} if the given {@code value.version} is larger or equals to all the
     * maximum version of all elements in the collection.
     * <p/>
     * Note! This method do not check all values in the hierarchy, only the elements
     * in the first collection found.
     */
    boolean isNewerOrSameVersionComparedWithExistingValues(V value);


    /**
     * @deprecated This interface should have business methods to retrieve the correct
     * entities based on desired version and validity version. NOT leaving this to the mapper.
     * Fixing this is part of:
     * TODO TOP2 https://github.com/opentripplanner/OpenTripPlanner/issues/2781
     *
     * @return a collection of all keys in the local map, all values added to one of the
     * parents are excluded from the collection.
     */
    Collection<String> localKeys();

    /**
     * @deprecated This interface should have business methods to retrieve the correct
     * entities based on desired version and validity version. NOT leaving this to the mapper.
     * Fixing this is part of:
     * TODO TOP2 https://github.com/opentripplanner/OpenTripPlanner/issues/2781
     *
     * Lookup element, if not found delegate up to the parent.
     * NB! elements of this class and its parents are NOT merged, the closest win.
     * @return an empty collection if no element are found.
     */
    @Deprecated
    Collection<V> lookup(String key);
}
