package org.opentripplanner.profile;

/**
 * A weighting function, expressing how influential something is according to its distance from you.
 * A functional interface (with a single function, so it can be defined using Java 8 shorthand).
 */
public abstract class WeightingFunction {

    public abstract double getWeight (double x);

    public double applyWeight (double n, double x) {
        return n * getWeight(x);
    }

    /**
     * The logistic function, a commonly used sigmoid (s-shaped function).
     * Basically a step function with a smooth rolloff.
     * Positive values of k cause the function to increase from zero to one, negative values will be inverted.
     * <br>
     * k=-1 gives a rolloff over an interval of ~10 minutes<br>
     * k=-0.5 gives a rolloff over an interval of ~20 minutes<br>
     * k=-2 contracts the rolloff interval to ~5 minutes<br>
     */
    public static final class Logistic extends WeightingFunction {

        double x0;  // cutoff point
        double k;   // steepness of the curve. negative

        public Logistic (double cutoff, double steepness) {
            this.x0 = cutoff;
            this.k = steepness;
        }

        @Override
        public double getWeight (double x) {
            return 1 / (1 + Math.exp( -k * (x - x0)));
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

        double x0; // the cutoff point

        public SharpCutoff(double x0) {
            this.x0 = x0;
        }

        @Override
        public double getWeight(double x) {
            return (x < x0) ? 1 : 0;
        }

        @Override
        public double applyWeight (double n, double x) {
            return (n < x0) ? n : 0;
        }

    }

}
