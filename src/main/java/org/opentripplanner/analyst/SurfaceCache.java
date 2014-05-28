package org.opentripplanner.analyst;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.util.List;
import java.util.Queue;

/**
 * Caches travel time surfaces, which are derived from shortest path trees.
 * TODO add LRU behavior upon get
 * TODO extend to store any type by moving the IDs into the cache
 * TODO use a disk-backed MapDB to avoid eating memory
 */
public class SurfaceCache {

    public static final int NONE = -1;
    public final List<TimeSurface> cache;
    public final int capacity;

    public SurfaceCache (int capacity) {
        this.cache = Lists.newArrayList();
        this.capacity = capacity;
    }

    public int add(TimeSurface surface) {
        synchronized (this) {
            if (cache.size() >= capacity) {
                cache.remove(0);
            }
            cache.add(surface);
        }
        return surface.id;
    }

    public TimeSurface get(int id) {
        for (TimeSurface surface : cache) {
            if (surface.id == id) {
                return surface;
            }
        }
        return null;
    }

}
