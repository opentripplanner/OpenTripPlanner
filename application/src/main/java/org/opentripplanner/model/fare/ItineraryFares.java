package org.opentripplanner.model.fare;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.utils.lang.Sandbox;
import org.opentripplanner.utils.tostring.ToStringBuilder;

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
   * Add fare products that cover the entire itinerary, i.e. are valid for all legs.
   */
  public void addItineraryProducts(Collection<FareProduct> products) {
    itineraryProducts.addAll(products);
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
    return ToStringBuilder.of(this.getClass())
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

  public void addFareProductUses(Multimap<Leg, FareProductUse> fareProducts) {
    legProducts.putAll(fareProducts);
  }

  /**
   * Add the contents of another instance to this one.
   */
  public void add(ItineraryFares fare) {
    itineraryProducts.addAll(fare.itineraryProducts);
    legProducts.putAll(fare.legProducts);
  }

  /**
   * Does this instance contain any fare products?
   */
  public boolean isEmpty() {
    return itineraryProducts.isEmpty() && legProducts.isEmpty();
  }
}
