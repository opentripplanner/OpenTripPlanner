package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import org.opentripplanner.model.Trip;

/**
 * This class is used to match a given trip and stop index.
 */
interface TransferPointMatcher {
    boolean match(int stopIndex, Trip trip);
}
