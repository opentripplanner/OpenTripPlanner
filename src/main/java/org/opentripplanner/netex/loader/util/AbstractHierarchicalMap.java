package org.opentripplanner.netex.loader.util;

/**
 * Base class for a hierarchical map. This class proved a way to create a hierarchy of maps with
 * a parent - child relationship. Elements must be added to the right level (map instance), but
 * when retrieving values({@link #lookup(Object)}) the lookup call check the current instance, then
 * ask the parent. This continue until the root of the hierarchy is reached. If a {@code key}
 * exist in more than two places in the hierarchy, the first value found wins.
 * <p/>
 * There is no reference from the parent to the child, enableing garbage collection of children,
 * when not referenced by the outer context any more.
 *
 * @param <K> The key type
 * @param <V> Thr value type
 */
public abstract class AbstractHierarchicalMap<K,V> implements ReadOnlyHierarchicalMap<K,V> {

    private final AbstractHierarchicalMap<K,V> parent;

    AbstractHierarchicalMap(AbstractHierarchicalMap<K, V> parent) {
        this.parent = parent;
    }

    /**
     * Lookup element, if not found delegate up to the parent.
     * NB! elements of this class and its parents are NOT merged, the closest win.
     * @return an empty collection if no element are found.
     */
    public V lookup(K key) {
        return (localContainsKey(key) || isRoot()) ? localGet(key) : parent.lookup(key);
    }

    /**
     * The key exist in this Collection or one of the parents (parent, parent´s parent and so on)
     */
    public boolean containsKey(K key) {
        return localContainsKey(key) || parentContainsKey(key);
    }

    /** Get value from 'local' map, parent is not queried. */
    protected abstract V localGet(K key);

    /** Check if key exist in 'local' map, parent is not queried. */
    protected abstract boolean localContainsKey(K key);


    /* private methods */

    /** Return true if this instance have a parent. */
    private boolean isRoot() {
        return parent == null;
    }

    /**
     * Return true if the {@code key} exist in one of the
     * parents (parent, parent´s parent and so on).
     */
    private boolean parentContainsKey(K key) {
        return parent != null && parent.containsKey(key);
    }
}


