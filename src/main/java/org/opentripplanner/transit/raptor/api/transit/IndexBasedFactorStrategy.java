package org.opentripplanner.transit.raptor.api.transit;

import java.util.Arrays;

/**
 * This class keep a facto for each index and the minimum factor for fast retrieval
 * during Raptor search.
 */
final class IndexBasedFactorStrategy implements FactorStrategy {

    private final int[] factors;
    private final int minFactor;

    private IndexBasedFactorStrategy(int[] factors) {
        this.factors = factors;
        this.minFactor = findMinimumFactor(factors);
    }

    /** Convert OTP domain reluctance array to Raptor factors. */
    IndexBasedFactorStrategy(double[] reluctanceByIndex) {
        this(RaptorCostConverter.toRaptorCosts(reluctanceByIndex));
    }

    @Override
    public final int factor(int index) {
        return factors[index];
    }

    @Override
    public final int minFactor() {
        return minFactor;
    }

    static private int findMinimumFactor(int[] factors) {
        return Arrays.stream(factors).min().orElseThrow();
    }
}
