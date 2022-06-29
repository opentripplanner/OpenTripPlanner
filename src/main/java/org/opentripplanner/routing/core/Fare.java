package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.model.FareProduct;

/**
 * <p>
 * Fare is a set of fares for different classes of users.
 * </p>
 */
public class Fare {

  /**
   * A mapping from {@link FareType} to {@link Money}.
   */
  public HashMap<FareType, Money> fare;
  /**
   * A mapping from {@link FareType} to a list of {@link FareComponent}. The FareComponents are
   * stored in an array instead of a list because JAXB doesn't know how to deal with interfaces when
   * serializing a trip planning response, and List is an interface. See
   * https://stackoverflow.com/a/1119241/778449
   */
  public HashMap<FareType, FareComponent[]> details;

  private Set<FareProduct> products = new HashSet<>();

  public Fare() {
    fare = new HashMap<>();
    details = new HashMap<>();
  }

  public Fare(Fare aFare) {
    this();
    if (aFare != null) {
      for (Map.Entry<FareType, Money> kv : aFare.fare.entrySet()) {
        fare.put(kv.getKey(), new Money(kv.getValue().getCurrency(), kv.getValue().getCents()));
      }
    }
  }

  public static Fare empty() {
    return new Fare();
  }

  public void addFare(FareType fareType, Currency currency, int cents) {
    fare.put(fareType, new Money(currency, cents));
  }

  public void addFare(FareType fareType, Money money) {
    fare.put(fareType, money);
  }

  public void addProduct(FareProduct fareProduct) {
    products.add(fareProduct);
  }

  public void addFareDetails(FareType fareType, List<FareComponent> newDetails) {
    details.put(fareType, newDetails.toArray(new FareComponent[newDetails.size()]));
  }

  public Money getFare(FareType type) {
    return fare.get(type);
  }

  public List<FareComponent> getDetails(FareType type) {
    return Arrays.asList(details.get(type));
  }

  @Override
  public int hashCode() {
    return Objects.hash(fare, details);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Fare fare1 = (Fare) o;
    return Objects.equals(fare, fare1.fare) && Objects.equals(details, fare1.details);
  }

  public String toString() {
    StringBuilder buffer = new StringBuilder("Fare(");
    for (FareType type : fare.keySet()) {
      Money cost = fare.get(type);
      buffer.append("[");
      buffer.append(type.toString());
      buffer.append(":");
      buffer.append(cost.toString());
      buffer.append("], ");
    }
    buffer.append(")");
    return buffer.toString();
  }

  public Set<FareProduct> getProducts() {
    return Set.copyOf(products);
  }

  public void addProducts(Set<FareProduct> products) {
    this.products.addAll(products);
  }

  public enum FareType implements Serializable {
    regular,
    student,
    senior,
    tram,
    special,
    youth,
  }
}
