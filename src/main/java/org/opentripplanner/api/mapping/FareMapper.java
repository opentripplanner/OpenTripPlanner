package org.opentripplanner.api.mapping;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.api.model.ApiCurrency;
import org.opentripplanner.api.model.ApiFare;
import org.opentripplanner.api.model.ApiFareComponent;
import org.opentripplanner.api.model.ApiMoney;
import org.opentripplanner.model.FareContainer;
import org.opentripplanner.model.FareProduct;
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
    var fromFares = fare.details
      .entrySet()
      .stream()
      .map(e -> {
        var type = e.getKey().name();
        var money = Arrays.stream(e.getValue()).map(FareMapper::toApiFareComponent).toList();
        return new SimpleEntry<>(type, money);
      });

    var fromProducts = fare
      .getProducts()
      .stream()
      .map(FareMapper::toApiFareComponent)
      .map(c -> {
        var type = c.fareId().toString();
        return new SimpleEntry<>(type, List.of(c));
      });

    return Stream
      .concat(fromFares, fromProducts)
      .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private static Map<String, ApiMoney> combineFaresAndProducts(Fare fare) {
    // fares v1
    var fares = fare.fare
      .entrySet()
      .stream()
      .map(e -> {
        var type = e.getKey().name();
        var money = toApiMoney(e.getValue());
        return new SimpleEntry<>(type, money);
      });

    // fares v2
    var products = fare
      .getProducts()
      .stream()
      .map(p -> new SimpleEntry<>(p.id().toString(), toApiMoney(p.amount())));

    return Stream.concat(fares, products).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private static ApiMoney toApiMoney(Money m) {
    return new ApiMoney(m.getCents(), new ApiCurrency(m.getCurrency()));
  }

  private static ApiFareComponent toApiFareComponent(FareComponent m) {
    return new ApiFareComponent(m.fareId, toApiMoney(m.price), m.routes, null, null);
  }

  private static ApiFareComponent toApiFareComponent(FareProduct p) {
    return new ApiFareComponent(
      p.id(),
      toApiMoney(p.amount()),
      List.of(),
      Optional.ofNullable(p.container()).map(FareContainer::name).orElse(null),
      Optional.ofNullable(p.category()).map(RiderCategory::name).orElse(null)
    );
  }
}
