package org.opentripplanner.transit.raptor.transitadapter;

import org.opentripplanner.transit.raptor.api.transit.TransferLeg;

import java.util.Iterator;


/**
 * Create a lightweight TransferLeg iterator, using a single object to represent
 * the iterator and all instances of the TransferLeg. The TransferLeg is
 * only valid for the duration of one step.
 * <p/>
 * Used {@link #clone()} to get a new iterator to iterate over the same Transfers.
 * This enables the iterator to be reused, and is THREAD SAFE.
 */
class LightweightTransferIterator implements Iterator<TransferLeg>, TransferLeg {
    private final int[] durationToStops;
    private int index;

    LightweightTransferIterator(int[] durationToStops) {
        this.durationToStops = durationToStops;
        this.index = this.durationToStops.length == 0 ? 0 : -2;
    }


    /* Iterator<TransferLeg> methods */

    @Override
    public boolean hasNext() {
        index += 2;
        return index < durationToStops.length;
    }

    @Override
    public TransferLeg next() {
        return this;
    }

    /* TransferLeg, lightweight implementation */

    @Override
    public int stop() {
        return durationToStops[index];
    }

    @Override
    public int durationInSeconds() {
        return durationToStops[index + 1];
    }

    /**
     * Used to reset the iterator, to start at the beginning again. This
     * enables the iterator to be reused, but be careful to not use it in a multi
     * threaded environment.
     * <p/>
     * NOT THREAD SAFE!
     */
    void reset() {
        this.index = this.durationToStops.length == 0 ? 0 : -2;
    }

    /**
     * Used to make a copy of the iterator.
     */
    @Override
    public org.opentripplanner.transit.raptor.transitadapter.LightweightTransferIterator clone() {
        return new org.opentripplanner.transit.raptor.transitadapter.LightweightTransferIterator(durationToStops);
    }
}
