package org.opentripplanner.analyst.core;

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

    public abstract double getWeight (int seconds);

    public double applyWeight (double n, int seconds) {
        return n * getWeight(seconds);
    }

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

        public Logistic (int cutoff, double steepness) {
            this.x0 = cutoff;
            this.k = steepness;

            // ln(1/epsilon - 1) / -k is the value at which the weight takes on the value epsilon
            this.rolloffMin = (int) (x0 + Math.log(1 / 0.999 - 1) / -k);
            this.rolloffMax = (int) (x0 + Math.log(1 / 0.001 - 1) / -k);

            // make a lookup table for the weights
            weights = new double[rolloffMax - rolloffMin + 1];
            for (int i = rolloffMin; i <= rolloffMax; i++) {
                weights[i - rolloffMin] = 1 / (1 + Math.exp( -k * (i - x0)));
            }
        }

        @Override
        public double getWeight (int x) {
            if (x < rolloffMin)
                return 1;

            if (x > rolloffMax)
                return 0;

            return weights[x - rolloffMin];
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

        double cutoff; // the cutoff point

        public SharpCutoff(int cutoff) {
            this.cutoff = cutoff;
        }

        @Override
        public double getWeight(int seconds) {
            return (seconds < cutoff) ? 1 : 0;
        }

        @Override
        public double applyWeight (double n, int seconds) {
            return (seconds < cutoff) ? n : 0;
        }

    }

}
