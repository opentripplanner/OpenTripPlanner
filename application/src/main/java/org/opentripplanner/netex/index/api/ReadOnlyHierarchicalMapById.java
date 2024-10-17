package org.opentripplanner.netex.index.api;

import org.rutebanken.netex.model.EntityStructure;

/**
 * Read only interface for a hierarchical map of {@link EntityStructure} values indexed by their
 * id.
 *
 * @param <V> the value type
 */
public interface ReadOnlyHierarchicalMapById<V> extends ReadOnlyHierarchicalMap<String, V> {}
