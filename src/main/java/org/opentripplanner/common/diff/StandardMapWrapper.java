package org.opentripplanner.common.diff;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This adapts all subclasses of Map to share an interface with some other Map-like classes, enabling a single method
 * to compare them all.
 *
 * Created by abyrd on 2018-11-02
 */
class StandardMapWrapper extends MapComparisonWrapper {

    private Map map;

    public StandardMapWrapper(Map map) {
        this.map = map;
    }

    @Override
    public Iterable<?> allKeys() {
        if (map instanceof LinkedHashMap) {
            // Avoid concurrent modification exception when comparing map to itself.
            // "In access-ordered linked hash maps, merely querying the map with get is a structural modification."
            // This will cause a ConcurrentModificationException if you get() while iterating.
            return new ArrayList<>(map.keySet());
        } else {
            return map.keySet();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public Object get(Object key) {
        return map.get(key);
    }

}
