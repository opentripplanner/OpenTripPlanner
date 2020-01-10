package org.opentripplanner.transit.raptor.rangeraptor.transit;

import java.util.BitSet;


/**
 * A implementation of the StopFilter interface that wraps a BitSet.
 */
public class StopFilterBitSet implements StopFilter {
    private final BitSet allowVisit;

    StopFilterBitSet (BitSet allowVisit) {
        this.allowVisit = allowVisit;
    }

    @Override
    public boolean allowStopVisit(int stop) {
        return allowVisit.get(stop);
    }
}
