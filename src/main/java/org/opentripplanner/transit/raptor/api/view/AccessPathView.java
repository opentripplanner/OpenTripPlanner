package org.opentripplanner.transit.raptor.api.view;

import org.opentripplanner.transit.raptor.api.transit.RaptorAccessEgress;

/**
 * Provide access path information to debugger and path mapping.
 */
public interface AccessPathView {
  /**
   * The transit model access connecting the start or end location of the search.
   * <p>
   * This is a reference to a transit-layer object passed into Raptor.
   */
  RaptorAccessEgress access();
}
