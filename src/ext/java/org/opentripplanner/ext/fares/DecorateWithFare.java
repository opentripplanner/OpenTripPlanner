package org.opentripplanner.ext.fares;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.fares.FareService;

/**
 * Computes the fares of an itinerary and adds them.
 * <p>
 * TODO: Convert to a class - exposing a service in a DTO is a risk.
 */
public record DecorateWithFare(FareService fareService) implements ItineraryDecorator {
  @Override
  public void decorate(Itinerary itinerary) {
    var fare = fareService.calculateFares(itinerary);
    if (fare != null) {
      itinerary.setFare(fare);
      FaresToItineraryMapper.addFaresToLegs(fare, itinerary);
    }
  }
}
