package org.opentripplanner.netex.index.api;

import java.util.Collection;

/**
 * Read only interface for a hierarchical map. This interface proved a interface to a hierarchy of
 * maps with a parent - child relationship. Elements are retrieved using ({@link #lookup(Object)}).
 * The lookup call check the current instance, then the parent to find an element. This continue
 * until the root of the hierarchy is reached. If a {@code key} exist in more than two places in
 * the hierarchy, the first value found wins.
 *
 * @param <K> The key type
 * @param <V> Thr value type
 */
public interface ReadOnlyHierarchicalMap<K,V> {

    /**
     * Lookup element, if not found delegate up to the parent.
     * NB! elements of this class and its parents are NOT merged, the closest win.
     * @return an empty collection if no element are found.
     */
    V lookup(K key);

    /**
     * The key exist in this Collection or one of the parents (parent, parent´s parent and so on)
     */
    boolean containsKey(K key);

    /**
     * @return a collection of all values hold in the local map, all values added to one of the
     * parents are excluded from the collection.
     */
    Collection<K> localKeys();


    /**
     * @return a collection of all values hold in the local map, all values added to one of the
     * parents are excluded from the collection.
     */
    Collection<V> localValues();

    /**
     * @return {@code true} if no local elements exist.
     */
    boolean localIsEmpty();
}
