package org.opentripplanner.ext.fares;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.fares.FareService;

/**
 * Computes the fares of an itinerary and adds them.
 */
public final class DecorateWithFare implements ItineraryDecorator {

  private final FareService fareService;

  public DecorateWithFare(FareService fareService) {
    this.fareService = fareService;
  }

  @Override
  public void decorate(Itinerary itinerary) {
    var fare = fareService.calculateFares(itinerary);
    if (fare != null) {
      itinerary.setFare(fare);
      FaresToItineraryMapper.addFaresToLegs(fare, itinerary);
    }
  }
}
