package org.opentripplanner.routing.trippattern;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.joda.time.LocalDate;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;

import java.util.BitSet;
import java.util.Map;

/**
 * Retains TripTimeSubsets once they are constructed so they can be reused between Analyst or Modeify profile routing
 * calls. This cache is stored in the graph itself (rather than the Router) so that it doesn't retain references to
 * outdated TripPatterns and service codes when the graph is reloaded.
 */
public class TripTimeSubsetCache {

    private final Graph graph;
    private final LoadingCache<CacheKey, Map<TripPattern, TripTimeSubset>> cache;

    public TripTimeSubsetCache(final Graph graph) {
        this.graph = graph;
        this.cache = CacheBuilder.newBuilder().maximumSize(1000).build(
            new CacheLoader<CacheKey, Map<TripPattern, TripTimeSubset>>() {
                @Override
                public Map<TripPattern, TripTimeSubset> load(CacheKey key) throws Exception {
                    return TripTimeSubset.indexGraph(graph, key.servicesRunning, key.startTime, key.endTime);
                }
            }
        );
    }

    /**
     * Using fixed-width time windows in the caller should reduce the number of cached TripTimeSubsets.
     * More than one LocalDate may map to the same set of service codes, which means two different dates can
     * generate semantically equal keys, and therefore their TripTimeSubsets will be shared.
     *
     * @param date a date which will be used to determine which transit services are running
     * @param startTime the beginning of the time window in seconds after midnight
     * @return either a new TripTimeSubset instance or a cached one if it already exists
     */
    public Map<TripPattern, TripTimeSubset> getOrMake(LocalDate date, int startTime, int endTime) {
        BitSet servicesRunning = graph.index.servicesRunning(date);
        CacheKey key = new CacheKey(servicesRunning, startTime, endTime);
        try {
            return cache.get(key);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static class CacheKey {

        private BitSet servicesRunning;
        int startTime, endTime;

        public CacheKey(BitSet servicesRunning, int startTime, int endTime) {
            this.servicesRunning = servicesRunning;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            if (endTime != cacheKey.endTime) return false;
            if (startTime != cacheKey.startTime) return false;
            if (!servicesRunning.equals(cacheKey.servicesRunning)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = servicesRunning.hashCode();
            result = 31 * result + startTime;
            result = 31 * result + endTime;
            return result;
        }
    }

}
