package org.opentripplanner.transit.raptor.api.view;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;


/**
 * Provide access path information to debugger and path mapping.
 */
public interface AccessPathView {

  /**
   * The transit model access connecting the start or end location of the search.
   * <p>
   * This is a reference to a transit-layer object passed into Raptor.
   */
  RaptorTransfer access();
}
