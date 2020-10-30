package org.opentripplanner.netex.index.hierarchy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;

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
 * @deprecated The use of this in the NetexIndex indicate that we are processing the entities in
 *             the load phase, and not just store it in an index and passing it to the
 *             validation/mapping phase. There might be a valid use-case for this class in the
 *             future in the mappers, so this should be checked before removing it.
 */
@Deprecated
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

    @Override
    public Collection<K> localKeys() {
        return map.keySet();
    }

    @Override
    public Collection<Collection<V>> localValues() {
        return map.asMap().values();
    }

    @Override
    Collection<V> localGet(K key) {
        return map.get(key);
    }

    @Override
    boolean localContainsKey(K key) {
        return map.containsKey(key);
    }

    @Override
    protected int localSize() {
        return map.size();
    }

    @Override
    void localRemove(K key) {
        map.removeAll(key);
    }
}
