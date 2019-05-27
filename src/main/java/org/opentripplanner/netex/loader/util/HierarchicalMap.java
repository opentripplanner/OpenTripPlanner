package org.opentripplanner.netex.loader.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A concrete implementation of {@link AbstractHierarchicalMap}.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class HierarchicalMap<K,V> extends AbstractHierarchicalMap<K, V> {
    private Map<K,V> map  = new HashMap<>();

    /** Create a new hierarchical map root. */
    public HierarchicalMap() {
        super(null);
    }

    /** Create new child map with the given {@code parent}. */
    public HierarchicalMap(HierarchicalMap<K, V> parent) {
        super(parent);
    }

    /**
     * Add a new pair of {@code key & value} to the map instance.
     */
    public void add(K key, V value) {
        map.put(key, value);
    }

    /**
     * @return a collection of all values hold in the local map, all values added to one of the
     * parents are excluded from the collection.
     */
    public Collection<V> localValues() {
        return map.values();
    }

    @Override
    protected V localGet(K key) {
        return map.get(key);
    }

    @Override
    protected boolean localContainsKey(K key) {
        return map.containsKey(key);
    }

}
