package org.opentripplanner.netex.index.hierarchy;

import org.rutebanken.netex.model.EntityStructure;

import java.util.Collection;

/**
 * Read only interface for a hierarchical map of {@link EntityStructure} values indexed by their id.
 *
 * @param <V> the value type
 */
public interface ReadOnlyHierarchicalMapById<V> extends ReadOnlyHierarchicalMap<String, V>{

    /**
     * @return a collection of all values hold in the local map, all values added to one of the
     * parents are excluded from the collection.
     */
    Collection<V> localValues();
}
