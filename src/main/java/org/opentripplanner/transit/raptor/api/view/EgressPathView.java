package org.opentripplanner.transit.raptor.api.view;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

/**
 * Provide egress path information to debugger and path mapping.
 */
public interface EgressPathView {

  /**
   * The transit model egress connecting the start or end location of the search.
   * <p>
   * This is a reference to a transit-layer object passed into Raptor.
   */
  RaptorTransfer egress();
}