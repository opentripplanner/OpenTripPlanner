package org.opentripplanner.model.fare;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.core.FareComponent;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.transit.model.basic.Money;

/**
 *
 * ItineraryFares is a set of fares for different legs, rider categories or fare media.
 */
@Sandbox
public class ItineraryFares {

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
  private final Multimap<Leg, FareProductUse> legProducts = LinkedHashMultimap.create();

  /**
   * The fares V1 fare "components" that apply to individual legs (not the entire price of the
   * itinerary).
   * <p>
   * This is an ill-thought-out concept that was bolted onto the existing implementation in 2016 and
   * is going to be removed once HSL has migrated off it.
   * <p>
   * Note: LinkedHashMultimap keeps the insertion order
   * @deprecated Exists only for backwards compatibility and will be removed in the future.
   */
  @Deprecated
  private final Multimap<FareType, FareComponent> components = LinkedHashMultimap.create();

  /**
   * Holds the "fares" for the entire itinerary. The definition of a fare is not clear so
   * this is deprecated.
   * @deprecated Exists only for backwards compatibility and will be removed in the future.
   */
  @Deprecated
  private final Map<FareType, Money> fares = new HashMap<>();

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
  public Multimap<Leg, FareProductUse> getLegProducts() {
    return ImmutableMultimap.copyOf(legProducts);
  }

  /**
   * Add a "fare". This is an ill-defined concept (is it for the entire itinerary or only some
   * legs?) from the early days of OTP which will be removed in the future.
   * <p>
   * @deprecated It only exists for backwards-compatibility.
   * Use {@link ItineraryFares#addFareProduct(Leg, FareProduct)},
   * {@link ItineraryFares#addItineraryProducts(Collection)} instead.
   */
  @Deprecated
  public void addFare(FareType fareType, Money money) {
    fares.put(fareType, money);
  }

  /**
   * Add a collection of "fare components" for a type. These concepts are ill-defined and will be
   * removed in the future.
   * <p>
   * @deprecated Only exitst for backwards compatibility.
   * Use @{link {@link ItineraryFares#addItineraryProducts(Collection)}},
   * {@link ItineraryFares#addFareProduct(Leg, FareProduct)} or
   */
  @Deprecated
  public void addFareComponent(FareType fareType, List<FareComponent> components) {
    this.components.replaceValues(fareType, components);
  }

  /**
   * Add fare products that cover the entire itinerary, i.e. are valid for all legs.
   */
  public void addItineraryProducts(Collection<FareProduct> products) {
    itineraryProducts.addAll(products);
  }

  /**
   *
   * Get the "fare" for a specific fare type.
   * <p>
   * It is ill-defined what this actually means (entire itinerary?, some legs?).
   * <p>
   * Use {@link ItineraryFares#getItineraryProducts()} or {@link ItineraryFares#getLegProducts()}
   * instead.
   */
  @Nullable
  public Money getFare(FareType type) {
    return fares.get(type);
  }

  /**
   * Get the "components" of a fare for a specific type.
   * <p>
   * Use {@link ItineraryFares#getItineraryProducts()} or {@link ItineraryFares#getLegProducts()}
   * instead.
   */
  @Deprecated
  public List<FareComponent> getComponents(FareType type) {
    return List.copyOf(components.get(type));
  }

  /**
   * Return the set of {@link FareType}s that are contained in this instance.
   */
  @Deprecated
  public Set<FareType> getFareTypes() {
    return fares.keySet();
  }

  @Override
  public int hashCode() {
    return Objects.hash(itineraryProducts, legProducts, components);
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

  /**
   * Add a single fare product for a single leg.
   */
  public void addFareProduct(Leg leg, FareProduct fareProduct) {
    this.legProducts.put(
        leg,
        new FareProductUse(fareProduct.uniqueInstanceId(leg.getStartTime()), fareProduct)
      );
  }

  /**
   * Add several fare products to a leg.
   */
  public void addFareProduct(Leg leg, Collection<FareProduct> fareProduct) {
    fareProduct.forEach(fp -> addFareProduct(leg, fp));
  }

  /**
   * Convert the fare received via the deprecated {@link FareComponent} to leg products. This
   * inverts the relationship:
   *   - fare component has several legs
   *   - leg product is a mapping from leg to a list of fare products
   */
  @Nonnull
  public Multimap<Leg, FareProductUse> legProductsFromComponents() {
    Multimap<Leg, FareProductUse> legProductsFromComponents = HashMultimap.create();
    for (var type : getFareTypes()) {
      var components = getComponents(type);

      for (var c : components) {
        var firstLegStartTime = c.legs().get(0).getStartTime();
        for (var leg : c.legs()) {
          final FareProduct fareProduct = FareProduct
            .of(c.fareId(), type.name(), c.price())
            .build();

          legProductsFromComponents.put(
            leg,
            new FareProductUse(fareProduct.uniqueInstanceId(firstLegStartTime), fareProduct)
          );
        }
      }
    }
    return legProductsFromComponents;
  }
}
