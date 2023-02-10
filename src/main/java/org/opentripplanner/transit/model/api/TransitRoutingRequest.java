package org.opentripplanner.transit.model.api;

import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.GeneralizedCostParameters;
import org.opentripplanner.transit.model.trip.TripOnDay;

/**
 * TODO RTM - This has nothing to do with the transit model, move somewhere else
 */
public record TransitRoutingRequest(
  RaptorRequest<TripOnDay> raptorRequest,
  GeneralizedCostParameters generalizedCostParams,

  /**
   * Returns the beginning of valid transit data. All trips running even partially after this time
   * are included.
   * <p>
   * Unit: seconds since midnight of the day of the search.
   */
  int validTransitDataStartTime,

  /**
   * Returns the end time of valid transit data. All trips running even partially before this time
   * are included.
   * <p>
   * Unit: seconds since midnight of the day of the search
   */
  int validTransitDataEndTime
) {}
