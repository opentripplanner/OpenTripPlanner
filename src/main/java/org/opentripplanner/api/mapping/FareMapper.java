package org.opentripplanner.api.mapping;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.api.model.ApiCurrency;
import org.opentripplanner.api.model.ApiFare;
import org.opentripplanner.api.model.ApiFareComponent;
import org.opentripplanner.api.model.ApiFareType;
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
    Map<ApiFareType, ApiMoney> apiFare = fare.fare
      .entrySet()
      .stream()
      .map(e -> {
        var type = toApiFare(e.getKey());
        var money = toApiMoney(e.getValue());
        return new SimpleEntry<>(type, money);
      })
      .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

    Map<ApiFareType, List<ApiFareComponent>> apiComponent = fare.details
      .entrySet()
      .stream()
      .map(e -> {
        var type = toApiFare(e.getKey());
        var money = Arrays.stream(e.getValue()).map(this::toApiFareComponent).toList();
        return new SimpleEntry<>(type, money);
      })
      .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

    return new ApiFare(apiFare, apiComponent);
  }

  private ApiFareType toApiFare(Fare.FareType t) {
    return switch (t) {
      case regular -> ApiFareType.regular;
      case student -> ApiFareType.student;
      case senior -> ApiFareType.senior;
      case tram -> ApiFareType.tram;
      case special -> ApiFareType.special;
      case youth -> ApiFareType.youth;
    };
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
    return new ApiFareComponent(m.fareId, toApiMoney(m.price), m.routes);
  }
}
