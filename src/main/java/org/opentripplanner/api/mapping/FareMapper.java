package org.opentripplanner.api.mapping;

import com.google.common.collect.Multimap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.api.model.ApiCurrency;
import org.opentripplanner.api.model.ApiFareComponent;
import org.opentripplanner.api.model.ApiFareProduct;
import org.opentripplanner.api.model.ApiFareQualifier;
import org.opentripplanner.api.model.ApiItineraryFares;
import org.opentripplanner.api.model.ApiLegProducts;
import org.opentripplanner.api.model.ApiMoney;
import org.opentripplanner.ext.fares.model.FareContainer;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.ext.fares.model.RiderCategory;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.core.FareComponent;
import org.opentripplanner.routing.core.ItineraryFares;
import org.opentripplanner.transit.model.basic.Money;

public class FareMapper {

  private final Locale locale;

  public FareMapper(Locale locale) {
    this.locale = locale;
  }

  public ApiItineraryFares mapFare(Itinerary itinerary) {
    var fares = itinerary.getFares();
    Map<String, ApiMoney> apiFare = toApiMoneys(fares);
    Map<String, List<ApiFareComponent>> apiComponent = toApiFareComponents(fares);

    return new ApiItineraryFares(
      apiFare,
      apiComponent,
      toApiFareProducts(fares.getItineraryProducts()),
      toApiLegProducts(itinerary, fares.getLegProducts())
    );
  }

  private List<ApiLegProducts> toApiLegProducts(
    Itinerary itinerary,
    Multimap<Leg, FareProduct> legProducts
  ) {
    if (legProducts.isEmpty()) {
      return null;
    } else {
      return legProducts
        .keySet()
        .stream()
        .map(leg -> {
          var index = itinerary.getLegIndex(leg);
          // eventually we want to implement products that span multiple legs (but not the entire itinerary)
          return new ApiLegProducts(List.of(index), toApiFareProducts(legProducts.get(leg)));
        })
        .toList();
    }
  }

  private static ApiFareQualifier toApiFareQualifier(@Nullable FareContainer nullable) {
    return Optional
      .ofNullable(nullable)
      .map(c -> new ApiFareQualifier(c.id(), c.name()))
      .orElse(null);
  }

  private static ApiFareQualifier toApiFareQualifier(@Nullable RiderCategory nullable) {
    return Optional
      .ofNullable(nullable)
      .map(c -> new ApiFareQualifier(c.id(), c.name()))
      .orElse(null);
  }

  private List<ApiFareProduct> toApiFareProducts(Collection<FareProduct> product) {
    if (product.isEmpty()) return null; else {
      return product
        .stream()
        .map(p ->
          new ApiFareProduct(
            p.id().toString(),
            p.name(),
            toApiMoney(p.amount()),
            toApiFareQualifier(p.category()),
            toApiFareQualifier(p.container())
          )
        )
        .toList();
    }
  }

  private Map<String, List<ApiFareComponent>> toApiFareComponents(ItineraryFares fare) {
    return fare
      .getTypes()
      .stream()
      .map(key -> {
        var money = fare.getDetails(key).stream().map(this::toApiFareComponent).toList();
        return new SimpleEntry<>(key, money);
      })
      .collect(Collectors.toMap(e -> e.getKey().name(), Entry::getValue));
  }

  private Map<String, ApiMoney> toApiMoneys(ItineraryFares fare) {
    return fare
      .getTypes()
      .stream()
      .map(key -> {
        var money = toApiMoney(fare.getFare(key));
        return new SimpleEntry<>(key, money);
      })
      .collect(Collectors.toMap(e -> e.getKey().name(), Entry::getValue));
  }

  private ApiMoney toApiMoney(Money m) {
    var c = m.currency();
    return new ApiMoney(
      m.cents(),
      new ApiCurrency(
        c.getCurrencyCode(),
        c.getDefaultFractionDigits(),
        c.getCurrencyCode(),
        c.getSymbol(locale)
      )
    );
  }

  private ApiFareComponent toApiFareComponent(FareComponent m) {
    return new ApiFareComponent(m.fareId(), m.name(), toApiMoney(m.price()), m.routes());
  }
}
