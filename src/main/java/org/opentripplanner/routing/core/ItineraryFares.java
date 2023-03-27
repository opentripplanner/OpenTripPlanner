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
import org.opentripplanner.ext.fares.model.FareProductInstance;
import org.opentripplanner.ext.fares.model.LegProducts;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * ItineraryFares is a set of fares for different legs, rider categories or fare media.
 */
public class ItineraryFares {

  private static final String FARES_V1_FEED_ID = "faresv1";

  /**
   * The fare products that are valid for all legs of an itinerary, like a day pass.
   * <p>
   * Note: LinkedHashSet keeps the insertion order
   */
  private final Set<FareProduct> itineraryProducts = new LinkedHashSet<>();

  /**
   * Fare products that apply to one or more legs but _not_ the entire journey.
   * <p>
   * Note: LinkedHashMultimap keeps the insertion order
   */
  private final Multimap<Leg, FareProductInstance> legProducts = LinkedHashMultimap.create();

  /**
   * The fares V1 fare "components" that apply to individual legs (not the entire price of the
   * itinerary).
   * <p>
   * This is an ill-thought-out concept that was bolted onto the existing implementation in 2016 and
   * is going to be removed once HSL has migrated off it.
   * <p>
   * Note: LinkedHashMultimap keeps the insertion order
   */
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

  /**
   * Get those fare products that cover the entire itinerary.
   */
  public List<FareProduct> getItineraryProducts() {
    return List.copyOf(itineraryProducts);
  }

  /**
   * Get those fare products that are valid for a subset of legs but not the entire itinerary.
   */
  public Multimap<Leg, FareProductInstance> getLegProducts() {
    return ImmutableMultimap.copyOf(legProducts);
  }

  public void addFare(FareType fareType, Money money) {
    itineraryProducts.add(
      new FareProduct(
        faresV1Id(fareType),
        "Itinerary fare for type %s".formatted(fareType.name()),
        money,
        null,
        null,
        null
      )
    );
  }

  @Deprecated
  public void addFareComponent(FareType fareType, List<FareComponent> components) {
    this.components.replaceValues(fareType, components);

    for (var c : components) {
      var firstLegStartTime = c.legs().get(0).getStartTime();
      for (var leg : c.legs()) {
        final FareProduct fareProduct = new FareProduct(
          c.fareId(),
          fareType.name(),
          c.price(),
          null,
          null,
          null
        );
        legProducts.put(
          leg,
          new FareProductInstance(fareProduct.uniqueInstanceId(firstLegStartTime), fareProduct)
        );
      }
    }
  }

  /**
   * Add fare products that cover the entire itinerary, i.e. are valid for all legs.
   */
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
    legProducts.forEach(lp -> {
      var time = lp.leg().getStartTime();
      var products = lp
        .products()
        .stream()
        .map(LegProducts.ProductWithTransfer::product)
        .map(fp -> new FareProductInstance(fp.uniqueInstanceId(time), fp))
        .toList();
      this.legProducts.putAll(lp.leg(), products);
    });
  }

  public void addFareProduct(Leg leg, FareProduct fareProduct) {
    this.legProducts.put(
        leg,
        new FareProductInstance(fareProduct.uniqueInstanceId(leg.getStartTime()), fareProduct)
      );
  }

  @Nonnull
  private static FeedScopedId faresV1Id(FareType fareType) {
    return new FeedScopedId(FARES_V1_FEED_ID, fareType.name());
  }
}
