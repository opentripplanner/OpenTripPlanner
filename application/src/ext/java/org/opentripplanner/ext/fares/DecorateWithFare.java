package org.opentripplanner.ext.fares;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.fares.FareService;

/**
 * Computes the fares of an itinerary and adds them.
 */
public class DecorateWithFare implements ItineraryDecorator {

  private final FareService fareService;

  public DecorateWithFare(FareService fareService) {
    this.fareService = fareService;
  }

  @Override
  public Itinerary decorate(Itinerary itinerary) {
    var fare = fareService.calculateFares(itinerary);
    return (fare != null)
      ? ItineraryFaresDecorator.decorateItineraryWithFare(itinerary, fare)
      : itinerary;
  }
}
