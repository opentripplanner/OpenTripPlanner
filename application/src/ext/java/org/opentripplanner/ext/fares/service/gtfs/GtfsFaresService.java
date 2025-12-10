package org.opentripplanner.ext.fares.service.gtfs;

import com.google.common.collect.Multimap;
import java.util.Objects;
import org.opentripplanner.ext.fares.service.gtfs.v1.DefaultFareService;
import org.opentripplanner.ext.fares.service.gtfs.v2.GtfsFaresV2Service;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.ItineraryFare;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.fares.FareService;

public final class GtfsFaresService implements FareService {

  private final DefaultFareService faresV1;
  private final GtfsFaresV2Service faresV2;

  public GtfsFaresService(DefaultFareService faresV1, GtfsFaresV2Service faresV2) {
    this.faresV1 = faresV1;
    this.faresV2 = faresV2;
  }

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
  private static void addLegProducts(Multimap<Leg, FareOffer> legProducts, ItineraryFare fares) {
    legProducts
      .entries()
      .forEach(e -> {
        fares.addFareProduct(e.getKey(), e.getValue());
      });
  }

  public DefaultFareService faresV1() {
    return faresV1;
  }
}
