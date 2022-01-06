package org.opentripplanner.transit.raptor.api.transit;


/**
 * Raptor does not need any information from the constrained transfer, but it passes the
 * instance in a callback to the cost calculator.
 */
public interface RaptorTransferConstraint {
    /* This is intentionally empty */
}