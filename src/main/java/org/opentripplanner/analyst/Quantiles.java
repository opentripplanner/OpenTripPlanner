package org.opentripplanner.analyst;

import java.util.Arrays;

/**
 * A compact, lossy representation of a cumulative distribution.
 * The main point is to produce many-to-many travel time datasets that are linear rather than quadratic
 * in the number of origin and destination points.
 * With n=5, the breaks are: [min, q25, q50, q75, max] defining four quantiles (quartiles)
 *
 * There should probably be an interface for this that allows full OD matrices to be swapped in.
 */
public class Quantiles {

    public final int      count;
    public final double[] breaks;

    /**
     * Represent the distribution of the given times using n+1 numbers.
     * @param times the time at which each destination is reached. The array will be sorted in place.
     * @param nq the number of quantiles to produce. This number may be increased but will not be decreased.
     */
    public Quantiles (float[] times, int nq) {
        if (nq < 4) nq = 4; // must have at least 4 quantiles (quartiles)
        if (nq > 20) nq = 20;
        Arrays.sort(times);
        if (times[0] < 0) throw new AssertionError();
        if (times[0] == Float.POSITIVE_INFINITY) count = 0;
        else {
            // We need to remove any INFs (unreachable) from the end of the array
            int lastIdx = Arrays.binarySearch(times, Float.POSITIVE_INFINITY);
            // If any INFs were found, decrement index to the last index that is non-INF
            while (lastIdx >= 0 && times[lastIdx] == Float.POSITIVE_INFINITY) lastIdx--;
            count = (lastIdx >= 0) ? lastIdx + 1 : times.length;
        }
        if (count < 2) throw new RuntimeException("Less than two times."); // bail out, algo will fail. maybe this should be a factory method.
        breaks = new double[nq + 1];
        // So: (breaks.length - 1) * step == count - 1, or the last index of times.
        double step = (count - 1) / (double) nq;
        for (int br = 0; br < breaks.length - 1; br++) {
            double position = br * step;
            int index = (int) position; // integer index
            double frac = position - index; // fractional index remainder
            float low = times[index];
            float high = times[index + 1];
            float range = high - low;
            breaks[br] = (int)(low + (frac * range));
        }
        breaks[breaks.length - 1] = times[count - 1]; // final break is max time at which a destination was reached

        // TODO: Here, while we still have the sorted original values and the breaks, we should
        // estimate the cumulative distribution at all points and compute RMS error.
        // beyond a certain error threshold, we should increase the number of breakpoints and re-compute them.
        // i.e. adaptive sizing of the array to avoid excessive error.
    }

    /** @return how many destinations would be found at or below x using linear interpolation. */
    public double evaluate (double x) {
        if (x < breaks[0]) {
            return 0; // all points fall above the given x
        }
        for (int i = 0; i < breaks.length - 1; i++) {
            double low  = breaks[i];
            double high = breaks[i + 1];
            if (x < high) {
                // x is in this slice (x >= low because the breaks are sorted)
                double range = high - low;
                double fraction = (x - low) / range;
                int n_slices = breaks.length - 1;
                return ((i + fraction) / n_slices) * count;
            }
        }
        return count; // all points fall below the given x
    }

}
