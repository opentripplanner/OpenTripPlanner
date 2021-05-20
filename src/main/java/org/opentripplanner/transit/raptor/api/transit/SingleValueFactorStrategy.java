package org.opentripplanner.transit.raptor.api.transit;

/**
 * This {@link FactorStrategy} keep a single value and use it every time
 * the factor is needed. The {@link #minFactor()} return the same value.
 * <p>
 * The class and methods are {@code final} to help the JIT compiler optimize the use of this class.
 */
final class SingleValueFactorStrategy implements FactorStrategy {

    private final int factor;

    SingleValueFactorStrategy(int factor) {
        this.factor = factor;
    }

    SingleValueFactorStrategy(double reluctance) {
        this(RaptorCostConverter.toRaptorCost(reluctance));
    }

    @Override
    public final int factor(int index) {
        return factor;
    }

    @Override
    public final int minFactor() {
        return factor;
    }
}
