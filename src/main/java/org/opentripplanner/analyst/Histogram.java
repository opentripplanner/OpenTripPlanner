package org.opentripplanner.analyst;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import org.opentripplanner.analyst.core.WeightingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * A pair of parallel histograms representing how many features are located at each amount of travel time 
 * away from from a single origin. One array contains the raw counts of features (e.g. number of places of employment
 * M minutes from home) and the other array contains the weighted sums of those features accounting for their 
 * magnitudes (e.g. the number of jobs in all places of employment M minutes away from home).
 * All time values are rounded down into 1-minute bins (0-60 seconds = minute 0, 61-120 = min 1, etc.)
 */
public class Histogram implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(Histogram.class);

    public static final double LOGISTIC_STEEPNESS = -2 / 60.0;

    /**
     * The weighting functions to be used, as an array. Generally this will be an array of 120
     * functions, one to calculate cumulative accessibility for each minute in two hours.
     * But any additive function can be used, and the output of the functions will be places in
     * counts and sums parallel to this array.
     */
    public static WeightingFunction weightingFunction = new WeightingFunction.Logistic(LOGISTIC_STEEPNESS);

    /**
     * The number features that can be reached within each one-minute bin. Index 0 is 0-1 minutes, index 50 is 50-51 
     * minutes, etc. The features are not weighted by their magnitudes, so values represent (for example) the number of 
     * places of employment that can be reached rather than the total number of jobs in all those places of employment.
     */
    public int[] counts;

    /**
     * The weighted sum of all features that can be reached within each one-minute bin.
     * Index 0 is 0-1 minutes, index 50 is 50-51 minutes, etc.
     * Features are weighted by their magnitudes, so values represent (for example) the total number of jobs in
     * all accessible places of employment, rather than the number of places of employment.
     */
    public int[] sums;

    /**
     * Given parallel arrays of travel times and magnitudes for any number of destination features, construct 
     * histograms that represent the distribution of individual features and total opportunities as a function of
     * travel time. The length of the arrays containing these histograms will be equal to the maximum travel time
     * specified in the original search request, in minutes.
     * @param times the time at which each destination is reached. The array will be destructively sorted in place.
     * @param weight the weight or magnitude of each destination reached. it is parallel to times.
     */    
    public Histogram (int[] times, int[] weight) {
        // optimization: bin times and weights by seconds.
        // there will often be more than one destination in a seconds due to the pigeonhole principle:
        // there are a lot more destinations than there are seconds
        int maxSecs = Integer.MIN_VALUE;

        for (int time : times) {
            if (time == Integer.MAX_VALUE)
                continue;

            if (time > maxSecs)
                maxSecs = time;
        }

        int[] binnedCounts = new int[maxSecs + 1];
        int[] binnedWeights = new int[maxSecs + 1];

        for (int i = 0; i < times.length; i++) {
            if (times[i] == Integer.MAX_VALUE)
                continue;

            binnedCounts[times[i]] += 1;
            binnedWeights[times[i]] += weight[i];
        }

        counts = weightingFunction.apply(binnedCounts);
        sums = weightingFunction.apply(binnedWeights);
    }

    /** no-arg constructor for serialization/deserialization */
    public Histogram () {}

    public static Map<String, Histogram> buildAll (int[] times, PointSet targets) {
        try {
            // bin counts and all properties
            int size = IntStream.of(times).reduce(0, (memo, i) -> i != Integer.MAX_VALUE ? Math.max(i, memo) : memo) + 1;

            int[] binnedCounts = new int[size];

            // use an identity hash map so that lookups are speedy - we only need this in the context of
            // this function
            Map<String, int[]> binnedProperties = new IdentityHashMap<>();
            for (String key : targets.properties.keySet()) {
                binnedProperties.put(key, new int[size]);
            }

            for (int fidx = 0; fidx < times.length; fidx++) {
                if (times[fidx] == Integer.MAX_VALUE)
                    continue;

                binnedCounts[times[fidx]] += 1;

                for (Map.Entry<String, int[]> prop : targets.properties.entrySet()) {
                    binnedProperties.get(prop.getKey())[times[fidx]] += prop.getValue()[fidx];
                }
            }

            // make the histograms
            // counts are the same for all histograms
            int[] counts = weightingFunction.apply(binnedCounts);

            Map<String, Histogram> ret = new HashMap<>();

            for (Map.Entry<String, int[]> e : binnedProperties.entrySet()) {
                Histogram h = new Histogram();
                h.counts = counts;
                h.sums = weightingFunction.apply(e.getValue());
                ret.put(e.getKey(), h);
            }

            return ret;
        } catch (Exception e) {
            LOG.error("Error constructing histograms", e);
            return null;
        }
    }

    /**
     * Serialize this pair of histograms out as a JSON document using the given JsonGenerator. The format is:
     * <pre> {
     *   sums: [],
     *   counts: []     
     * } </pre> 
     */
    public void writeJson(JsonGenerator jgen) throws JsonGenerationException, IOException {
        // The number of features reached during each minute, ignoring their magnitudes
        jgen.writeArrayFieldStart("sums"); {
            for(int sum : sums) {
                jgen.writeNumber(sum);
            }
        }
        jgen.writeEndArray();
        // The total number of opportunities reached during each minute (the sum of the features' magnitudes)
        jgen.writeArrayFieldStart("counts"); {
            for(int count : counts) {
                jgen.writeNumber(count);
            }
        }
        jgen.writeEndArray();
    }
}
