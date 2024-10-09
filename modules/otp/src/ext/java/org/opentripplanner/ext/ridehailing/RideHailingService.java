package org.opentripplanner.ext.ridehailing;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.ext.ridehailing.model.ArrivalTime;
import org.opentripplanner.ext.ridehailing.model.RideEstimate;
import org.opentripplanner.ext.ridehailing.model.RideHailingProvider;
import org.opentripplanner.framework.geometry.WgsCoordinate;

/**
 * A service for querying ride hailing information to be used during routing.
 */
public interface RideHailingService {
  /**
   * The provider of the service.
   */
  RideHailingProvider provider();

  /**
   * Get the next arrivals for a specific location.
   */
  List<ArrivalTime> arrivalTimes(WgsCoordinate coordinate, boolean wheelchairAccessible)
    throws ExecutionException;

  List<RideEstimate> rideEstimates(
    WgsCoordinate start,
    WgsCoordinate end,
    boolean wheelchairAccessible
  ) throws ExecutionException;
}
