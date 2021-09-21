package org.opentripplanner.transit.raptor.api.transit;


/**
 * The Raptor path result contain an entity with the "special" transfer with
 * constraints. Raptor do not use this at all, but to avoid looking it up in the
 * itinerary mapping Raptor provide the hooks to return it.
 */
public interface RaptorConstrainedTransfer {

    /**
     * The transfer constraint used for thi transfer.
     */
    RaptorTransferConstraint getTransferConstraint();
}
