package org.opentripplanner.ext.fares.impl;

import java.util.Objects;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.ItineraryFares;
import org.opentripplanner.routing.fares.FareService;

public record GtfsFaresService(DefaultFareService faresV1, GtfsFaresV2Service faresV2)
  implements FareService {
  @Override
  public ItineraryFares getCost(Itinerary itinerary) {
    var fare = Objects.requireNonNullElse(faresV1.getCost(itinerary), ItineraryFares.empty());
    var products = faresV2.getProducts(itinerary);
    fare.addItineraryProducts(products.itineraryProducts());
    if (products.itineraryProducts().isEmpty()) {
      fare.addLegProducts(products.legProducts());
    }
    return fare;
  }
}
