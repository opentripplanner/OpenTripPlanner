package org.opentripplanner.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

/**
 * A multimap, one key -> many values.
 * The list of values for a given key is guarantied to be sorted; hence the
 * values type V must be {@link Comparable}.
 * <p>
 * This class does not require the <code>K</code> type to implement
 * {@link Comparable} like {@link com.google.common.collect.SortedSetMultimap}.
 */
public class SortedMultimap<K, V extends Comparable<? super V>> {
    private Map<K, List<V>> map = new HashMap<>();

    /**
     * Return a unmodifiable, nullsafe list. An <em>empty</em>empty is returned if no
     * values exist for a given key.
     */
    public List<V> get(K key) {
        List<V> list = map.get(key);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);

    }

    public void addAll(Collection<V> values, Function<V, K> keyGenerator) {
        Set<K> keys = new HashSet<>();
        for (V value : values) {
            K key = keyGenerator.apply(value);
            keys.add(key);
            map.computeIfAbsent(key, trip -> new ArrayList<>()).add(value);
        }
        // Sort and updated stops for all keys touched.
        for (K key : keys) {
            Collections.sort(map.get(key));
        }
    }

    public void replace(K key, Collection<V> list) {
        map.replace(key, sort(list));
    }

    public void put(K key, Collection<V> list) {
        map.put(key, sort(list));
    }

    public Collection<V> values() {
        return map.values().stream().flatMap(Collection::stream).collect(toList());
    }

    public Map<K, List<V>> asMap() {
        HashMap<K, List<V>> newMap = new HashMap<>();
        for (Map.Entry<K, List<V>> e : map.entrySet()) {
            newMap.put(e.getKey(), e.getValue());
        }
        return newMap;
    }

    public int size() {
        return map.size();
    }

    public Set<K> keys() {
        return map.keySet();
    }


    /* private methods */

    private static <V extends Comparable<? super V>> List<V> sort(Collection<V> list) {
        List<V> values = new ArrayList<>(list);
        Collections.sort(values);
        return values;
    }

    public void reindex() {
        HashMap<K, List<V>> temp = new HashMap<>(map);
        map.clear();
        map.putAll(temp);
    }
}
