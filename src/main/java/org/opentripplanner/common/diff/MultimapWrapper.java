package org.opentripplanner.common.diff;

import com.google.common.collect.Multimap;

/**
 * Enable comparison of Guava Multimaps, treating them as maps from keys to Collections of values.
 * Created by abyrd on 2018-11-02
 */
public class MultimapWrapper extends MapComparisonWrapper {
    Multimap multimap;

    public MultimapWrapper(Multimap multimap) {
        this.multimap = multimap;
    }

    @Override
    public Iterable<?> allKeys() {
        return multimap.keySet();
    }

    @Override
    public boolean containsKey(Object key) {
        return multimap.containsKey(key);
    }

    @Override
    public Object get(Object key) {
        return multimap.get(key);
    }

}
