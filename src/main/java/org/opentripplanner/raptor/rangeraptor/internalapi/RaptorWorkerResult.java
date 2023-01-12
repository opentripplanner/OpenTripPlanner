package org.opentripplanner.raptor.rangeraptor.internalapi;

import java.util.Collection;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.response.StopArrivals;

/**
 * This is the result of the {@link RaptorWorker#route()} call.
 */
public interface RaptorWorkerResult<T extends RaptorTripSchedule> {
  /**
   * Return all paths found.
   */
  Collection<RaptorPath<T>> extractPaths();

  /**
   * Return best over-all-arrival-times, best transit-arrival-times, and lowest number of
   * transfers for all stops.
   */
  StopArrivals stopArrivals();
}
