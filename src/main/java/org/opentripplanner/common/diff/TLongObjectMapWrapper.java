package org.opentripplanner.common.diff;

import com.google.common.primitives.Longs;
import gnu.trove.map.TLongObjectMap;

/**
 * Adapts a Trove Long-Object hash map to the minimal common Map interface used in comparisons.
 *
 * Created by abyrd on 2018-11-02
 */
class TLongObjectMapWrapper extends MapComparisonWrapper {

    private TLongObjectMap map;

    public TLongObjectMapWrapper(TLongObjectMap map) {
        this.map = map;
    }

    @Override
    public Iterable<?> allKeys() {
        return Longs.asList(map.keys());
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey((long) key);
    }

    @Override
    public Object get(Object key) {
        return map.get((long) key);
    }

}
