package org.opentripplanner.analyst.core;

import org.apache.commons.math3.util.FastMath;

/**
 * A weighting function, expressing how influential something is according to its distance from you.
 *
 * Perhaps this could be a FunctionalInterface so implementations can be defined using Java 8 shorthand, but it
 * currently has two functions. The separation into two functions was to allow optimizations for simple hard-cutoff
 * functions, where you can avoid doing a multiplication to determine the final result. I'm not sure eliminating
 * multiplications by 0 and 1 actually has much of an effect on execution speed, so maybe the functional abstraction
 * is more important. But then again, maybe we'll always just use the same few functions, in which case the abstraction
 * is purely to aid comprehension by OTP developers.
 */
public abstract class WeightingFunction {

    /**
     * Weight the counts (binned by second, so countsPerSecond[i] is destinations reachable in i - i + seconds)
     * and return the output of this weighting function as a _cumulative distribution_.
     */
    public abstract int[] apply (int[] countsPerSecond);

    /**
     * The logistic function, a commonly used sigmoid (s-shaped function).
     * Basically a step function with a smooth rolloff.
     * Positive values of k cause the function to increase from zero to one, negative values will be inverted.
     * <br>
     * k=-1 / 60 gives a rolloff over an interval of ~10 minutes<br>
     * k=-0.5 / 60 gives a rolloff over an interval of ~20 minutes<br>
     * k=-2 / 60 contracts the rolloff interval to ~5 minutes<br>
     */
    public static final class Logistic extends WeightingFunction {

        double x0;  // cutoff point
        double k;   // steepness of the curve. negative

        // the seconds at which the rolloff starts and ends.
        // (where the weight is equal to 0.999 and 0.001, respectively)
        int rolloffMin, rolloffMax;

        // lookup table for weights
        double[] weights;

        public Logistic (double steepness) {
            this.k = steepness;

            // ln(1/epsilon - 1) / -k is the value at which the weight takes on the value epsilon
            this.rolloffMin = (int) (Math.log(1 / 0.999 - 1) / -k);
            this.rolloffMax = (int) (Math.log(1 / 0.001 - 1) / -k);

            // make a lookup table for the weights, once everything has been normalized by the cutoff
            weights = new double[rolloffMax - rolloffMin + 1];
            for (int i = rolloffMin; i <= rolloffMax; i++) {
                weights[i - rolloffMin] = 1 / (1 + Math.exp( -k * (i - x0)));
            }
        }

        /**
         * Apply the logistic weighting function. Uses some slightly clever optimizations.
         * We have precomputed the weights for the sigmoid centered on 0 and stored them in the weights array,
         * offset by -rolloffMin. We loop over the values to output and first find the largest
         * second where the weight is effectively 1. We store a cumulative sum up to that point.
         * We then run through the next
         */
        @Override public int[] apply(int[] countsPerSecond) {
            int len = countsPerSecond.length / 60;

            // the frontier is the highest index for which the weight is effectively 1
            // (in seconds)
            int frontier = 0;
            // cumulative sum up to the frontier
            int valueAtFrontier = 0;

            int[] ret = new int[len];

            for (int i = 0; i < len; i++) {
                int newFrontier = Math.max(0, (i * 60) + rolloffMin);

                for (int j = frontier; j < newFrontier; j++) {
                    valueAtFrontier += countsPerSecond[j];
                }

                double sum = valueAtFrontier;

                for (int k = newFrontier; k < i * 60 + rolloffMax + 1 && k < countsPerSecond.length; k++) {
                    sum += weights[k - (i * 60) - rolloffMin] * countsPerSecond[k];
                }

                // cast to int here rather than making a cumulative curve so that int roundoff error is never greater than 1.
                // if we had a douible array and cast the differences to ints, we could accumulate roundoff error up to the number of minutes.
                ret[i] = (int) sum;

                frontier = newFrontier;
            }

            // make cumulative curve
            for (int i = ret.length - 1; i > 0; i--) {
                ret[i] -= ret[i - 1];
            }

            return ret;
        }
    }

    /**
     * The typical hard cutoff used to select all objects within an isochrone curve.
     * WARNING: This can have nefarious effects on accessibility indicators when the opportunity density surface
     * contains steep gradients (e.g. Manhattan).
     * Any misalignment in the before/after travel time surfaces near these gradients (for example due to street length
     * roundoff errors) will create a sharp bidirectional fringe x0 minutes away in the resulting
     * accessibility indicator surface, frequently in outlying low-accessibility areas where the echo will overwhelm
     * the local indicator value, showing impossible decreases in accessibility in a clearly superior network.
     */
    public static class SharpCutoff extends WeightingFunction {

        @Override public int[] apply(int[] countsPerSecond) {
            int len = countsPerSecond.length / 60;

            int[] ret = new int[len];

            int cumulative = 0;

            for (int i = 0; i < countsPerSecond.length; i++) {
                cumulative += countsPerSecond[i];

                if (i % 60 == 59) {
                    ret[(int) FastMath.floor(i / 60)] = cumulative;
                }
            }

            // make cumulative curve
            for (int i = ret.length - 1; i > 0; i--) {
                ret[i] -= ret[i - 1];
            }

            return ret;
        }
    }
}
