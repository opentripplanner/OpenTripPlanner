package org.opentripplanner.api.mapping;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opentripplanner.api.model.ApiCurrency;
import org.opentripplanner.api.model.ApiFare;
import org.opentripplanner.api.model.ApiFareComponent;
import org.opentripplanner.api.model.ApiFareQualifier;
import org.opentripplanner.api.model.ApiMoney;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareComponent;
import org.opentripplanner.routing.core.Money;

public class FareMapper {

  private final Locale locale;

  public FareMapper(Locale locale) {
    this.locale = locale;
  }

  public ApiFare mapFare(Fare fare) {
    Map<String, ApiMoney> apiFare = combineFaresAndProducts(fare);
    Map<String, List<ApiFareComponent>> apiComponent = combineComponentsAndProducts(fare);

    return new ApiFare(apiFare, apiComponent);
  }

  private Map<String, List<ApiFareComponent>> combineComponentsAndProducts(Fare fare) {
    return fare
      .getTypes()
      .stream()
      .map(key -> {
        var money = fare.getDetails(key).stream().map(this::toApiFareComponent).toList();
        return new SimpleEntry<>(key, money);
      })
      .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private Map<String, ApiMoney> combineFaresAndProducts(Fare fare) {
    return fare
      .getTypes()
      .stream()
      .map(key -> {
        var money = toApiMoney(fare.getFare(key));
        return new SimpleEntry<>(key, money);
      })
      .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
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
    return new ApiFareComponent(
      m.fareId(),
      m.name(),
      toApiMoney(m.price()),
      m.routes(),
      Optional
        .ofNullable(m.container())
        .map(c -> new ApiFareQualifier(c.id(), c.name()))
        .orElse(null),
      Optional
        .ofNullable(m.category())
        .map(c -> new ApiFareQualifier(c.id(), c.name()))
        .orElse(null)
    );
  }
}
