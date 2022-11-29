package org.opentripplanner.routing.algorithm.raptoradapter.api;

import org.opentripplanner.raptor.spi.RaptorTripPattern;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.PatternCostCalculator;
import org.opentripplanner.transit.model.network.Route;

public interface DefaultTripPattern extends RaptorTripPattern {
  /**
   * This is not used by the default calculator, but by the {@link PatternCostCalculator} to
   * give unpreferred routes or agencies a generalized-cost penalty.
   */
  Route route();
}
