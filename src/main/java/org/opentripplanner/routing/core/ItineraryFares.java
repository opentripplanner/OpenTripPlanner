package org.opentripplanner.routing.core;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.ext.fares.model.LegProducts;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.transit.model.basic.Money;

/**
 * <p>
 * ItineraryFares is a set of fares for different classes of users.
 * </p>
 */
public class ItineraryFares {

  private final Set<FareProduct> itineraryProducts = new HashSet<>();
  private final Multimap<Leg, FareProduct> legProducts = ArrayListMultimap.create();

  /**
   * A mapping from {@link FareType} to {@link Money}.
   */
  private HashMap<FareType, Money> fare = new HashMap<>();

  public ItineraryFares(ItineraryFares aFare) {
    if (aFare != null) {
      fare.putAll(aFare.fare);
    }
  }

  public ItineraryFares() {}

  public static ItineraryFares empty() {
    return new ItineraryFares();
  }

  public Set<FareProduct> getItineraryProducts() {
    return Set.copyOf(itineraryProducts);
  }

  public Multimap<Leg, FareProduct> getLegProducts() {
    return ImmutableMultimap.copyOf(legProducts);
  }

  public void addFare(FareType fareType, Money money) {
    fare.put(fareType, money);
  }

  public void addItineraryProducts(Collection<FareProduct> products) {
    itineraryProducts.addAll(products);
  }

  public Money getFare(FareType type) {
    return fare.get(type);
  }

  public Set<FareType> getTypes() {
    return fare.keySet();
  }

  @Override
  public int hashCode() {
    return Objects.hash(fare, itineraryProducts, legProducts);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ItineraryFares fare1 = (ItineraryFares) o;
    return (
      Objects.equals(itineraryProducts, fare1.itineraryProducts) &&
      Objects.equals(legProducts, fare1.legProducts)
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("legProducts", legProducts).toString();
  }

  public void addLegProducts(Collection<LegProducts> legProducts) {
    legProducts.forEach(lp ->
      this.legProducts.putAll(
          lp.leg(),
          lp.products().stream().map(LegProducts.ProductWithTransfer::product).toList()
        )
    );
  }

  public void addFareProduct(Leg leg, FareProduct fareProduct) {
    this.legProducts.put(leg, fareProduct);
  }
}
