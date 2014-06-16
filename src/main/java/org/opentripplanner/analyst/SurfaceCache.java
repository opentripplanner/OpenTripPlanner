package org.opentripplanner.analyst;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import org.opentripplanner.routing.spt.ShortestPathTree;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Caches travel time surfaces, which are derived from shortest path trees.
 * TODO add LRU behavior upon get
 * TODO extend to store any type by moving the IDs into the cache
 * TODO use a disk-backed MapDB to avoid eating memory
 */
public class SurfaceCache {

    public static final int NONE = -1;
    public final Cache<Integer, TimeSurface> cache;

    public SurfaceCache (int capacity) {
        this.cache = CacheBuilder.newBuilder()
        	       		.maximumSize(100)
        	       		.build();
    }

    public int add(TimeSurface surface) {
    	this.cache.put(surface.id, surface);
    	return surface.id;
    }

    public TimeSurface get(int id) {
        return this.cache.getIfPresent(id);
    }

}
