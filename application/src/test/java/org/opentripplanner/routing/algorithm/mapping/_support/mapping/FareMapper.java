package org.opentripplanner.routing.algorithm.mapping._support.mapping;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.model.fare.FareMedium;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.RiderCategory;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.mapping._support.model.ApiCurrency;
import org.opentripplanner.routing.algorithm.mapping._support.model.ApiFareProduct;
import org.opentripplanner.routing.algorithm.mapping._support.model.ApiFareQualifier;
import org.opentripplanner.routing.algorithm.mapping._support.model.ApiItineraryFares;
import org.opentripplanner.routing.algorithm.mapping._support.model.ApiMoney;
import org.opentripplanner.transit.model.basic.Money;

@Deprecated
class FareMapper {

  private final Locale locale;

  public FareMapper(Locale locale) {
    this.locale = locale;
  }

  public ApiItineraryFares mapFare(Itinerary itinerary) {
    var fares = itinerary.fare();

    return new ApiItineraryFares(
      Map.of(),
      Map.of(),
      toApiFareProducts(fares.getItineraryProducts()),
      toApiLegProducts(fares.getLegProducts())
    );
  }

  private List<ApiFareProduct> toApiLegProducts(Multimap<Leg, FareOffer> legProducts) {
    if (legProducts.isEmpty()) {
      return null;
    } else {
      return legProducts
        .keySet()
        .stream()
        .flatMap(leg -> {
          // eventually we want to implement products that span multiple legs (but not the entire itinerary)
          return instancesToApiFareProducts(legProducts.get(leg)).stream();
        })
        .toList();
    }
  }

  private static ApiFareQualifier toApiFareQualifier(@Nullable FareMedium nullable) {
    return Optional.ofNullable(nullable)
      .map(c -> new ApiFareQualifier(c.id().getId(), c.name()))
      .orElse(null);
  }

  private static ApiFareQualifier toApiFareQualifier(@Nullable RiderCategory nullable) {
    return Optional.ofNullable(nullable)
      .map(c -> new ApiFareQualifier(c.id().getId(), c.name()))
      .orElse(null);
  }

  private List<ApiFareProduct> instancesToApiFareProducts(Collection<FareOffer> product) {
    return toApiFareProducts(product.stream().map(FareOffer::fareProduct).toList());
  }

  private List<ApiFareProduct> toApiFareProducts(Collection<FareProduct> product) {
    if (product.isEmpty()) return null;
    else {
      return product
        .stream()
        .map(p ->
          new ApiFareProduct(
            p.id().toString(),
            p.name(),
            toApiMoney(p.price()),
            toApiFareQualifier(p.medium()),
            toApiFareQualifier(p.category())
          )
        )
        .toList();
    }
  }

  private ApiMoney toApiMoney(Money m) {
    var c = m.currency();
    return new ApiMoney(
      m.minorUnitAmount(),
      new ApiCurrency(
        c.getCurrencyCode(),
        c.getDefaultFractionDigits(),
        c.getCurrencyCode(),
        c.getSymbol(locale)
      )
    );
  }
}
