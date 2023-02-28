package org.opentripplanner.routing.core;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.ext.fares.model.LegProducts;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * <p>
 * ItineraryFares is a set of fares for different classes of users.
 * </p>
 */
public class ItineraryFares {

  private static final String FARES_V1_FEED_ID = "faresv1";
  private final Set<FareProduct> itineraryProducts = new LinkedHashSet<>();
  private final Multimap<Leg, FareProduct> legProducts = LinkedHashMultimap.create();

  public ItineraryFares(ItineraryFares aFare) {
    itineraryProducts.addAll(aFare.itineraryProducts);
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

  /**
   * Backwards-compatible method to add a fare product that is valid for the entire itinerary.
   */
  public void addFare(FareType fareType, Money money) {
    itineraryProducts.add(
      new FareProduct(
        new FeedScopedId(FARES_V1_FEED_ID, fareType.name()),
        fareType.name(),
        money,
        null,
        null,
        null
      )
    );
  }

  public void addItineraryProducts(Collection<FareProduct> products) {
    itineraryProducts.addAll(products);
  }

  public Money getFare(FareType type) {
    return itineraryProducts
      .stream()
      .filter(f -> f.name().equals(type.name()))
      .findAny()
      .map(FareProduct::amount)
      .orElse(null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(itineraryProducts, legProducts);
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
