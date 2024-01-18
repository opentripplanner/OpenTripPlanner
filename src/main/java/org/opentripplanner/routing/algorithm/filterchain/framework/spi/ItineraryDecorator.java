package org.opentripplanner.routing.algorithm.filterchain.framework.spi;

import org.opentripplanner.model.plan.Itinerary;

/**
 * Use this interface to decorate itineraries with more information.
 */
public interface ItineraryDecorator {
  /**
   * Implement this to decorate each itinerary in the result.
   */
  void decorate(Itinerary itinerary);
}
