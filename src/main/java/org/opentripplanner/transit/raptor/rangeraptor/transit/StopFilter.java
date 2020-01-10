package org.opentripplanner.transit.raptor.rangeraptor.transit;


/**
 * The stop filter have one method used to tell the client is allowed to visit a stop or not.
 */
@FunctionalInterface
public interface StopFilter {

    /**
     * Is is allowed to do a transfer at this stop? If FALSE alighting at and departure from this
     * stop is not allowed.
     */
    boolean allowStopVisit(int stop);
}
