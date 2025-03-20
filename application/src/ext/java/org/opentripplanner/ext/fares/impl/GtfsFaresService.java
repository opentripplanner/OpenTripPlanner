package org.opentripplanner.ext.fares.impl;

import java.util.Collection;
import java.util.Objects;
import org.opentripplanner.ext.fares.model.LegProducts;
import org.opentripplanner.model.fare.ItineraryFare;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.fares.FareService;

public record GtfsFaresService(DefaultFareService faresV1, GtfsFaresV2Service faresV2)
  implements FareService {
  @Override
  public ItineraryFare calculateFares(Itinerary itinerary) {
    var fare = Objects.requireNonNullElse(faresV1.calculateFares(itinerary), ItineraryFare.empty());
    var products = faresV2.getProducts(itinerary);
    fare.addItineraryProducts(products.itineraryProducts());
    if (products.itineraryProducts().isEmpty()) {
      addLegProducts(products.legProducts(), fare);
    }
    return fare;
  }
  /**
   * Add a complex set of fare products for a specific leg;
   */
  private static void addLegProducts(Collection<LegProducts> legProducts, ItineraryFare fares) {
    legProducts.forEach(lp -> {
      lp
        .products()
        .stream()
        .map(LegProducts.ProductWithTransfer::products)
        .forEach(fp -> {
          fares.addFareProduct(lp.leg(), fp);
        });
    });
  }
}
