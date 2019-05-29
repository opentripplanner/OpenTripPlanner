package org.opentripplanner.netex.loader.util;

import org.rutebanken.netex.model.EntityInVersionStructure;

import static org.opentripplanner.netex.support.NetexVersionHelper.lastestVersionedElementIn;
import static org.opentripplanner.netex.support.NetexVersionHelper.latestVersionIn;
import static org.opentripplanner.netex.support.NetexVersionHelper.versionOf;

/**
 * A hierarchical multimap indexing a collections of {@link EntityInVersionStructure} values by
 * their {@code id}. Use the one argument {@link #add(EntityInVersionStructure)} method to add
 * elements to the map.
 *
 * @param <V> the value type
 */
public class HierarchicalMultimapById<V extends EntityInVersionStructure>
        extends HierarchicalMultimap<String, V> {

    /** Create a root for the hierarchy */
    public HierarchicalMultimapById() { }

    /** Create a child of the given {@code parent}. */
    public HierarchicalMultimapById(HierarchicalMultimapById<V> parent) {
        super(parent);
    }

    /**
     * Add an entity and use its Id as key to index it.
     */
    public void add(V entity) {
        super.add(entity.getId(), entity);
    }

    /**
     * Use the {@link #add(EntityInVersionStructure)} method!
     * @throws IllegalArgumentException This method throws an exception to prevent adding elements
     *                                  with a key different than the element id.
     */
    @Override
    public void add(String key, V value) {
        throw new IllegalArgumentException("Use the add method with just one argument instead.");
    }

    /**
     * Return the element with the latest version with the given {@code id}. Returns
     * {@code null} if not element is found.
     */
    public V lookupLastVersionById(String id) {
        return lastestVersionedElementIn(lookup(id));
    }

    /**
     * Return {@code true} if the given {@code value.version} is larger than all elements
     * in the collection of elements returned using the {@link #lookup(Object)} method.
     * <p/>
     * Note! This method do not check all values in the hierarchy, only the elements
     * in the first collection found.
     */
    public boolean isNewLatestVersion(V value) {
        return versionOf(value) > latestVersionIn(lookup(value.getId()));
    }
}
