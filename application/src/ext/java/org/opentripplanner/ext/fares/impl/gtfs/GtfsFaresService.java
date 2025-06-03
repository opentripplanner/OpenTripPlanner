package org.opentripplanner.ext.fares.impl.gtfs;

import com.google.common.collect.Multimap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareOffer.DefaultFareProduct;
import org.opentripplanner.model.fare.FareOffer.DependentFareProduct;
import org.opentripplanner.model.fare.ItineraryFare;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.fares.FareService;

public record GtfsFaresService(DefaultFareService faresV1, GtfsFaresV2Service faresV2)
  implements FareService {
  @Override
  public ItineraryFare calculateFares(Itinerary itinerary) {
    var fare = ItineraryFare.empty();
    if (faresV2.isEmpty()) {
      fare = Objects.requireNonNullElse(faresV1.calculateFares(itinerary), ItineraryFare.empty());
    } else {
      var products = faresV2.calculateFares(itinerary);
      fare.addItineraryProducts(products.itineraryProducts());
      if (products.itineraryProducts().isEmpty()) {
        addLegProducts(products.legProducts(), fare);
      }
    }
    return fare;
  }
  /**
   * Add a complex set of fare products for a specific leg;
   */
  private static void addLegProducts(
    Multimap<Leg, LegFareProductResult> legProducts,
    ItineraryFare fares
  ) {
    legProducts
      .entries()
      .forEach(e -> {
        LegFareProductResult value = e.getValue();
        Set<FareOffer> dependencies = value
          .dependencies()
          .stream()
          .map(DefaultFareProduct::new)
          .map(FareOffer.class::cast)
          .collect(Collectors.toSet());
        if (dependencies.isEmpty()) {
          fares.addFareProduct(e.getKey(), new DefaultFareProduct(value.transferProduct()));
        } else {
          fares.addFareProduct(
            e.getKey(),
            new DependentFareProduct(value.transferProduct(), dependencies)
          );
        }
      });
  }
}
