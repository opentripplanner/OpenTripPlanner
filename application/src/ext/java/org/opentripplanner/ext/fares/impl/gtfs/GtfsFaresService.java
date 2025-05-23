package org.opentripplanner.ext.fares.impl.gtfs;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.FareProductLike;
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
    Multimap<Leg, TransferFareProduct> legProducts,
    ItineraryFare fares
  ) {
    legProducts
      .entries()
      .forEach(e -> {
        final TransferFareProduct value = e.getValue();
        final Collection<FareProductLike> dependencies = value
          .dependencies()
          .stream()
          .map(fp -> new FareProductLike(fp, List.of()))
          .toList();
        var productLike = new FareProductLike(value.transferProduct(), dependencies);
        fares.addFareProduct(e.getKey(), productLike);
      });
  }
}
