package org.opentripplanner.netex.loader.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * A concrete multimap implementation of {@link AbstractHierarchicalMap}.
 * </p>
 * <p>
 * Note that the collection retuned by the {@link ReadOnlyHierarchicalMap#lookup(Object)} is
 * not <em>ReadOnly</em>, but should be treated as such. It is just to painful to achieve this with the
 * current verion of the Java Collection API - without any side-effects.
 *</p>
 * @param <K> the key type
 * @param <V> the base value type
 */
public class HierarchicalMultimap<K,V> extends AbstractHierarchicalMap<K, Collection<V>> {
    private final Multimap<K,V> map  = ArrayListMultimap.create();

    /** Create a root for the hierarchy */
    public HierarchicalMultimap() {
        super(null);
    }

    /** Create a child of the given {@code parent}. */
    public HierarchicalMultimap(HierarchicalMultimap<K, V> parent) {
        super(parent);
    }

    /** Add a new pair of {@code key & value} to the local map value collection. */
    public void add(K key, V value) {
        map.put(key, value);
    }

    /** Add a multimap to the local map */
    public void addAll(Multimap<K, V> other) {
        for (Map.Entry<K, V> it : other.entries()) {
            add(it.getKey(), it.getValue());
        }
    }

    Collection<K> localKeys() {
        return map.keySet();
    }

    @Override
    Collection<V> localGet(K key) {
        return map.get(key);
    }

    @Override
    boolean localContainsKey(K key) {
        return map.containsKey(key);
    }
}
