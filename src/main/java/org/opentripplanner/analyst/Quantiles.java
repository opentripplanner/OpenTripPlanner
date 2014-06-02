package org.opentripplanner.analyst;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

/**
 * A compact, lossy representation of a cumulative distribution.
 * The main point is to produce many-to-many travel time datasets that are linear rather than quadratic
 * in the number of origin and destination points.
 * With n=5, the breaks are: [min, q25, q50, q75, max] defining four quantiles (quartiles)
 *
 * There should probably be an interface for this that allows full OD matrices to be swapped in.
 */
public class Quantiles {

    public final int   count;
    public final int[] breaks;

    // TODO allow specifying breaks

    /**
     * Represent the distribution of the given times using n+1 numbers.
     * @param times the time at which each destination is reached. The array will be sorted in place.
     * @param weights the weight or magnitude of each destination reached. parallel to times.
     * @param nq the number of quantiles to produce. This number may be increased but will not be decreased.
     */
    public Quantiles (int[] times, int[] weights, int nq) {
        if (nq < 4) nq = 4; // must have at least 4 quantiles (quartiles)
        if (nq > 20) nq = 20;
        if (times.length != weights.length) throw new AssertionError();
        List<TimeWeight> tws = Lists.newArrayListWithCapacity(times.length);
        for (int i = 0; i < times.length; i++) {
            TimeWeight tw = new TimeWeight();
            tw.weight = weights[i];
            tw.time = times[i];
            tws.add(tw);
        }
        Collections.sort(tws);
        if (tws.get(0).time < 0) throw new AssertionError("Negative time.");
        // do one loop through the array summing up the weights and finding the length without INFs
        int sum = 0;
        int len = 0;
        for (TimeWeight tw : tws) {
            if (tw.time == Integer.MAX_VALUE) break;
            sum += tw.weight;
            len += 1;
        }
        count = sum;
        breaks = new int[nq + 1];
        if (len < 2) return; // algorithm needs at least 2 elements to work
        tws = tws.subList(0, len);
        // Min and max time to reach an opportunity can be simply read off from the sorted array.
        TimeWeight tw0 = tws.get(0); // a pair of successive tws
        TimeWeight tw1 = tws.get(1);
        breaks[0] = tw0.time;
        breaks[nq] = tws.get(tws.size() - 1).time;
        int cw0 = 0, cw1 = -1; // cumulative weights
        // Loop over other intermediate quantiles. -1 ensures that 'while' executes on first iteration.
        for (int q = 1, wdi = 0; q < nq; q++) {
            // Determine the cumulative weight value for which we want to read off the time.
            float cw = ((float) q) * sum / nq;
            // Accumulate weights until we reach the first pair that includes the target value.
            while (cw1 < cw) {
                wdi += 1;
                tw0 = tws.get(wdi);
                tw1 = tws.get(wdi + 1);
                cw0 += tw0.weight; // accumulate weight
                cw1 = cw0 + tw1.weight;
            }
            // Linear interpolation. Range of cumulative values is the same as the weight of tw1 (see assignment above).
            float frac = (cw - cw0) / tw1.weight;
            float t = tw0.time + (frac * (tw1.time - tw0.time));
            breaks[q] = (int) t;
        }
        // TODO: Here, while we still have the sorted original values and the breaks, we should
        // estimate the cumulative distribution at all points and compute RMS error.
        // beyond a certain error threshold, we should increase the number of breakpoints and re-compute them.
        // i.e. adaptive sizing of the array to avoid excessive error.
    }

    /** A certain magnitude or number of opportunities at a certain distance away from the origin. */
    private static class TimeWeight implements Comparable<TimeWeight> {
        int time;
        int weight;
        @Override
        public int compareTo(TimeWeight other) {
            return Float.compare(this.time, other.time);
        }
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
