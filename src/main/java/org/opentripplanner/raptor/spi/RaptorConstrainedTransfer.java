package org.opentripplanner.raptor.spi;

/**
 * The Raptor path result contains an entity with the "special" transfer with constraints. Raptor
 * does not use this interface, but in order to avoid looking it up in the itinerary mapping, Raptor
 * provides the hooks to return it.
 */
public interface RaptorConstrainedTransfer {
  /**
   * The transfer constraint used for the transfer.
   */
  RaptorTransferConstraint getTransferConstraint();
}
