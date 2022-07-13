package org.opentripplanner.ext.fares.impl;

import java.util.Objects;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.fares.FareService;

public record GtfsFaresService(DefaultFareServiceImpl faresV1, GtfsFaresV2Service faresV2)
  implements FareService {
  @Override
  public Fare getCost(Itinerary itinerary) {
    var fare = Objects.requireNonNullElse(faresV1.getCost(itinerary), Fare.empty());
    var products = faresV2.getProducts(itinerary);
    fare.addProducts(products.productsCoveringItinerary());
    if (products.productsCoveringItinerary().isEmpty()) {
      fare.addLegProducts(products.legProducts());
    }
    return fare;
  }
}
