package org.opentripplanner.netex.loader.util;

import org.rutebanken.netex.model.EntityStructure;

public class HierarchicalMapById<V extends EntityStructure> extends HierarchicalMap<String, V> {

    /** Create a root for the hierarchy */
    public HierarchicalMapById() {
    }

    /** Create a child of the given {@code parent}. */
    public HierarchicalMapById(HierarchicalMap<String, V> parent) {
        super(parent);
    }

    /**
     * Add an entity and use its Key to index it.
     */
    public void add(V entity) {
        super.add(entity.getId(), entity);
    }

    /**
     * Use the {@link #add(EntityStructure)} method, this method throws an exception.
     * @throws IllegalArgumentException every time it is called.
     */
    @Override
    public void add(String key, V value) {
        throw new IllegalArgumentException(
                "Use the add method with just one argument instead of this method."
        );
    }
}
