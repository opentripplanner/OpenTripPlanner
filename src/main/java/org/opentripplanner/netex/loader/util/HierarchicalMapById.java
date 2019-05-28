package org.opentripplanner.netex.loader.util;

import org.rutebanken.netex.model.EntityStructure;

/**
 * A hierarchical map of {@link EntityStructure} values indexed by their id. Use the
 * one argument {@link #add(EntityStructure)} method to add elements to the map.
 *
 * @param <V> the value type
 */
public class HierarchicalMapById<V extends EntityStructure> extends HierarchicalMap<String, V> {

    /** Create a root for the hierarchy */
    public HierarchicalMapById() {
    }

    /** Create a child of the given {@code parent}. */
    public HierarchicalMapById(HierarchicalMap<String, V> parent) {
        super(parent);
    }

    /**
     * Add an entity and use its {@code id} as key to index it.
     */
    public void add(V entity) {
        super.add(entity.getId(), entity);
    }

    /**
     * Use the {@link #add(EntityStructure)} method!
     * @throws  IllegalArgumentException This method throws an exception
     * to prevent adding elements with a key different than the element id.
     */
    @Override
    public void add(String key, V value) {
        throw new IllegalArgumentException("Use the add method with just one argument instead.");
    }
}
