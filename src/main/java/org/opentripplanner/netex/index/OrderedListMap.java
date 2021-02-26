package org.opentripplanner.netex.index;

import java.util.HashMap;
import java.util.List;

/**
 * A map of ordered lists. The client is responsible for the order of the list of values added.
 *
 * @param <K> the key type
 * @param <V> the list element type
 */
public class OrderedListMap<K, V> extends HashMap<K, List<V>> {

    /**
     * Get the list of values for the given {@code key} and iterate over the list to find the
     * <em>index</em> of the given {@code value}.
     * <p>
     * Return the index of the {@code value} in list, start at zero (0), and return {@code -1} if
     * the {@code key} do not exist and {@code -2} if the {@code value} do not exist.
     */
    public int index(K key, V value) {
        List<V> values = get(key);

        if (values == null) { return -1; }

        for (int i = 0; i < values.size(); i++) {
            if (value.equals(values.get(i))) { return i; }
        }
        return -2;
    }
}
