package org.opentripplanner.routing.algorithm.raptoradapter.api;

import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.transit.model.network.Route;

public interface DefaultTripPattern extends RaptorTripPattern {
  /**
   * This is used to calculate the unpreferred cost for routes or agencies.
   */
  Route route();
}
