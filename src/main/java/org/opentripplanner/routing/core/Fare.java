package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.model.FareProduct;

/**
 * <p>
 * Fare is a set of fares for different classes of users.
 * </p>
 */
public class Fare {

  protected final Map<String, Container> details;

  public Fare(Fare aFare) {
    if (aFare != null) {
      details = new HashMap<>(aFare.details);
    } else {
      details = new HashMap<>();
    }
  }

  public Fare() {
    details = new HashMap<>();
  }

  public static Fare empty() {
    return new Fare();
  }

  public void addFare(FareType fareType, Money money) {
    addFare(fareType, money, List.of());
  }

  public void addFare(FareType fareType, Money money, List<FareComponent> components) {
    details.put(fareType.name(), new Container(money, components));
  }

  public void addProduct(FareProduct fareProduct) {
    details.put(fareProduct.id().toString(), new Container(fareProduct.amount(), List.of()));
  }

  public Money getFare(FareType type) {
    return getFare(type.name());
  }

  public Money getFare(String type) {
    return Optional.ofNullable(details.get(type)).map(Container::amount).orElse(null);
  }

  public List<FareComponent> getDetails(FareType type) {
    return getDetails(type.name());
  }

  public List<FareComponent> getDetails(String type) {
    return Optional.ofNullable(details.get(type)).map(Container::components).orElse(null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(details);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Fare fare1 = (Fare) o;
    return Objects.equals(details, fare1.details);
  }

  public void addProducts(Collection<FareProduct> products) {
    products.forEach(this::addProduct);
  }

  public Set<String> getTypes() {
    return details.keySet();
  }

  public Set<Money> getMoneys() {
    return details.values().stream().map(Container::amount).collect(Collectors.toSet());
  }

  public enum FareType implements Serializable {
    regular,
    student,
    senior,
    tram,
    special,
    youth,
  }

  private record Container(Money amount, List<FareComponent> components) {}
}
