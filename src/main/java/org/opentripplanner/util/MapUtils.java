package org.opentripplanner.util;

import gnu.trove.map.TLongObjectMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapUtils {

    public static final <U> void addToMapSet(TLongObjectMap<Set<U>> mapSet, long key, U value) {
        Set<U> set = mapSet.get(key);
        if (set == null) {
            set = new HashSet<U>();
            mapSet.put(key, set);
        }
        set.add(value);
    }

    public static final <T, U> void addToMapList(Map<T, List<U>> mapList, T key, U value) {
        List<U> list = mapList.get(key);
        if (list == null) {
            list = new ArrayList<U>();
            mapList.put(key, list);
        }
        list.add(value);
    }

    public static final <T> boolean addToMaxMap(Map<T, Double> map, T key, double value) {
        Double oldValue = map.get(key);
        if (oldValue == null || value > oldValue) {
            map.put(key, value);
            return true;
        }
        return false;
    }

    public static <T, U> void addToMapListUnique(Map<T, List<U>> mapList,
            T key, List<U> values) {
        List<U> list = mapList.get(key);
        if (list == null) {
            list = new ArrayList<U>(values.size());
            mapList.put(key, list);
        }
        for (U value : values) {
            if (!list.contains(value)) {
                list.add(value);
            }
        }
    }

    public static <T, U, V extends Collection<U>> void mergeInUnique(Map<T, V> mapList,
            Map<T, V> from) {
        for (Map.Entry<T, V> entry : from.entrySet()) {
            T key = entry.getKey();
            V value = entry.getValue();
            V originalValue = mapList.get(key);
            if (originalValue != null) {
                HashSet<U> originalSet = new HashSet<U>(originalValue);
                for (U item : value) {
                    if (!originalSet.contains(item))
                        originalValue.add(item);
                }
            } else {
                mapList.put(key, value);
            }
        }
    }

    /**
     * Map a collection of objects of type <em>S</em> to a list of type <em>T</em> using the
     * provided mapping function.
     * <p>
     * Nullsafe: if <em>entities</em> is <code>null</code>, then <code>null</code> is returned.
     */
    public static <S, T> List<T> mapToList(Collection<S> entities, Function<S, T> mapper) {
        return entities == null ? null : entities.stream().map(mapper).collect(Collectors.toList());
    }
}
