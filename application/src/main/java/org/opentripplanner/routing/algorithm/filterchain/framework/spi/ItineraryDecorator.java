package org.opentripplanner.routing.algorithm.filterchain.framework.spi;

import org.opentripplanner.model.plan.Itinerary;

/**
 * Use this interface to decorate itineraries with more information.
 */
public interface ItineraryDecorator {
  /**
   * Implement this to decorate each itinerary in the result. Since the Itinerary class is
   * immutable (for most parts) you need to copy it into a builder and the return the newly build
   * itinerary. The filter-chain framwork will replace the original itinerary object with the newly
   * build one.
   */
  Itinerary decorate(Itinerary itinerary);
}
