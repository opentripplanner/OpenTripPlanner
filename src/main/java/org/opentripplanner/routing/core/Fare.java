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
import org.opentripplanner.ext.fares.model.LegProducts;
import org.opentripplanner.model.FareProduct;
import org.opentripplanner.util.lang.ToStringBuilder;

/**
 * <p>
 * Fare is a set of fares for different classes of users.
 * </p>
 */
public class Fare {

  protected final Map<String, FareEntry> details;

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
    details.put(fareType.name(), new FareEntry(money, components));
  }

  public void addProduct(FareProduct product) {
    details.put(
      product.id().toString(),
      new FareEntry(
        product.amount(),
        List.of(
          new FareComponent(
            product.id(),
            product.amount(),
            List.of(),
            product.container(),
            product.category()
          )
        )
      )
    );
  }

  public Money getFare(FareType type) {
    return getFare(type.name());
  }

  public Money getFare(String type) {
    return Optional.ofNullable(details.get(type)).map(FareEntry::amount).orElse(null);
  }

  public List<FareComponent> getDetails(FareType type) {
    return getDetails(type.name());
  }

  public List<FareComponent> getDetails(String type) {
    return Optional.ofNullable(details.get(type)).map(FareEntry::components).orElse(null);
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

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("details", details).toString();
  }

  public void addProducts(Collection<FareProduct> products) {
    products.forEach(this::addProduct);
  }

  public Set<String> getTypes() {
    return details.keySet();
  }

  public Set<Money> getMoneys() {
    return details.values().stream().map(FareEntry::amount).collect(Collectors.toSet());
  }

  public void addLegProducts(List<LegProducts> legProducts) {
    legProducts.forEach(lp -> {
      // we put -1 one here to tell clients that it's not the real price. the details
      // about each leg are in the fare component.
      var sum = Money.usDollars(-1);
      lp
        .products()
        .forEach(p -> {
          var component = new FareComponent(
            p.id(),
            p.amount(),
            List.of(lp.leg().getRoute().getId()),
            p.container(),
            p.category()
          );

          details.put(p.id().toString(), new FareEntry(sum, List.of(component)));
        });
    });
  }

  public enum FareType implements Serializable {
    regular,
    student,
    senior,
    tram,
    special,
    youth,
  }

  private record FareEntry(Money amount, List<FareComponent> components) {}
}
