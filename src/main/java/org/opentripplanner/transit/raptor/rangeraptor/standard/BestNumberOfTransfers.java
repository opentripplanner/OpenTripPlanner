package org.opentripplanner.transit.raptor.rangeraptor.standard;


/**
 * This interface is part of calculating heuristics for transfers.
 */
public interface BestNumberOfTransfers {

    /**
     * If a stop is not reached the {@link #calculateMinNumberOfTransfers(int)} should
     * return this value. The value is a very high number.
     */
    default int unreachedMinNumberOfTransfers() {
        return 9999;
    }


    /**
     * Return the minimum number for transfers used to reach the given stop.
     * <p/>
     * This method is called after the search is complete, not before.
     * <p/>
     * The rusult is used to calculate heuristics, so the calculated value
     * should be less than or equals to the "real vaule".
     */
    int calculateMinNumberOfTransfers(int stop);
}
