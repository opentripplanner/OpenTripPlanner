package org.opentripplanner.analyst;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.primitives.Ints;

import java.io.IOException;
import java.io.Serializable;

import org.opentripplanner.analyst.core.WeightingFunction;

/**
 * A pair of parallel histograms representing how many features are located at each amount of travel time 
 * away from from a single origin. One array contains the raw counts of features (e.g. number of places of employment
 * M minutes from home) and the other array contains the weighted sums of those features accounting for their 
 * magnitudes (e.g. the number of jobs in all places of employment M minutes away from home).
 * All time values are rounded down into 1-minute bins (0-60 seconds = minute 0, 61-120 = min 1, etc.)
 */
public class Histogram implements Serializable {

    /**
     * The weighting functions to be used, as an array. Generally this will be an array of 120
     * functions, one to calculate cumulative accessibility for each minute in two hours.
     * But any additive function can be used, and the output of the functions will be places in
     * counts and sums parallel to this array.
     */
    public static WeightingFunction[] weightingFunctions;

    /**
     * The steepness of the logistic rolloff, basically a smoothing parameter.
     * Must be negative or your results will be backwards (i.e. jobs nearby will be worth less than jobs far away).
     * 
     * The larger it is in magnitude, the less smoothing. setting it to -2 / 60.0 yields a rolloff of about 5 minutes.
     */
    // TODO: should not be final, but that means that we need to rebuild the weighting functions when it is changed. 
    public static final double LOGISTIC_STEEPNESS = -2 / 60.0;

    static {
        weightingFunctions = new WeightingFunction[120];

        for (int i = 0; i < 120; i++) {
            weightingFunctions[i] = new WeightingFunction.Logistic((i + 1) * 60, LOGISTIC_STEEPNESS);
        }
    }

    /**
     * The number features that can be reached within each one-minute bin. Index 0 is 0-1 minutes, index 50 is 50-51 
     * minutes, etc. The features are not weighted by their magnitudes, so values represent (for example) the number of 
     * places of employment that can be reached rather than the total number of jobs in all those places of employment.
     */
    public final int[] counts;

    /**
     * The weighted sum of all features that can be reached within each one-minute bin.
     * Index 0 is 0-1 minutes, index 50 is 50-51 minutes, etc.
     * Features are weighted by their magnitudes, so values represent (for example) the total number of jobs in
     * all accessible places of employment, rather than the number of places of employment.
     */
    public final int[] sums;

    /**
     * Given parallel arrays of travel times and magnitudes for any number of destination features, construct 
     * histograms that represent the distribution of individual features and total opportunities as a function of
     * travel time. The length of the arrays containing these histograms will be equal to the maximum travel time
     * specified in the original search request, in minutes.
     * @param times the time at which each destination is reached. The array will be destructively sorted in place.
     * @param weights the weight or magnitude of each destination reached. it is parallel to times.
     */    
    public Histogram (int[] times, int[] weight) {
        int size = weightingFunctions.length;

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

        // we use logistic rolloff, so we want to compute the counts and sums using floating-point values before truncation
        double[] tmpCounts = new double[size];
        double[] tmpSums = new double[size];

        for (int i = 0; i < binnedCounts.length; i++) {
            for (int j = 0; j < weightingFunctions.length; j++) {
                double w = weightingFunctions[j].getWeight(i);
                tmpCounts[j] += w * binnedCounts[i];
                tmpSums[j] += w * binnedWeights[i];
            }
        }

        // convert to ints
        counts = new int[size];
        sums = new int[size];

        for (int i = 0; i < weightingFunctions.length; i++) {
            counts[i] = (int) Math.round(tmpCounts[i]);
            sums[i] = (int) Math.round(tmpSums[i]);
        }

        // make density rather than cumulative
        // note that counts[0] is already a density so we don't touch it
        for (int i = weightingFunctions.length - 1; i > 0; i--) {
            counts[i] -= counts[i - 1];
            sums[i] -= sums[i - 1];
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
