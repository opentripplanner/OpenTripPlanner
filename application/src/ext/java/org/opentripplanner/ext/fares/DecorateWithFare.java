package org.opentripplanner.ext.fares;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.fares.FareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Computes the fares of an itinerary and adds them.
 */
public class DecorateWithFare implements ItineraryDecorator {

  private static final Logger LOG = LoggerFactory.getLogger(DecorateWithFare.class);
  private final FareService fareService;

  public DecorateWithFare(FareService fareService) {
    this.fareService = fareService;
  }

  @Override
  public Itinerary decorate(Itinerary itinerary) {
    LOG.error("Decorating with {}.", fareService);
    var fare = fareService.calculateFares(itinerary);
    return (fare != null)
      ? ItineraryFaresDecorator.decorateItineraryWithFare(itinerary, fare)
      : itinerary;
  }
}
