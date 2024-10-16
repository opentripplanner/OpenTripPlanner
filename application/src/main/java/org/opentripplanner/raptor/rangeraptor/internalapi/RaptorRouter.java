package org.opentripplanner.raptor.rangeraptor.internalapi;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * Interface for Raptor Router. Allow instrumentation/wrapping the router. This is not
 * currently used in the main branch of OTP, but it is used in Entur fork to extend the
 * router functionality.
 */
public interface RaptorRouter<T extends RaptorTripSchedule> {
  /**
   * Perform the routing request and return the result. A range-raptor request will
   * iterate over the minutes in the search-window, while a plain raptor search will
   * just do one iteration.
   */
  RaptorRouterResult<T> route();
}
