package org.opentripplanner.analyst;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

/**
 * A histogram summing/counting all time values into 1-minute bins (0-60 seconds = min 1, 61-120 = min 2, etc.)
 */
public class Histogram implements Serializable {

    /**
     * The number of accessible features in each one-minute bin.
     * Index 0 is 0-1 minutes, index 50 is 50-51 minutes, etc.
     * The features are not weighted by their magnitudes, so values represent (for example) the number of places of
     * employment that can be reached rather than the total number of jobs in all those places of employment.
     */
    public final int[] counts;

    /**
     * The weighted sum of all accessible features in each one-minute bin.
     * Index 0 is 0-1 minutes, index 50 is 50-51 minutes, etc.
     * Features are weighted by their magnitudes, so values represent (for example) the total number of jobs in
     * all accessible places of employment, rather than the number of places of employment.
     */
    public final int[] sums;

    /**
     * Represent the distribution of the given times using n+1 numbers.
     *
     * @param times   the time at which each destination is reached. The array will be sorted in place.
     * @param weights the weight or magnitude of each destination reached. parallel to times.
     */
    public Histogram(int[] times, int[] weights, int nBins) {

        counts = new int[nBins];
        sums = new int[nBins];

        for (int i = 0; i < times.length; i++) {

            if (times[i] < 0 || times[i] == Integer.MAX_VALUE) {
                continue;
            }

            int minuteBin = (int) Math.floor(times[i] / 60.0);
            if (minuteBin >= nBins) {
                continue;
            }

            counts[minuteBin] += 1;
            sums[minuteBin] += weights[i];

        }

    }

    public void writeJson(JsonGenerator jgen) throws JsonGenerationException, IOException {

        jgen.writeArrayFieldStart("sums");
        {
            for (int sum : sums) {
                jgen.writeNumber(sum);
            }
        }
        jgen.writeEndArray();

        jgen.writeArrayFieldStart("counts");
        {
            for (int count : counts) {
                jgen.writeNumber(count);
            }
        }
        jgen.writeEndArray();
    }
}
