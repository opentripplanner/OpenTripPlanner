package org.opentripplanner.routing.core;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
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

  // LinkedHashSet for insertion order
  private final Set<FareProduct> itineraryProducts = new LinkedHashSet<>();

  // LinkedHashMultimap keeps the insertion order
  private final Multimap<Leg, FareProduct> legProducts = LinkedHashMultimap.create();

  @Deprecated
  private final Multimap<FareType, FareComponent> components = LinkedHashMultimap.create();

  public ItineraryFares(ItineraryFares aFare) {
    if (aFare != null) {
      itineraryProducts.addAll(aFare.itineraryProducts);
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
    itineraryProducts.add(
      new FareProduct(faresV1Id(fareType), fareType.name(), money, null, null, null)
    );
  }

  @Nonnull
  private static FeedScopedId faresV1Id(FareType fareType) {
    return new FeedScopedId(FARES_V1_FEED_ID, fareType.name());
  }

  @Deprecated
  public void addFareDetails(FareType fareType, List<FareComponent> components) {
    this.components.replaceValues(fareType, components);

    for (var c : components) {
      for (var leg : c.legs()) {
        legProducts.put(
          leg,
          new FareProduct(c.fareId(), fareType.name(), c.price(), null, null, null)
        );
      }
    }
  }

  public void addItineraryProducts(Collection<FareProduct> products) {
    itineraryProducts.addAll(products);
  }

  public Money getFare(FareType type) {
    return itineraryProducts
      .stream()
      .filter(f -> faresV1Id(type).equals(f.id()))
      .findAny()
      .map(FareProduct::amount)
      .orElse(null);
  }

  public List<FareComponent> getComponents(FareType type) {
    return List.copyOf(components.get(type));
  }

  public Set<FareType> getFaresV1Types() {
    return itineraryProducts
      .stream()
      .filter(fp -> fp.id().getFeedId().equals(FARES_V1_FEED_ID))
      .map(fp -> fp.id().getId())
      .map(FareType::valueOf)
      .collect(Collectors.toSet());
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
    return ToStringBuilder
      .of(this.getClass())
      .addObj("itineraryProducts", itineraryProducts)
      .addObj("legProducts", legProducts)
      .toString();
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
