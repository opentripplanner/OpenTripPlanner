package org.opentripplanner.api.mapping;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opentripplanner.api.model.ApiCurrency;
import org.opentripplanner.api.model.ApiFare;
import org.opentripplanner.api.model.ApiFareComponent;
import org.opentripplanner.api.model.ApiMoney;
import org.opentripplanner.model.FareContainer;
import org.opentripplanner.model.RiderCategory;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareComponent;
import org.opentripplanner.routing.core.Money;

public class FareMapper {

  public static ApiFare mapFare(Fare fare) {
    Map<String, ApiMoney> apiFare = combineFaresAndProducts(fare);
    Map<String, List<ApiFareComponent>> apiComponent = combineComponentsAndProducts(fare);

    return new ApiFare(apiFare, apiComponent);
  }

  private static Map<String, List<ApiFareComponent>> combineComponentsAndProducts(Fare fare) {
    return fare
      .getTypes()
      .stream()
      .map(key -> {
        var money = fare.getDetails(key).stream().map(FareMapper::toApiFareComponent).toList();
        return new SimpleEntry<>(key, money);
      })
      .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private static Map<String, ApiMoney> combineFaresAndProducts(Fare fare) {
    return fare
      .getTypes()
      .stream()
      .map(key -> {
        var money = toApiMoney(fare.getFare(key));
        return new SimpleEntry<>(key, money);
      })
      .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private static ApiMoney toApiMoney(Money m) {
    return new ApiMoney(m.getCents(), new ApiCurrency(m.getCurrency()));
  }

  private static ApiFareComponent toApiFareComponent(FareComponent m) {
    return new ApiFareComponent(
      m.fareId,
      toApiMoney(m.price),
      m.routes,
      Optional.ofNullable(m.container).map(FareContainer::name).orElse(null),
      Optional.ofNullable(m.category).map(RiderCategory::name).orElse(null)
    );
  }
}
