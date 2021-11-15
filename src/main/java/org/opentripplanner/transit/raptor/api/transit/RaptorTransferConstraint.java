package org.opentripplanner.transit.raptor.api.transit;


/**
 * Raptor does not need any information from the constrained transfer, but it passes the
 * instance in a callback to the cost calculator.
 */
public interface RaptorTransferConstraint {

    /**
     * Return {@code true} if the constrained transfer is not allowed between the two routes.
     * Note! If a constraint only apply to specific trips, then the
     * {@link RaptorConstrainedTripScheduleBoardingSearch} is reponsible for NOT returning the
     * NOT-ALLOWED transfer, and finding the next ALLOWED trip.
     */
    boolean isNotAllowed();
}