package org.opentripplanner.raptor.rangeraptor.internalapi;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * The worker performs the travel search. There are multiple implementations, even some that do not
 * return paths.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorWorker<T extends RaptorTripSchedule> {
  /**
   * Perform the routing request.
   */
  RaptorWorkerResult<T> route();
}
