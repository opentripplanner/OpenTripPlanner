package org.opentripplanner.netex.loader.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Set;

public class HierarchicalMultimap<K,V> extends AbstractHierarchicalMap<K, Collection<V>> {
    private Multimap<K,V> map  = ArrayListMultimap.create();

    /** Create a root for the hierarchy */
    public HierarchicalMultimap() {
        super(null);
    }

    /** Create a child of the given {@code parent}. */
    public HierarchicalMultimap(HierarchicalMultimap<K, V> parent) {
        super(parent);
    }

    public void add(K key, V value) {
        map.put(key, value);
    }

    public Set<K> localKeys() {
        return map.keySet();
    }

    @Override
    protected Collection<V> localGet(K key) {
        return map.get(key);
    }

    @Override
    protected boolean localContainsKey(K key) {
        return map.containsKey(key);
    }
}
